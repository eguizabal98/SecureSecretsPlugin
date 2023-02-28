package com.plugeem.securesecrets

/**
 * Helper to generate C++ and Kotlin code
 */
object CodeGenerator {

    fun getCppNoEncodedCode(packageName: String, keyName: String, obfuscatedKey: String): String {

        return "\nextern \"C\" JNIEXPORT jstring JNICALL\n" +
                "Java_" + Utils.getSnakeCasePackageName(packageName) + "_Secrets_get$keyName(\n" +
                "        JNIEnv* env,\n" +
                "        jobject pThis) {\n" +
                "     std::string api_key = $obfuscatedKey;\n" +
                "     return env->NewStringUTF(api_key.c_str());\n" +
                "}\n"
    }

    fun getKotlinNoEncodedCode(keyName: String): String {
        return "\n    external fun get$keyName(): String\n" +
                "}"
    }
}
