#include <jni.h>
#include <string>
#include <vector>
#include "whisper.h"

struct WhisperStreamCallbackCtx {
    JNIEnv *env;
    jobject listener;
    jmethodID onSegmentMethod;
};

static void on_new_segment(
    struct whisper_context *ctx,
    struct whisper_state * /*state*/,
    int n_new,
    void *user_data
) {
    auto *cb = static_cast<WhisperStreamCallbackCtx *>(user_data);
    if (!cb || !cb->env || !cb->listener || !cb->onSegmentMethod) return;

    const int n_segments = whisper_full_n_segments(ctx);
    const int start_idx = n_segments - n_new;
    std::string appended;
    for (int i = (start_idx < 0 ? 0 : start_idx); i < n_segments; ++i) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        if (text) {
            appended += text;
        }
    }
    if (!appended.empty()) {
        jstring jtext = cb->env->NewStringUTF(appended.c_str());
        cb->env->CallVoidMethod(cb->listener, cb->onSegmentMethod, jtext);
        cb->env->DeleteLocalRef(jtext);
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_anongram_app_features_chats_media_voice_stt_WhisperNativeBridge_transcribePcm16k(
    JNIEnv *env,
    jobject /*thiz*/,
    jstring jModelPath,
    jstring jLanguage,
    jfloatArray jPcmFloats,
    jobject jListener
) {
    if (!jModelPath || !jPcmFloats) return env->NewStringUTF("");

    const char *model_path = env->GetStringUTFChars(jModelPath, nullptr);
    const char *lang_cstr = jLanguage ? env->GetStringUTFChars(jLanguage, nullptr) : nullptr;

    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(model_path, cparams);

    env->ReleaseStringUTFChars(jModelPath, model_path);

    if (!ctx) {
        if (lang_cstr) env->ReleaseStringUTFChars(jLanguage, lang_cstr);
        return env->NewStringUTF("");
    }

    jsize n_samples = env->GetArrayLength(jPcmFloats);
    std::vector<float> pcm(static_cast<size_t>(n_samples));
    env->GetFloatArrayRegion(jPcmFloats, 0, n_samples, pcm.data());

    struct whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_realtime = false;
    wparams.print_progress = false;
    wparams.print_timestamps = false;
    wparams.print_special = false;
    wparams.translate = false;
    wparams.n_threads = 4;
    if (lang_cstr && std::string(lang_cstr) != "auto") {
        wparams.language = lang_cstr;
    } else {
        wparams.language = "auto";
    }

    WhisperStreamCallbackCtx cb_ctx{};
    if (jListener != nullptr) {
        jclass cls = env->GetObjectClass(jListener);
        jmethodID mid = env->GetMethodID(cls, "onNewSegment", "(Ljava/lang/String;)V");
        if (mid != nullptr) {
            cb_ctx.env = env;
            cb_ctx.listener = jListener;
            cb_ctx.onSegmentMethod = mid;
            wparams.new_segment_callback = on_new_segment;
            wparams.new_segment_callback_user_data = &cb_ctx;
        }
        env->DeleteLocalRef(cls);
    }

    int rc = whisper_full(ctx, wparams, pcm.data(), static_cast<int>(pcm.size()));
    std::string full_text;
    if (rc == 0) {
        const int n_segments = whisper_full_n_segments(ctx);
        for (int i = 0; i < n_segments; ++i) {
            const char *txt = whisper_full_get_segment_text(ctx, i);
            if (txt) full_text += txt;
        }
    }

    whisper_free(ctx);
    if (lang_cstr) env->ReleaseStringUTFChars(jLanguage, lang_cstr);

    return env->NewStringUTF(full_text.c_str());
}
