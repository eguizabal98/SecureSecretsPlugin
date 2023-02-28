#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_YOUR_PACKAGE_GOES_HERE_Secrets_getYOUR_KEY_NAME_GOES_HERE(
        JNIEnv* env,
        jobject pThis) {
    std::string api_key = YOUR_KEY_GOES_HERE;
    return env->NewStringUTF(api_key.c_str());
}
