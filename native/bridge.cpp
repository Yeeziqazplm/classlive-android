#include <jni.h>
#include <transcribe.h>
#include <algorithm>
#include <cstdint>
#include <string>
#include <vector>

namespace {
void fail(JNIEnv *env, const char *operation, int status) {
    const auto message = std::string(operation) + ": " + transcribe_status_string(status);
    env->ThrowNew(env->FindClass("java/io/IOException"), message.c_str());
}
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_yeezi_classlive_OfflineAsr_openNative(JNIEnv *env, jclass, jstring path, jint threads) {
    const char *file = env->GetStringUTFChars(path, nullptr);
    if (!file) return 0;
    transcribe_model_load_params load;
    transcribe_model_load_params_init(&load);
    load.backend = TRANSCRIBE_BACKEND_CPU;
    transcribe_session_params config;
    transcribe_session_params_init(&config);
    config.n_threads = threads;
    transcribe_session *session = nullptr;
    const auto status = transcribe_open(file, &load, &config, &session);
    env->ReleaseStringUTFChars(path, file);
    if (status != TRANSCRIBE_OK) {
        fail(env, "Parakeet model load", status);
        return 0;
    }
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_yeezi_classlive_OfflineAsr_decodeNative(JNIEnv *env, jclass, jlong handle, jbyteArray pcm) {
    auto *session = reinterpret_cast<transcribe_session *>(handle);
    const auto size = env->GetArrayLength(pcm);
    if (!session || size <= 0 || size % 2 || size > 400000) {
        env->ThrowNew(env->FindClass("java/io/IOException"), "Invalid PCM block");
        return nullptr;
    }
    std::vector<jbyte> bytes(size);
    env->GetByteArrayRegion(pcm, 0, size, bytes.data());
    if (env->ExceptionCheck()) return nullptr;
    // The model also gets a short silent tail to settle the last word.
    std::vector<float> samples(size / 2 + 6400);
    int peak = 0;
    for (int i = 0; i < size / 2; ++i) {
        const unsigned value = static_cast<uint8_t>(bytes[2*i])
                | (static_cast<unsigned>(static_cast<uint8_t>(bytes[2*i+1])) << 8);
        const int sample = value < 32768 ? value : static_cast<int>(value) - 65536;
        peak = std::max(peak, std::abs(sample));
        samples[i] = sample / 32768.0f;
    }
    if (peak == 0) return env->NewByteArray(0);
    transcribe_run_params params;
    transcribe_run_params_init(&params);
    params.timestamps = TRANSCRIBE_TIMESTAMPS_WORD;
    const auto status = transcribe_run(session, samples.data(), samples.size(), &params);
    if (status != TRANSCRIBE_OK) {
        fail(env, "Parakeet inference", status);
        return nullptr;
    }
    std::string result;
    const int count = transcribe_n_words(session);
    bool valid = count > 0;
    for (int i = 0; i < count; ++i) {
        transcribe_word word;
        transcribe_word_init(&word);
        if (transcribe_get_word(session, i, &word) != TRANSCRIBE_OK || !word.text || word.t1_ms <= word.t0_ms) {
            valid = false;
            break;
        }
        result += std::to_string(word.t0_ms) + '\t' + std::to_string(word.t1_ms) + '\t' + word.text + '\n';
    }
    if (!valid) {
        const char *full = transcribe_full_text(session);
        result = full ? std::string("-1\t-1\t") + full : "";
    }
    const auto length = static_cast<jsize>(result.size());
    auto output = env->NewByteArray(length);
    if (output) env->SetByteArrayRegion(output, 0, length, reinterpret_cast<const jbyte *>(result.data()));
    return output;
}

extern "C" JNIEXPORT void JNICALL
Java_com_yeezi_classlive_OfflineAsr_closeNative(JNIEnv *, jclass, jlong handle) {
    transcribe_session_free(reinterpret_cast<transcribe_session *>(handle));
}
