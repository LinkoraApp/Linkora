use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jbyteArray, jlong};
use jni::JNIEnv;
use monolith::core::{create_monolithic_document, MonolithOutputFormat, Options};

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
pub extern "system" fn Java_com_sakethh_linkora_hoarder_MonolithHoarder_getHTMLPage(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    user_agent: JString,
    timeout: jlong,
    allow_insecure_protocol: jboolean,
    ignore_doc_errors: jboolean,
    use_css: jboolean,
    embed_fonts: jboolean,
    embed_images: jboolean,
    restrict_js: jboolean,
    log_stuff: jboolean,
) -> jbyteArray {
    let url: String = get_string_from_jni(&mut env, url);
    let user_agent: String = get_string_from_jni(&mut env, user_agent);

    match get_html_doc(
        url,
        user_agent,
        timeout as u64,
        allow_insecure_protocol != 0,
        ignore_doc_errors != 0,
        use_css != 0,
        embed_fonts != 0,
        embed_images != 0,
        restrict_js != 0,
        log_stuff != 0,
    ) {
        Ok(html_doc) => match env.byte_array_from_slice(&html_doc) {
            Ok(arr) => **arr,
            Err(e) => {
                env.throw_new("java/lang/RuntimeException", e.to_string())
                    .unwrap();
                JObject::null().into_raw()
            }
        },
        Err(e) => {
            env.throw_new("java/lang/RuntimeException", &e).unwrap();
            JObject::null().into_raw()
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
            no_audio: false,
            no_css: !use_css,
            no_fonts: !embed_fonts,
            no_frames: false,
            no_images: !embed_images,
            no_js: restrict_js,
            no_metadata: false,
            no_video: false,
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
