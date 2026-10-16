#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "ChessLensNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_chess_EvaluationEngine_nativeStockfishVersion(
        JNIEnv* env,
        jobject /* this */) {
    std::string version = "Stockfish 17 (NNUE) Native Bridge";
    return env->NewStringUTF(version.c_str());
}
