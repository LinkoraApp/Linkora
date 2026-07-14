use dashmap::DashMap;
use jni::JNIEnv;
use jni::objects::{GlobalRef, JClass, JObject, JString, JValue};
use jni::sys::{jboolean, jint, jlong};
use monolith::core::{
    CancelToken, EXTERNAL_CANCELLATION_PANIC, MonolithOutputFormat, Options,
    create_monolithic_document,
};
use std::any::Any;
use std::panic::{AssertUnwindSafe, catch_unwind};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex, OnceLock, mpsc};
use tokio::runtime::Runtime;
use tokio::task::AbortHandle;
#[cfg(target_os = "android")]
use {std::fs::File, std::io::Write, std::os::fd::FromRawFd};

static TOKIO_RUNTIME: OnceLock<Runtime> = OnceLock::new();

fn get_tokio_runtime() -> &'static Runtime {
    TOKIO_RUNTIME.get_or_init(|| {
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .unwrap()
    })
}

#[derive(Debug)]
enum JniOpError {
    Jni(String),
}

impl JniOpError {
    fn message(&self) -> String {
        match self {
            JniOpError::Jni(msg) => msg.clone(),
        }
    }
}

impl From<jni::errors::Error> for JniOpError {
    fn from(err: jni::errors::Error) -> Self {
        JniOpError::Jni(err.to_string())
    }
}

fn get_string_from_jni(env: &mut JNIEnv, string: JString) -> Result<String, JniOpError> {
    let j_str = env.get_string(&string)?;
    Ok(j_str.into())
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

static WEB_CAPTURE_OP_ABORT_HANDLES: OnceLock<DashMap<String, (CancelToken, AbortHandle)>> =
    OnceLock::new();

fn get_web_capture_abort_handles() -> &'static DashMap<String, (CancelToken, AbortHandle)> {
    WEB_CAPTURE_OP_ABORT_HANDLES.get_or_init(DashMap::new)
}

fn call_on_thrown(env: &mut JNIEnv, callback: &JObject, msg: &str) {
    let Ok(jni_msg) = env.new_string(msg) else {
        eprintln!("failed to allocate JString for onThrown message: {msg}");
        return;
    };
    if let Err(err) = env.call_method(
        callback,
        "onThrown",
        "(Ljava/lang/String;)V",
        &[JValue::Object(&jni_msg)],
    ) {
        eprintln!("failed to invoke onThrown callback: {err}, original message: {msg}");
    }
}

fn on_capture_result(env: &mut JNIEnv, global_ref: &GlobalRef, op_key: JString, is_success: bool) {
    let _ = env.call_method(
        global_ref,
        "onCaptureResult",
        "(Ljava/lang/String;Z)V",
        &[JValue::Object(&*op_key), JValue::Bool(is_success.into())],
    );
}

pub enum RouterMessage {
    CaptureResult {
        op_key: String,
        is_success: bool,
    },
    CaptureError {
        op_key: String,
        error_message: String,
    },
    Shutdown,
}

static CAPTURE_RESULT_SENDER: Mutex<Option<mpsc::Sender<RouterMessage>>> = Mutex::new(None);
static GLOBAL_ON_RESULT_CALLBACK: Mutex<Option<GlobalRef>> = Mutex::new(None);

