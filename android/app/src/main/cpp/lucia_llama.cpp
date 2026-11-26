#include <jni.h>
#include <algorithm>
#include <memory>
#include <mutex>
#include <string>
#include <vector>
#include "llama.h"

namespace {
constexpr int context_size = 4096;
constexpr int batch_size = 128;

struct Model {
    llama_model * weights;
    ~Model() { llama_model_free(weights); }
};

struct Session {
    const llama_vocab * vocab;
    llama_context * context = nullptr;
    llama_sampler * sampler = nullptr;
    int remaining;
    int decoded = 0;
    std::vector<llama_token> prompt;

    ~Session() {
        if (sampler) llama_sampler_free(sampler);
        if (context) llama_free(context);
    }
};
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_zirakusa_gatewayhub_assistant_local_NativeLlama_load(JNIEnv * env, jobject, jstring path) {
    static std::once_flag initialized;
    std::call_once(initialized, [] {
        llama_log_set([](ggml_log_level, const char *, void *) {}, nullptr);
        llama_backend_init();
    });
    const char * filename = env->GetStringUTFChars(path, nullptr);
    if (!filename) return 0;
    auto params = llama_model_default_params();
    params.n_gpu_layers = 0;
    auto * model = llama_model_load_from_file(filename, params);
    env->ReleaseStringUTFChars(path, filename);
    return model ? reinterpret_cast<jlong>(new Model{model}) : 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_zirakusa_gatewayhub_assistant_local_NativeLlama_unload(JNIEnv *, jobject, jlong model) {
    delete reinterpret_cast<Model *>(model);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_zirakusa_gatewayhub_assistant_local_NativeLlama_begin(
        JNIEnv * env, jobject, jlong model, jbyteArray input, jint threads, jint limit, jboolean deterministic, jstring grammar) {
    auto * weights = reinterpret_cast<Model *>(model)->weights;
    auto session = std::make_unique<Session>();
    session->vocab = llama_model_get_vocab(weights);
    session->remaining = limit;

    const auto size = env->GetArrayLength(input);
    std::string prompt(size, '\0');
    env->GetByteArrayRegion(input, 0, size, reinterpret_cast<jbyte *>(prompt.data()));
    session->prompt.resize(context_size - limit);
    const int count = llama_tokenize(session->vocab, prompt.data(), size, session->prompt.data(),
                                     session->prompt.size(), false, true);
    if (count <= 0) return 0;
    session->prompt.resize(count);

    auto params = llama_context_default_params();
    params.n_ctx = context_size;
    params.n_batch = batch_size;
    params.n_ubatch = batch_size;
    params.n_threads = threads;
    params.n_threads_batch = threads;
    params.no_perf = true;
    session->context = llama_init_from_model(weights, params);
    if (!session->context) return 0;

    session->sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    // A GBNF grammar (Lucía's tool choice): the model can only write JSON with a real tool's name. Invalid → none.
    if (grammar != nullptr) {
        const char * text = env->GetStringUTFChars(grammar, nullptr);
        auto * constrained = llama_sampler_init_grammar(session->vocab, text, "root");
        env->ReleaseStringUTFChars(grammar, text);
        if (constrained) llama_sampler_chain_add(session->sampler, constrained);
    }
    if (deterministic) {
        llama_sampler_chain_add(session->sampler, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(session->sampler, llama_sampler_init_top_k(20));
        llama_sampler_chain_add(session->sampler, llama_sampler_init_top_p(0.8f, 1));
        llama_sampler_chain_add(session->sampler, llama_sampler_init_temp(0.7f));
        llama_sampler_chain_add(session->sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    }
    return reinterpret_cast<jlong>(session.release());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_zirakusa_gatewayhub_assistant_local_NativeLlama_prefill(JNIEnv * env, jobject, jlong handle) {
    auto & session = *reinterpret_cast<Session *>(handle);
    const auto remaining = static_cast<int>(session.prompt.size()) - session.decoded;
    if (remaining == 0) return false;
    const int count = std::min(batch_size, remaining);
    if (llama_decode(session.context, llama_batch_get_one(session.prompt.data() + session.decoded, count)) != 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "Could not decode model prompt");
        return false;
    }
    session.decoded += count;
    return session.decoded < static_cast<int>(session.prompt.size());
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_zirakusa_gatewayhub_assistant_local_NativeLlama_next(JNIEnv * env, jobject, jlong handle) {
    auto & session = *reinterpret_cast<Session *>(handle);
    if (session.remaining-- <= 0) return nullptr;
    llama_token token = llama_sampler_sample(session.sampler, session.context, -1);
    if (llama_vocab_is_eog(session.vocab, token)) return nullptr;

    std::vector<char> piece(128);
    int length = llama_token_to_piece(session.vocab, token, piece.data(), piece.size(), 0, false);
    if (length < 0) {
        piece.resize(-length);
        length = llama_token_to_piece(session.vocab, token, piece.data(), piece.size(), 0, false);
    }
    if (length < 0 || llama_decode(session.context, llama_batch_get_one(&token, 1)) != 0) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "Could not decode model token");
        return nullptr;
    }
    auto bytes = env->NewByteArray(length);
    if (bytes && length) env->SetByteArrayRegion(bytes, 0, length, reinterpret_cast<jbyte *>(piece.data()));
    return bytes;
}

extern "C" JNIEXPORT void JNICALL
Java_com_zirakusa_gatewayhub_assistant_local_NativeLlama_end(JNIEnv *, jobject, jlong session) {
    delete reinterpret_cast<Session *>(session);
}
