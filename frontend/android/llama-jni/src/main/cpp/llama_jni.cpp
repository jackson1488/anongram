#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <cstring>
#include <mutex>
#include <string>
#include <vector>
#include "llama.h"

#define TAG "AnonGramLlama"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

std::mutex g_mutex;
std::atomic<bool> g_stop_requested{false};
bool g_backend_initialized = false;

llama_model *g_model = nullptr;
llama_context *g_ctx = nullptr;
std::string g_loaded_path;
int g_loaded_ctx_size = 0;
int g_loaded_threads = 0;

void free_loaded_locked() {
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
    g_loaded_path.clear();
    g_loaded_ctx_size = 0;
    g_loaded_threads = 0;
}

// Returns the length of the longest prefix consisting only of complete UTF-8 sequences.
size_t utf8_complete_prefix_len(const std::string &s) {
    size_t i = 0;
    const size_t n = s.size();
    while (i < n) {
        unsigned char c = static_cast<unsigned char>(s[i]);
        size_t char_len = 1;
        if ((c & 0x80) == 0x00) {
            char_len = 1;
        } else if ((c & 0xE0) == 0xC0) {
            char_len = 2;
        } else if ((c & 0xF0) == 0xE0) {
            char_len = 3;
        } else if ((c & 0xF8) == 0xF0) {
            char_len = 4;
        } else {
            // Invalid leading byte, emit 1 byte safely replaced later or skip
            i += 1;
            continue;
        }
        if (i + char_len > n) {
            break;
        }
        i += char_len;
    }
    return i;
}