fn send_router_message(message: RouterMessage) {
    let capture_sender_guard = CAPTURE_RESULT_SENDER
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());

    if let Some(sender) = capture_sender_guard.as_ref() {
        let _ = sender.send(message);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_sakethh_linkora_JVMAndAndroidWebCapture_spawnResultDaemon(
    mut env: JNIEnv,
    this: JObject,
    on_thrown: JObject,
) {
    let mut global_callback_guard = GLOBAL_ON_RESULT_CALLBACK
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());

    if global_callback_guard.is_some() {
        call_on_thrown(
            &mut env,
            &on_thrown,
            "Result Callback is already initialized",
        );
        return;
    }

    let mut capture_sender_guard = CAPTURE_RESULT_SENDER
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());

    if capture_sender_guard.is_some() {
        call_on_thrown(
            &mut env,
            &on_thrown,
            "Capture Result Sender is already initialized",
        );
        return;
    }

    let local_callback = match env.new_global_ref(&this) {
        Ok(global_callback_ref) => global_callback_ref,
        Err(err) => {
            call_on_thrown(
                &mut env,
                &on_thrown,
                &format!("Failed to create global reference for callback: {err}"),
            );
            return;
        }
    };

    *global_callback_guard = Some(local_callback);

    let (capture_status_sender, capture_status_receiver) = mpsc::channel::<RouterMessage>();
    *capture_sender_guard = Some(capture_status_sender);

    let java_vm = match env.get_java_vm() {
        Ok(jvm) => jvm,
        Err(err) => {
            call_on_thrown(
                &mut env,
                &on_thrown,
                &format!("Failed to get Java VM: {err}"),
            );
            return;
        }
    };

    std::thread::spawn(move || {
        let mut attached_thread_env = match java_vm.attach_current_thread_as_daemon() {
            Ok(env) => env,
            Err(_) => {
                eprintln!("Fatal error: Listener thread failed to attach to JVM.");
                return;
            }
        };
        while let Ok(message) = capture_status_receiver.recv() {
            if let RouterMessage::Shutdown = message {
                break;
            }

            let callback_guard = GLOBAL_ON_RESULT_CALLBACK
                .lock()
                .unwrap_or_else(|poisoned| poisoned.into_inner());

            let global_callback_ref = match callback_guard.as_ref() {
                Some(cb_ref) => cb_ref,
                None => continue,
            };

            let _ = attached_thread_env.with_local_frame(16, |env| {
                match message {
                    RouterMessage::CaptureResult { op_key, is_success } => {
                        let jni_op_key = match env.new_string(&op_key) {
                            Ok(s) => s,
                            Err(_) => return Ok(()),
                        };

                        #[cfg(not(test))]
                        {
                            on_capture_result(env, global_callback_ref, jni_op_key, is_success);
                        }
                    }

                    RouterMessage::CaptureError {
                        op_key,
                        error_message,
                    } => {
                        if error_message == EXTERNAL_CANCELLATION_PANIC {
                            println!("monolith cancelled a task due to external cancellation");
                        } else {
                            eprintln!("capture error for op_key (logged only): {error_message}");
                        }

                        let jni_op_key = match env.new_string(&op_key) {
                            Ok(s) => s,
                            Err(_) => return Ok(()),
                        };

                        #[cfg(not(test))]
                        {
                            on_capture_result(env, global_callback_ref, jni_op_key, false);
                        }
                    }

                    RouterMessage::Shutdown => {
                        unreachable!("Shutdown is handled before entering the local frame");
                    }
                }

                Ok::<(), jni::errors::Error>(())
            });
        }
    });
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_sakethh_linkora_JVMAndAndroidWebCapture_killResultDaemon(
    mut env: JNIEnv,
    _class: JClass,
    on_thrown: JObject,
) {
    let mut capture_sender_guard = CAPTURE_RESULT_SENDER
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());

    if let Some(capture_status_sender) = capture_sender_guard.as_ref() {
        if capture_status_sender.send(RouterMessage::Shutdown).is_err() {
            call_on_thrown(
                &mut env,
                &on_thrown,
                "Failed to send shutdown signal to listener thread",
            );
            return;
        }
    } else {
        call_on_thrown(
            &mut env,
            &on_thrown,
            "Listener thread is not currently running",
        );
        return;
    }

    capture_sender_guard.take();
    GLOBAL_ON_RESULT_CALLBACK
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner())
        .take();
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_sakethh_linkora_JVMAndAndroidWebCapture_cancelWebCapture(
    mut env: JNIEnv,
    _class: JClass,
    key: JString,
    on_thrown: JObject,
) {
    let op_key_string = match get_string_from_jni(&mut env, key) {
        Ok(s) => s,
        Err(e) => {
            call_on_thrown(
                &mut env,
                &on_thrown,
                &format!("Invalid Capture Op Key: {}", e.message()),
            );
            return;
        }
    };

    if let Some((_, (cancel_token, abort_handler))) =
        get_web_capture_abort_handles().remove(&op_key_string)
    {
        cancel_token.trigger_cancellation();
        abort_handler.abort()
    } else {
        call_on_thrown(
            &mut env,
            &on_thrown,
            "Failed to find web capture abort handler",
        );
    }
}

