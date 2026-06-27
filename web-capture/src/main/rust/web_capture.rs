use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jlong};
use jni::JNIEnv;
use monolith::core::{create_monolithic_document, MonolithOutputFormat, Options};
use std::any::Any;
use std::fs::File;
use std::io::Write;
use std::os::fd::FromRawFd;
use std::panic::{catch_unwind, AssertUnwindSafe};

fn get_string_from_jni(env: &mut JNIEnv, string: JString) -> Option<String> {
    match env.get_string(&string) {
        Ok(j_str) => Some(j_str.into()),
        Err(err) => {
            let _ = env.throw_new("java/lang/RuntimeException", err.to_string());
            None
        }
    }
}

fn get_string_from_panic_payload(payload: Box<dyn Any + Send>) -> String {
    if let Some(string) = payload.downcast_ref::<&'static str>() {
        return string.to_string();
    }
    if let Some(string) = payload.downcast_ref::<String>() {
        return string.to_string();
    }
    "unknown panic cause".to_string()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_sakethh_linkora_JVMAndAndroidWebCapture_saveHTMLPage(
    mut env: JNIEnv,
    _class: JClass,
    file_descriptor: jint,
    file_path: JString,
    url: JString,
    user_agent: JString,
    timeout: jlong,
    allow_insecure_protocol: jboolean,
    ignore_doc_errors: jboolean,
    use_css: jboolean,
    embed_fonts: jboolean,
    embed_images: jboolean,
    restrict_js: jboolean,
    include_audio_elements: jboolean,
    include_video_elements: jboolean,
    include_metadata: jboolean,
    log_stuff: jboolean,
) -> jboolean {
    // > A Rust panic is not always implemented via unwinding,
    // > but can be implemented by aborting the process as well.
    // > This function only catches unwinding panics, not those that abort the process.
    // `catch_unwind` just in case something goes wrong and the internals of the used functions
    // here don't abort the process and support unwinding; we will let `Drop` do its thing, so we won't leak anything.
    let web_capture_result = catch_unwind(AssertUnwindSafe(|| {
        let Some(url) = get_string_from_jni(&mut env, url) else {
            return jboolean::from(false);
        };
        let Some(user_agent) = get_string_from_jni(&mut env, user_agent) else {
            return jboolean::from(false);
        };
        let html_doc = match get_html_doc(
            url.to_string(),
            user_agent,
            timeout as u64,
            allow_insecure_protocol != 0,
            ignore_doc_errors != 0,
            use_css != 0,
            embed_fonts != 0,
            embed_images != 0,
            restrict_js != 0,
            include_audio_elements != 0,
            include_video_elements != 0,
            include_metadata != 0,
            log_stuff != 0,
        ) {
            Ok(doc) => doc,
            Err(err) => {
                let _ = env.throw_new("java/lang/RuntimeException", err.to_string());
                return jboolean::from(false);
            }
        };
        let web_capture_file_creation = if file_descriptor == -1 {
            // on desktop, we don't need file descriptor
            println!("web-capture(rust): Writing: {}", url);
            let Some(file_path) = get_string_from_jni(&mut env, file_path) else {
                return jboolean::from(false);
            };
            let web_capture_result = std::fs::write(file_path, html_doc);
            println!("web-capture(rust): Written: {}", url);
            web_capture_result
        } else {
            let mut web_capture_file = unsafe { File::from_raw_fd(file_descriptor) };
            println!("web-capture(rust): Writing: {}", url);
            let web_capture_result = web_capture_file.write_all(&*html_doc);
            println!("web-capture(rust): Written: {}", url);
            web_capture_result
        };
        match web_capture_file_creation {
            Ok(_) => {
                println!("noice");
                jboolean::from(true)
            }
            Err(err) => {
                let _ = env.throw_new("java/lang/RuntimeException", err.to_string());
                jboolean::from(false)
            }
        }
    }));

    match web_capture_result {
        Ok(result) => result,
        Err(panic_payload) => {
            // a panic anywhere above unwinds here,
            // `Drop` already ran on the way up, so nothing's leaked
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                get_string_from_panic_payload(panic_payload),
            );
            jboolean::from(false)
        }
    }
}

