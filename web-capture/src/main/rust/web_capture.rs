use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jlong};
use jni::JNIEnv;
use monolith::core::{create_monolithic_document, MonolithOutputFormat, Options};
use std::fs::File;
use std::io::Write;
use std::os::fd::FromRawFd;

fn get_string_from_jni(env: &mut JNIEnv, string: JString) -> String {
    match env.get_string(&string) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new("java/lang/RuntimeException", e.to_string());
            String::new()
        }
    }
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
    let url: String = get_string_from_jni(&mut env, url);
    let user_agent: String = get_string_from_jni(&mut env, user_agent);

    let html_doc = match get_html_doc(
        url,
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
            env.throw_new("java/lang/RuntimeException", err.to_string())
                .unwrap();
            return 0;
        }
    };

    let web_capture_file_creation = if file_descriptor == -1 {
        // on desktop, we don't need file descriptor
        let file_path = get_string_from_jni(&mut env, file_path);
        std::fs::write(file_path, html_doc)
    } else {
        // on android, we use file descriptor passed from the app to write the content directly to disk
        // other way to do this is to share raw byte array back to kotlin, which i obviously did at first,
        // but that seems to create its own memory, which is not so good
        let mut web_capture_file = unsafe { File::from_raw_fd(file_descriptor) };
        web_capture_file.write_all(&*html_doc)
    };

    match web_capture_file_creation {
        Ok(_) => {
            println!("noice");
        }
        Err(err) => {
            env.throw_new("java/lang/RuntimeException", err.to_string())
                .unwrap();
            return 0;
        }
    }
    // rust will close the stuff related to file descriptor once the call exits this block, adios
    1
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