std::string format_fallback_chatml(const std::vector<std::string> &roles, const std::vector<std::string> &contents) {
    std::string out;
    for (size_t i = 0; i < roles.size(); ++i) {
        out += "<|im_start|>";
        out += roles[i];
        out += "\n";
        out += contents[i];
        out += "<|im_end|>\n";
    }
    out += "<|im_start|>assistant\n";
    return out;
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_anongram_llama_LlamaNativeBridge_nativeLoadModel(
    JNIEnv *env,
    jobject /*thiz*/,
    jstring jModelPath,
    jint nCtx,
    jint nThreads
) {
    if (!jModelPath) return JNI_FALSE;
    const char *path_cstr = env->GetStringUTFChars(jModelPath, nullptr);
    std::string model_path(path_cstr ? path_cstr : "");
    env->ReleaseStringUTFChars(jModelPath, path_cstr);
    if (model_path.empty()) return JNI_FALSE;

    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_backend_initialized) {
        llama_backend_init();
        g_backend_initialized = true;
    }

    if (g_model && g_ctx && g_loaded_path == model_path &&
        g_loaded_ctx_size == nCtx && g_loaded_threads == nThreads) {
        return JNI_TRUE;
    }

    free_loaded_locked();

    llama_model_params mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(model_path.c_str(), mparams);
    if (!g_model) {
        LOGE("Failed to load GGUF model: %s", model_path.c_str());
        return JNI_FALSE;
    }

    llama_context_params cparams = llama_context_default_params();
    cparams.n_ctx = static_cast<uint32_t>(nCtx > 256 ? nCtx : 2048);
    cparams.n_batch = 256;
    cparams.n_threads = nThreads > 0 ? nThreads : 4;
    cparams.n_threads_batch = cparams.n_threads;

    g_ctx = llama_init_from_model(g_model, cparams);
    if (!g_ctx) {
        LOGE("Failed to create llama_context");
        free_loaded_locked();
        return JNI_FALSE;
    }

    g_loaded_path = model_path;
    g_loaded_ctx_size = static_cast<int>(cparams.n_ctx);
    g_loaded_threads = static_cast<int>(cparams.n_threads);
    LOGI("Loaded GGUF model %s (ctx=%d, threads=%d)", model_path.c_str(), g_loaded_ctx_size, g_loaded_threads);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_anongram_llama_LlamaNativeBridge_nativeUnloadModel(
    JNIEnv * /*env*/,
    jobject /*thiz*/
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    free_loaded_locked();
}

extern "C" JNIEXPORT void JNICALL
Java_com_anongram_llama_LlamaNativeBridge_nativeRequestStop(
    JNIEnv * /*env*/,
    jobject /*thiz*/
) {
    g_stop_requested.store(true);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_anongram_llama_LlamaNativeBridge_nativeGenerate(
    JNIEnv *env,
    jobject /*thiz*/,
    jobjectArray jRoles,
    jobjectArray jContents,
    jfloat temperature,
    jint maxTokens,
    jobject jListener
) {
    if (!jRoles || !jContents || !jListener) return -1;

    jsize count = env->GetArrayLength(jRoles);
    if (count <= 0 || env->GetArrayLength(jContents) != count) return -2;

    std::vector<std::string> roles;
    std::vector<std::string> contents;
    roles.reserve(count);
    contents.reserve(count);

    for (jsize i = 0; i < count; ++i) {
        auto jRole = static_cast<jstring>(env->GetObjectArrayElement(jRoles, i));
        auto jContent = static_cast<jstring>(env->GetObjectArrayElement(jContents, i));
        const char *r = jRole ? env->GetStringUTFChars(jRole, nullptr) : "";
        const char *c = jContent ? env->GetStringUTFChars(jContent, nullptr) : "";
        roles.emplace_back(r ? r : "");
        contents.emplace_back(c ? c : "");
        if (jRole) {
            env->ReleaseStringUTFChars(jRole, r);
            env->DeleteLocalRef(jRole);
        }
        if (jContent) {
            env->ReleaseStringUTFChars(jContent, c);
            env->DeleteLocalRef(jContent);
        }
    }

    jclass listenerCls = env->GetObjectClass(jListener);
    jmethodID onTokenMethod = env->GetMethodID(listenerCls, "onToken", "(Ljava/lang/String;)V");
    env->DeleteLocalRef(listenerCls);
    if (!onTokenMethod) return -3;

    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model || !g_ctx) return -4;

    g_stop_requested.store(false);
    llama_memory_clear(llama_get_memory(g_ctx), true);

    std::vector<llama_chat_message> chat_msgs;
    chat_msgs.reserve(roles.size());
    for (size_t i = 0; i < roles.size(); ++i) {
        chat_msgs.push_back({roles[i].c_str(), contents[i].c_str()});
    }

    std::string formatted_prompt;
    const char *tmpl = llama_model_chat_template(g_model, nullptr);
    if (tmpl != nullptr) {
        int32_t needed = llama_chat_apply_template(tmpl, chat_msgs.data(), chat_msgs.size(), true, nullptr, 0);
        if (needed > 0) {
            std::vector<char> buf(static_cast<size_t>(needed) + 16, '\0');
            int32_t written = llama_chat_apply_template(tmpl, chat_msgs.data(), chat_msgs.size(), true, buf.data(), static_cast<int32_t>(buf.size()));
            if (written > 0) {
                formatted_prompt.assign(buf.data(), static_cast<size_t>(written));
            }
        }
    }
    if (formatted_prompt.empty()) {
        formatted_prompt = format_fallback_chatml(roles, contents);
    }

    const llama_vocab *vocab = llama_model_get_vocab(g_model);
    const int32_t n_prompt_cap = -llama_tokenize(
        vocab,
        formatted_prompt.c_str(),
        static_cast<int32_t>(formatted_prompt.size()),
        nullptr,
        0,
        true,
        true
    );
    if (n_prompt_cap <= 0) return -5;

    std::vector<llama_token> prompt_tokens(static_cast<size_t>(n_prompt_cap));
    int32_t n_prompt = llama_tokenize(
        vocab,
        formatted_prompt.c_str(),
        static_cast<int32_t>(formatted_prompt.size()),
        prompt_tokens.data(),
        static_cast<int32_t>(prompt_tokens.size()),
        true,
        true
    );
    if (n_prompt <= 0) return -6;
    prompt_tokens.resize(static_cast<size_t>(n_prompt));

    const int n_ctx = static_cast<int>(llama_n_ctx(g_ctx));
    if (n_prompt >= n_ctx - 16) {
        // Keep the latest tokens if prompt exceeds context
        int keep = std::max(16, n_ctx - 256);
        prompt_tokens.erase(prompt_tokens.begin(), prompt_tokens.end() - keep);
        n_prompt = static_cast<int32_t>(prompt_tokens.size());
    }

    // Decode prompt in batches of 256
    const int32_t batch_size = 256;
    for (int32_t pos = 0; pos < n_prompt; pos += batch_size) {
        if (g_stop_requested.load()) return 0;
        int32_t chunk = std::min(batch_size, n_prompt - pos);
        llama_batch batch = llama_batch_get_one(prompt_tokens.data() + pos, chunk);
        if (llama_decode(g_ctx, batch) != 0) {
            LOGE("llama_decode failed during prompt eval");
            return -7;
        }
    }

    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    llama_sampler *smpl = llama_sampler_chain_init(sparams);
    if (temperature <= 0.01f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.92f, 1));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    }

    std::string utf8_buf;
    int generated = 0;
    const int limit = maxTokens > 0 ? maxTokens : 768;

    while (generated < limit && (n_prompt + generated) < n_ctx - 2) {
        if (g_stop_requested.load()) break;

        llama_token new_token = llama_sampler_sample(smpl, g_ctx, -1);
        if (llama_vocab_is_eog(vocab, new_token)) {
            break;
        }

        char piece[256];
        int32_t n_piece = llama_token_to_piece(vocab, new_token, piece, sizeof(piece), 0, true);
        if (n_piece > 0) {
            utf8_buf.append(piece, static_cast<size_t>(n_piece));
            size_t valid_len = utf8_complete_prefix_len(utf8_buf);
            if (valid_len > 0) {
                std::string emit = utf8_buf.substr(0, valid_len);
                utf8_buf.erase(0, valid_len);
                jstring jPiece = env->NewStringUTF(emit.c_str());
                env->CallVoidMethod(jListener, onTokenMethod, jPiece);
                env->DeleteLocalRef(jPiece);
            }
        }

        llama_batch next_batch = llama_batch_get_one(&new_token, 1);
        if (llama_decode(g_ctx, next_batch) != 0) {
            LOGE("llama_decode failed during generation");
            break;
        }
        generated++;
    }

    llama_sampler_free(smpl);
    return 0;
}