fn get_html_doc(
    url: String,
    user_agent: String,
    timeout: u64,
    allow_insecure_protocol: bool,
    ignore_doc_errors: bool,
    use_css: bool,
    embed_fonts: bool,
    embed_images: bool,
    restrict_js: bool,
    include_audio_elements: bool,
    include_video_elements: bool,
    include_metadata: bool,
    log_stuff: bool,
) -> Result<Vec<u8>, String> {
    if user_agent.is_empty() {
        return Err("User-Agent should not be empty".to_string());
    }
    if url.is_empty() {
        return Err("url should not be empty".to_string());
    }
    let monolith_doc = create_monolithic_document(
        url,
        &mut Options {
            base_url: None,
            blacklist_domains: false,
            cookies: vec![],
            domains: None,
            encoding: None,
            ignore_errors: ignore_doc_errors,
            insecure: allow_insecure_protocol,
            isolate: false,
            no_audio: !include_audio_elements,
            no_css: !use_css,
            no_fonts: !embed_fonts,
            no_frames: false,
            no_images: !embed_images,
            no_js: restrict_js,
            no_metadata: !include_metadata,
            no_video: !include_video_elements,
            output_format: MonolithOutputFormat::HTML,
            silent: !log_stuff,
            timeout,
            unwrap_noscript: false,
            user_agent: Some(user_agent),
        },
        &mut None,
    );
    monolith_doc
        .map_err(|err| err.to_string())
        .and_then(|(doc, _)| Ok(doc))
}

#[cfg(test)]
mod jni_integration_tests {
    use super::*;
    use jni::{InitArgsBuilder, JNIVersion, JavaVM};
    use std::os::fd::IntoRawFd;
    use std::sync::OnceLock;

    fn test_vm() -> &'static JavaVM {
        static VM: OnceLock<JavaVM> = OnceLock::new();
        VM.get_or_init(|| {
            let args = InitArgsBuilder::new()
                .version(JNIVersion::V8)
                .build()
                .expect("bad JVM init args");
            JavaVM::new(args).expect("failed to start embedded JVM - check JAVA_HOME")
        })
    }

    #[test]
    fn writes_file_on_success() {
        let vm = test_vm();
        let mut env = vm.attach_current_thread().unwrap();

        let tmp_file = tempfile::NamedTempFile::new().unwrap();
        let file_path = env.new_string(tmp_file.path().to_str().unwrap()).unwrap();
        let url = env.new_string("https://example.com").unwrap();
        let user_agent = env.new_string("linkora-test-agent").unwrap();
        let class = env.find_class("java/lang/Object").unwrap(); // unused by the fn body, any valid class works

        let desktop_result = Java_com_sakethh_linkora_JVMAndAndroidWebCapture_saveHTMLPage(
            unsafe { env.unsafe_clone() },
            unsafe { JClass::from_raw(class.clone()) },
            -1, // desktop path, no fd needed
            file_path,
            unsafe { JString::from_raw(url.clone()) },
            unsafe { JString::from_raw(user_agent.clone()) },
            5000,
            0,
            0,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            0,
        );

        assert_eq!(desktop_result, jboolean::from(true));
        assert!(tmp_file.path().exists());
        assert!(!std::fs::read_to_string(tmp_file.path()).unwrap().is_empty());

        // remove all the content from the existing file
        File::create(tmp_file.path()).unwrap();
        assert!(std::fs::read_to_string(tmp_file.path()).unwrap().is_empty());

        let android_result = Java_com_sakethh_linkora_JVMAndAndroidWebCapture_saveHTMLPage(
            unsafe { env.unsafe_clone() },
            class,
            tmp_file.reopen().unwrap().into_raw_fd(), // desktop test will close the file
            env.new_string("").unwrap(),
            url,
            user_agent,
            5000,
            0,
            0,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            0,
        );

        assert_eq!(android_result, jboolean::from(true));
        assert!(tmp_file.path().exists());
        assert!(!std::fs::read_to_string(tmp_file.path()).unwrap().is_empty());
    }

    #[test]
    fn throws_runtime_exception_on_connection_failure() {
        let vm = test_vm();
        let mut env = vm.attach_current_thread().unwrap();

        let tmp = tempfile::NamedTempFile::new().unwrap();
        let file_path = env.new_string(tmp.path().to_str().unwrap()).unwrap();
        let url = env.new_string("http://127.0.0.1:1").unwrap();
        let user_agent = env.new_string("linkora-test-agent").unwrap();
        let class = env.find_class("java/lang/Object").unwrap();

        let result = Java_com_sakethh_linkora_JVMAndAndroidWebCapture_saveHTMLPage(
            unsafe { env.unsafe_clone() },
            class,
            -1,
            file_path,
            url,
            user_agent,
            2000,
            0,
            0,
            1,
            1,
            1,
            1,
            1,
            1,
            1,
            0,
        );

        assert_eq!(result, jboolean::from(false));
        assert!(
            env.exception_check().unwrap(),
            "expected a pending RuntimeException"
        );
        env.exception_clear().unwrap();
    }
}
