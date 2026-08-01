#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "SurakshakNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_alphagroup_surakshak_c2pa_C2PAManifestBuilderImpl_getNativeVersion(
        JNIEnv* env,
        jobject /* this */) {
    std::string version = "C2PA Native Stub 1.0";
    return env->NewStringUTF(version.c_str());
}
