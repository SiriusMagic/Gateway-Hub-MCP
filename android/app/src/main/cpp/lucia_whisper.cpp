#include <jni.h>
#include <algorithm>
#include <memory>
#include <string>
#include <vector>
#include "whisper.h"

namespace {
struct Cancellation {
    JavaVM * vm;
    jobject flag;
    jmethodID get;
};

// A backend may run the callback on a worker thread. JNI environments belong to their threads.
bool cancelled(void * data) {
    const auto & signal = *static_cast<Cancellation *>(data);
    JNIEnv * env = nullptr;
    bool attached = false;
    if (signal.vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) == JNI_EDETACHED) {
        if (signal.vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return true;
        attached = true;
    }
    const bool stop = !env || env->CallBooleanMethod(signal.flag, signal.get);
    if (attached) signal.vm->DetachCurrentThread();
    return stop;
}
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_zirakusa_gatewayhub_assistant_NativeWhisper_recognize(
        JNIEnv * env, jobject, jstring path, jshortArray samples, jint length, jint threads, jobject stop) {
    if (length <= 0 || length > env->GetArrayLength(samples) || length > WHISPER_SAMPLE_RATE * 8) return nullptr;
    const char * filename = env->GetStringUTFChars(path, nullptr);
    if (!filename) return nullptr;
    auto contextParams = whisper_context_default_params();
    contextParams.use_gpu = false;
    std::unique_ptr<whisper_context, decltype(&whisper_free)> context(
        whisper_init_from_file_with_params(filename, contextParams), whisper_free);
    env->ReleaseStringUTFChars(path, filename);
    if (!context) return nullptr;

    std::vector<jshort> input(length);
    env->GetShortArrayRegion(samples, 0, length, input.data());
    if (env->ExceptionCheck()) return nullptr;
    std::vector<float> pcm(length);
    std::transform(input.begin(), input.end(), pcm.begin(), [](jshort sample) { return sample / 32768.0f; });

    Cancellation signal{};
    env->GetJavaVM(&signal.vm);
    signal.flag = env->NewGlobalRef(stop);
    const auto flagType = env->GetObjectClass(stop);
    signal.get = env->GetMethodID(flagType, "get", "()Z");
    env->DeleteLocalRef(flagType);
    if (!signal.flag || !signal.get) {
        if (signal.flag) env->DeleteGlobalRef(signal.flag);
        return nullptr;
    }

    auto params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = "es";
    params.n_threads = threads;
    params.no_context = true;
    params.no_timestamps = true;
    params.single_segment = true;
    params.suppress_blank = true;
    // Each encoder position represents 20 ms. Keep one second of padding for short commands.
    params.audio_ctx = std::min(whisper_n_audio_ctx(context.get()), std::max(160, (length + 319) / 320 + 50));
    params.max_tokens = 96;
    params.temperature_inc = 0.0f;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.abort_callback = cancelled;
    params.abort_callback_user_data = &signal;

    const int result = cancelled(&signal) ? -1 : whisper_full(context.get(), params, pcm.data(), length);
    env->DeleteGlobalRef(signal.flag);
    if (result != 0) return nullptr;

    std::string text;
    for (int i = 0; i < whisper_full_n_segments(context.get()); ++i) {
        text += whisper_full_get_segment_text(context.get(), i);
    }
    const auto bytes = static_cast<jsize>(text.size());
    jbyteArray output = env->NewByteArray(bytes);
    if (output) env->SetByteArrayRegion(output, 0, bytes, reinterpret_cast<const jbyte *>(text.data()));
    return output;
}