trait CancelTokenExtensions {
    fn trigger_cancellation(&self);
}

impl CancelTokenExtensions for CancelToken {
    fn trigger_cancellation(&self) {
        let Some(cancel_token) = &self.0 else {
            return;
        };
        cancel_token.store(true, Ordering::Relaxed)
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_sakethh_linkora_JVMAndAndroidWebCapture_saveHTMLPage(
    mut env: JNIEnv,
    _class: JClass,
    file_descriptor: jint,
    file_path: JString,
    url: JString,
    op_key: JString,
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
    on_thrown: JObject,
) {
    let local_op_key = match get_string_from_jni(&mut env, op_key) {
        Ok(s) => s,
        Err(e) => {
            call_on_thrown(
                &mut env,
                &on_thrown,
                &format!("Couldn't retrieve the operation key: {}", e.message()),
            );
            return;
        }
    };
    let tokio_op_key = local_op_key.clone();
    let cleanup_op_key = local_op_key.clone();
    let cancel_token = CancelToken(Some(Arc::new(AtomicBool::new(false))));
    let cleanup_cancel_token = cancel_token.clone();

    let file_path_string = match get_string_from_jni(&mut env, file_path) {
        Ok(s) => s,
        Err(e) => {
            call_on_thrown(&mut env, &on_thrown, &e.message());
            return;
        }
    };

    let url_string = match get_string_from_jni(&mut env, url) {
        Ok(s) => s,
        Err(e) => {
            call_on_thrown(&mut env, &on_thrown, &e.message());
            return;
        }
    };

    let user_agent_string = match get_string_from_jni(&mut env, user_agent) {
        Ok(s) => s,
        Err(e) => {
            call_on_thrown(&mut env, &on_thrown, &e.message());
            return;
        }
    };

    let capture_job = get_tokio_runtime().spawn_blocking(move || {
        let web_capture_result = catch_unwind(AssertUnwindSafe(|| {
            let html_doc = match get_html_doc(
                url_string,
                user_agent_string,
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
                cancel_token,
            ) {
                Ok(doc) => doc,
                Err(err) => {
                    send_router_message(RouterMessage::CaptureError {
                        op_key: tokio_op_key.clone(),
                        error_message: err,
                    });
                    return;
                }
            };

            #[cfg(target_os = "android")]
            let web_capture_file_creation = {
                let mut web_capture_file = unsafe { File::from_raw_fd(file_descriptor) };
                web_capture_file.write_all(&html_doc)
            };

            #[cfg(not(target_os = "android"))]
            let web_capture_file_creation = { std::fs::write(file_path_string, html_doc) };

            match web_capture_file_creation {
                Ok(_) => {
                    send_router_message(RouterMessage::CaptureResult {
                        op_key: tokio_op_key.clone(),
                        is_success: true,
                    });
                }
                Err(err) => {
                    send_router_message(RouterMessage::CaptureError {
                        op_key: tokio_op_key.clone(),
                        error_message: err.to_string(),
                    });
                }
            }
        }));

        if let Err(panic_payload) = web_capture_result {
            send_router_message(RouterMessage::CaptureError {
                op_key: tokio_op_key.clone(),
                error_message: get_string_from_panic_payload(panic_payload),
            });
        }

        get_web_capture_abort_handles().remove(&cleanup_op_key);
    });

    let insert_capture_abort_handler_result = catch_unwind(AssertUnwindSafe(|| {
        get_web_capture_abort_handles().insert(
            local_op_key.clone(),
            (cleanup_cancel_token.clone(), capture_job.abort_handle()),
        );
    }));

    if let Err(panic_payload) = insert_capture_abort_handler_result {
        cleanup_cancel_token.trigger_cancellation();
        capture_job.abort();
        call_on_thrown(
            &mut env,
            &on_thrown,
            &get_string_from_panic_payload(panic_payload),
        );
        send_router_message(RouterMessage::CaptureResult {
            op_key: local_op_key,
            is_success: false,
        });
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
    cancel_token: CancelToken,
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
            cancel_token,
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
        .map(|(doc, _)| doc)
}

#[cfg(test)]
mod jni_integration_tests {
    use super::*;
    use jni::{InitArgsBuilder, JNIVersion, JavaVM};
    use std::io::Read;
    use std::sync::OnceLock;
    use std::time::Duration;

    fn spawn_mock_http_server(delay: Duration, body: &'static str) -> u16 {
        let listener = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
        let port = listener.local_addr().unwrap().port();

        std::thread::spawn(move || {
            // Loop endlessly to accept multiple connections
            for stream in listener.incoming() {
                if let Ok(mut stream) = stream {
                    // Spawn a thread for each request so they process concurrently
                    std::thread::spawn(move || {
                        use std::io::{Read, Write};
                        let mut buf = [0u8; 1024];
                        let _ = stream.read(&mut buf);
                        std::thread::sleep(delay);
                        let response = format!(
                            "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
                            body.len(),
                            body
                        );
                        let _ = stream.write_all(response.as_bytes());
                    });
                }
            }
        });

        port
    }

    struct TestRng(u64);

    impl TestRng {
        fn new() -> Self {
            let seed = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_nanos() as u64
                | 1;
            Self(seed)
        }

        fn next_u64(&mut self) -> u64 {
            self.0 ^= self.0 << 13;
            self.0 ^= self.0 >> 7;
            self.0 ^= self.0 << 17;
            self.0
        }

        fn next_bool(&mut self) -> bool {
            self.next_u64() & 1 == 0
        }

        fn next_range(&mut self, upper_exclusive: u64) -> u64 {
            self.next_u64() % upper_exclusive
        }
    }

    fn test_vm() -> &'static JavaVM {
        static VM: OnceLock<JavaVM> = OnceLock::new();
        VM.get_or_init(|| {
            let args = InitArgsBuilder::new()
                .version(JNIVersion::V8)
                .build()
                .expect("bad JVM init args");
            JavaVM::new(args).expect("failed to start embedded JVM")
        })
    }

    #[test]
    fn rapid_daemon_toggle_stress_test() {
        let vm = test_vm();
        let mut env = vm.attach_current_thread().unwrap();
        let class = env.find_class("java/lang/Object").unwrap();
        let dummy_callback = env.new_string("dummy").unwrap();

        for _ in 0..10 {
            Java_com_sakethh_linkora_JVMAndAndroidWebCapture_spawnResultDaemon(
                unsafe { env.unsafe_clone() },
                unsafe { JObject::from_raw(class.clone()) },
                unsafe { JObject::from_raw(dummy_callback.clone()) },
            );

            assert!(CAPTURE_RESULT_SENDER.lock().unwrap().is_some());
            assert!(GLOBAL_ON_RESULT_CALLBACK.lock().unwrap().is_some());

            Java_com_sakethh_linkora_JVMAndAndroidWebCapture_killResultDaemon(
                unsafe { env.unsafe_clone() },
                unsafe { JClass::from_raw(class.clone()) },
                unsafe { JObject::from_raw(dummy_callback.clone()) },
            );

            assert!(CAPTURE_RESULT_SENDER.lock().unwrap().is_none());
            assert!(GLOBAL_ON_RESULT_CALLBACK.lock().unwrap().is_none());
            assert!(!env.exception_check().unwrap());
        }
    }

    #[tokio::test(flavor = "multi_thread", worker_threads = 15)]
    async fn concurrent_web_captures_behavior_test() {
        let vm = test_vm();
        let mut env = vm.attach_current_thread().unwrap();
        let class = env.find_class("java/lang/Object").unwrap();
        let dummy_callback = env.new_string("dummy").unwrap();

        Java_com_sakethh_linkora_JVMAndAndroidWebCapture_spawnResultDaemon(
            unsafe { env.unsafe_clone() },
            unsafe { JObject::from_raw(class.clone()) },
            unsafe { JObject::from_raw(dummy_callback.clone()) },
        );

        let mut temp_files = Vec::new();
        let user_agent = env.new_string("linkora-test-agent").unwrap();

        let port = spawn_mock_http_server(
            Duration::from_millis(50),
            "<html><body>Concurrent Success</body></html>",
        );
        let url = env
            .new_string(format!("http://127.0.0.1:{}/", port))
            .unwrap();

        for i in 0..15 {
            let tmp_file = tempfile::NamedTempFile::new().unwrap();
            let file_path = env.new_string(tmp_file.path().to_str().unwrap()).unwrap();
            let op_key = env.new_string(format!("op_{}", i)).unwrap();

            Java_com_sakethh_linkora_JVMAndAndroidWebCapture_saveHTMLPage(
                unsafe { env.unsafe_clone() },
                unsafe { JClass::from_raw(class.clone()) },
                -1,
                file_path,
                unsafe { JString::from_raw(url.clone()) },
                op_key,
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
                unsafe { JObject::from_raw(dummy_callback.clone()) },
            );

            temp_files.push(tmp_file);
        }

        tokio::time::sleep(Duration::from_secs(5)).await;

        for tmp_file in temp_files {
            assert!(tmp_file.path().exists());
            let file_contents = std::fs::read_to_string(tmp_file.path()).unwrap();
            assert!(
                !file_contents.is_empty(),
                "File was empty, meaning capture failed!"
            );
        }

        Java_com_sakethh_linkora_JVMAndAndroidWebCapture_killResultDaemon(
            unsafe { env.unsafe_clone() },
            unsafe { JClass::from_raw(class.clone()) },
            unsafe { JObject::from_raw(dummy_callback.clone()) },
        );

        assert!(!env.exception_check().unwrap());
    }

    #[tokio::test(flavor = "multi_thread")]
    async fn cancel_web_capture_test() {
        let vm = test_vm();
        let mut env = vm.attach_current_thread().unwrap();
        let class = env.find_class("java/lang/Object").unwrap();
        let dummy_callback = env.new_string("dummy").unwrap();

        Java_com_sakethh_linkora_JVMAndAndroidWebCapture_spawnResultDaemon(
            unsafe { env.unsafe_clone() },
            unsafe { JObject::from_raw(class.clone()) },
            unsafe { JObject::from_raw(dummy_callback.clone()) },
        );

        let port = spawn_mock_http_server(Duration::from_millis(500), "<html></html>");

        let tmp_file = tempfile::NamedTempFile::new().unwrap();
        let file_path = env.new_string(tmp_file.path().to_str().unwrap()).unwrap();
        let url = env
            .new_string(format!("http://127.0.0.1:{}/", port))
            .unwrap();
        let user_agent = env.new_string("linkora-test-agent").unwrap();
        let op_key = env.new_string("cancel_test_op").unwrap();

        Java_com_sakethh_linkora_JVMAndAndroidWebCapture_saveHTMLPage(
            unsafe { env.unsafe_clone() },
            unsafe { JClass::from_raw(class.clone()) },
            -1,
            file_path,
            url,
            unsafe { JString::from_raw(op_key.clone()) },
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
            unsafe { JObject::from_raw(dummy_callback.clone()) },
        );

        Java_com_sakethh_linkora_JVMAndAndroidWebCapture_cancelWebCapture(
            unsafe { env.unsafe_clone() },
            unsafe { JClass::from_raw(class.clone()) },
            op_key,
            unsafe { JObject::from_raw(dummy_callback.clone()) },
        );

        assert!(!env.exception_check().unwrap());

        tokio::time::sleep(Duration::from_secs(1)).await;

        let file_contents = std::fs::read_to_string(tmp_file.path()).unwrap();
        assert!(file_contents.is_empty());

        Java_com_sakethh_linkora_JVMAndAndroidWebCapture_killResultDaemon(
            unsafe { env.unsafe_clone() },
            unsafe { JClass::from_raw(class.clone()) },
            unsafe { JObject::from_raw(dummy_callback.clone()) },
        );
    }

    #[tokio::test(flavor = "multi_thread", worker_threads = 4)]
    async fn chaotic_cancel_web_capture_test() {
        let vm = test_vm();
        let mut env = vm.attach_current_thread().unwrap();
        let class = env.find_class("java/lang/Object").unwrap();
        let dummy_callback = env.new_string("dummy").unwrap();

        Java_com_sakethh_linkora_JVMAndAndroidWebCapture_spawnResultDaemon(
            unsafe { env.unsafe_clone() },
            unsafe { JObject::from_raw(class.clone()) },
            unsafe { JObject::from_raw(dummy_callback.clone()) },
        );

        let mut rng = TestRng::new();
        let user_agent = env.new_string("linkora-test-agent").unwrap();

        struct CaptureAttempt {
            file: tempfile::NamedTempFile,
            op_key: String,
            was_cancelled: bool,
        }

        let mut attempts = Vec::new();

        for i in 0..40 {
            let delay_ms = rng.next_range(400);
            let port = spawn_mock_http_server(
                Duration::from_millis(delay_ms),
                "<html><body>chaos</body></html>",
            );

            let tmp_file = tempfile::NamedTempFile::new().unwrap();
            let file_path = env.new_string(tmp_file.path().to_str().unwrap()).unwrap();
            let url = env
                .new_string(format!("http://127.0.0.1:{}/", port))
                .unwrap();
            let op_key_string = format!("chaos_op_{}", i);
            let op_key = env.new_string(op_key_string.as_str()).unwrap();

            Java_com_sakethh_linkora_JVMAndAndroidWebCapture_saveHTMLPage(
                unsafe { env.unsafe_clone() },
                unsafe { JClass::from_raw(class.clone()) },
                -1,
                file_path,
                url,
                unsafe { JString::from_raw(op_key.clone()) },
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
                unsafe { JObject::from_raw(dummy_callback.clone()) },
            );

            if rng.next_range(5) == 0 {
                let bogus_key = env
                    .new_string(format!("nonexistent_{}", rng.next_u64()))
                    .unwrap();

                Java_com_sakethh_linkora_JVMAndAndroidWebCapture_cancelWebCapture(
                    unsafe { env.unsafe_clone() },
                    unsafe { JClass::from_raw(class.clone()) },
                    bogus_key,
                    unsafe { JObject::from_raw(dummy_callback.clone()) },
                );

                // We expect a JNI exception here because cancel fails to find the handle
                // and attempts to call onThrown on our dummy String object.
                assert!(env.exception_check().unwrap());
                env.exception_clear().unwrap();
            }

            let mut was_cancelled = false;
            if rng.next_bool() {
                tokio::time::sleep(Duration::from_millis(rng.next_range(50))).await;

                Java_com_sakethh_linkora_JVMAndAndroidWebCapture_cancelWebCapture(
                    unsafe { env.unsafe_clone() },
                    unsafe { JClass::from_raw(class.clone()) },
                    op_key,
                    unsafe { JObject::from_raw(dummy_callback.clone()) },
                );

                if env.exception_check().unwrap() {
                    // It failed to find the abort handle (job already finished and cleaned up)
                    env.exception_clear().unwrap();
                } else {
                    was_cancelled = true;
                }
            }

            attempts.push(CaptureAttempt {
                file: tmp_file,
                op_key: op_key_string,
                was_cancelled,
            });
        }

        tokio::time::sleep(Duration::from_secs(1)).await;

        let mut cancelled_and_empty = 0usize;
        let mut cancelled_but_wrote_anyway = 0usize;
        let mut not_cancelled_and_wrote = 0usize;
        let mut not_cancelled_and_empty = 0usize;

        for attempt in attempts {
            let contents = std::fs::read_to_string(attempt.file.path()).unwrap_or_default();
            let populated = !contents.is_empty();

            match (attempt.was_cancelled, populated) {
                (true, true) => cancelled_but_wrote_anyway += 1,
                (true, false) => cancelled_and_empty += 1,
                (false, true) => not_cancelled_and_wrote += 1,
                (false, false) => not_cancelled_and_empty += 1,
            }

            assert!(
                get_web_capture_abort_handles()
                    .get(&attempt.op_key)
                    .is_none(),
                "abort handle for {} was never cleaned up",
                attempt.op_key
            );
        }

        println!(
            "cancelled+empty: {}, cancelled but wrote anyway: {}, not cancelled+wrote: {}, not cancelled+empty: {}",
            cancelled_and_empty,
            cancelled_but_wrote_anyway,
            not_cancelled_and_wrote,
            not_cancelled_and_empty
        );

        Java_com_sakethh_linkora_JVMAndAndroidWebCapture_killResultDaemon(
            unsafe { env.unsafe_clone() },
            unsafe { JClass::from_raw(class.clone()) },
            unsafe { JObject::from_raw(dummy_callback.clone()) },
        );
    }

    #[test]
    fn chaotic_daemon_lifecycle_stress_test() {
        let vm = test_vm();
        let mut env = vm.attach_current_thread().unwrap();
        let class = env.find_class("java/lang/Object").unwrap();
        let dummy_callback = env.new_string("dummy").unwrap();

        let mut rng = TestRng::new();
        let mut daemon_is_up = false;

        for _ in 0..500 {
            if rng.next_bool() {
                Java_com_sakethh_linkora_JVMAndAndroidWebCapture_spawnResultDaemon(
                    unsafe { env.unsafe_clone() },
                    unsafe { JObject::from_raw(class.clone()) },
                    unsafe { JObject::from_raw(dummy_callback.clone()) },
                );

                if daemon_is_up {
                    assert!(env.exception_check().unwrap());
                    env.exception_clear().unwrap();
                } else {
                    assert!(!env.exception_check().unwrap());
                    daemon_is_up = true;
                }
            } else {
                Java_com_sakethh_linkora_JVMAndAndroidWebCapture_killResultDaemon(
                    unsafe { env.unsafe_clone() },
                    unsafe { JClass::from_raw(class.clone()) },
                    unsafe { JObject::from_raw(dummy_callback.clone()) },
                );

                if daemon_is_up {
                    assert!(!env.exception_check().unwrap());
                    daemon_is_up = false;
                } else {
                    assert!(env.exception_check().unwrap());
                    env.exception_clear().unwrap();
                }
            }

            assert_eq!(
                CAPTURE_RESULT_SENDER.lock().unwrap().is_some(),
                daemon_is_up
            );
            assert_eq!(
                GLOBAL_ON_RESULT_CALLBACK.lock().unwrap().is_some(),
                daemon_is_up
            );
        }

        if daemon_is_up {
            Java_com_sakethh_linkora_JVMAndAndroidWebCapture_killResultDaemon(
                unsafe { env.unsafe_clone() },
                unsafe { JClass::from_raw(class.clone()) },
                unsafe { JObject::from_raw(dummy_callback.clone()) },
            );
        }
    }

    #[test]
    fn direct_monolith_cancellation_during_initial_network_test() {
        let cancel_token = CancelToken(Some(Arc::new(AtomicBool::new(false))));
        let thread_cancel_token = cancel_token.clone();

        // 1. Setup a server that stalls the INITIAL HTML request for 3 seconds
        let port = spawn_mock_http_server(
            Duration::from_secs(3),
            "<html><body>Should not be reached</body></html>",
        );
        let url = format!("http://127.0.0.1:{}/", port);

        // 2. Spawn a background thread to pull the plug after 1 second
        std::thread::spawn(move || {
            std::thread::sleep(Duration::from_secs(1));
            thread_cancel_token.trigger_cancellation();
        });

        // 3. Fire the blocking call inside a catch_unwind
        let web_capture_result = catch_unwind(AssertUnwindSafe(|| {
            get_html_doc(
                url,
                "linkora-test-agent".to_string(),
                5000,
                false, // allow_insecure_protocol
                false, // ignore_doc_errors
                false, // use_css
                false, // embed_fonts
                false, // embed_images
                false, // restrict_js
                false, // include_audio_elements
                false, // include_video_elements
                false, // include_metadata
                false, // log_stuff
                cancel_token,
            )
        }));

        // 4. Assert that the operation panicked
        assert!(
            web_capture_result.is_err(),
            "Expected panic during initial network call"
        );
        let error_message = get_string_from_panic_payload(web_capture_result.unwrap_err());
        assert_eq!(error_message, EXTERNAL_CANCELLATION_PANIC);
    }

    #[test]
    fn direct_monolith_cancellation_during_dom_processing_test() {
        let cancel_token = CancelToken(Some(Arc::new(AtomicBool::new(false))));
        let thread_cancel_token = cancel_token.clone();

        // 1. Stalled Asset Server: Simulates a slow sub-resource (like a heavy image)
        let listener_asset = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
        let port_asset = listener_asset.local_addr().unwrap().port();
        std::thread::spawn(move || {
            if let Ok((mut stream, _)) = listener_asset.accept() {
                use std::io::{Read, Write};
                let mut buf = [0u8; 1024];
                let _ = stream.read(&mut buf);
                std::thread::sleep(Duration::from_secs(3)); // STALL HERE
                let response = "HTTP/1.1 200 OK\r\nConnection: close\r\n\r\n";
                let _ = stream.write_all(response.as_bytes());
            }
        });

        // 2. Fast HTML Server: Responds immediately, but contains a trap pointing to the stalled server
        let listener_html = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
        let port_html = listener_html.local_addr().unwrap().port();
        let html_body = format!(
            "<html><body><img src=\"http://127.0.0.1:{}/stall.jpg\"></body></html>",
            port_asset
        );

        std::thread::spawn(move || {
            if let Ok((mut stream, _)) = listener_html.accept() {
                use std::io::{Read, Write};
                let mut buf = [0u8; 1024];
                let _ = stream.read(&mut buf);
                let response = format!(
                    "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
                    html_body.len(),
                    html_body
                );
                let _ = stream.write_all(response.as_bytes());
            }
        });

        let url = format!("http://127.0.0.1:{}/", port_html);

        // 3. Background thread pulls the plug at 1 second.
        // At this point, the fast HTML server has already responded,
        // and monolith is stuck waiting on the stalled asset server.
        std::thread::spawn(move || {
            std::thread::sleep(Duration::from_secs(1));
            thread_cancel_token.trigger_cancellation();
        });

        let web_capture_result = catch_unwind(AssertUnwindSafe(|| {
            get_html_doc(
                url,
                "linkora-test-agent".to_string(),
                5000,
                false, // allow_insecure_protocol
                false, // ignore_doc_errors
                false, // use_css
                false, // embed_fonts
                true, // embed_images (CRITICAL: Forces monolith to parse DOM and request the image)
                false, // restrict_js
                false, // include_audio_elements
                false, // include_video_elements
                false, // include_metadata
                false, // log_stuff
                cancel_token,
            )
        }));

        assert!(
            web_capture_result.is_err(),
            "Expected panic during DOM processing/asset fetching"
        );
        let error_message = get_string_from_panic_payload(web_capture_result.unwrap_err());
        assert_eq!(error_message, EXTERNAL_CANCELLATION_PANIC);
    }
}
