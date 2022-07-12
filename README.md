
# Gradle plugin to hide secrets on Android



Fork of [klaxit plugin](https://github.com/klaxit/hidden-secrets-gradle-plugin)

This plugin allows you to obfuscate strings in native code through the Android NDK to prevent credentials harvesting. It allows to obfuscate keys from [gradle.properties] and handle different values depending on the build type.


It uses a combination of obfuscation techniques to do so :

* secret is obfuscated using the reversible XOR operator, so it never appears in plain sight
* obfuscated secret is stored in a NDK binary as an hexadecimal array, so it is really hard to spot / put together from a disassembly
* the obfuscating string is not persisted in the binary to force runtime evaluation (ie : prevent the compiler from disclosing the secret by optimizing the de-obfuscation logic)
* optionally, anyone can provide its own encoding / decoding algorithm when using the plugin to add a security layer.



## Compatibility
This gradle plugin can be used with Android project in Kotlin.

## 1. Install the plugin

```groovy
buildscript {
    repositories {
        google()
        maven {
            url ("Pending")
        }
    }
    dependencies {
        classpath "com.dplg:SecureSecrets:X.Y.Z"
    }
}

apply plugin: 'com.dplg:SecureSecrets'
```
# Secrets Configuration

Put your secrets in gradle properties file

```
SECURE_KEY_[KEY-NAME] = KEY-NAME
SECURE_VALUE_[BUILD-TYPE-NAME]_[KEY-NAME] = [KEY-VALUE] 
```
### Example

```
SECURE_KEY_SERVER_URL = SERVER_URL
SECURE_VALUE_DEBUG_SERVER_URL = googledev.com
```
## Configure plugin
In your gradle file where you aplied the plugin
```
secureExtension {
    buildTypesName = ['debug','release','appQA','appUAT']
    buildTypeKeys = ['DEBUG','RELEASE','APPQA','APPUAT']
    buildTypesSuffix = ['dev', '', 'qa', 'UAT']
}
```
Keep the order of the **buildTypeKeys** and **buildTypesSuffix** according to **buildTypesName List**
* buildTypesName: The name of your Build Types
* buildTypeKeys: Names of BUILD-TYPE-NAME in gradle.file if the name are differents of the original build types
* buildTypesSuffix: Suffix of the differents build types if you have, Example -> com.Example. **SuffixDev**



## Get Secrets in App
Enable C++ files compilation by adding this lines in the Module level build.gradle :

```groovy
android {

    ...

    // Enable NDK build
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
        }
    }
}
```

The plugin will autogenerate the native files with the Secrets depending the build type y the path ```Project/app/src/main/cpp/```


## (Optional) Improve your key security

You can improve the security of your keys by using your own custom encoding / decoding algorithm. The keys will be persisted in C++, additionally encoded using your custom algorithm. The decoding algorithm will also be compiled. So an attacker will also have to reverse-engineer it from compiled C++ to find your keys.

As an example, we will use a rot13 algorithm to encode / decode our key. Of course, don't use rot13 in your own project, it won't provide any additional security. Find your own "secret" encoding/decoding algorithm!

After a rot13 encoding your key ```yourKeyToObfuscate``` becomes ```lbheXrlGbBoshfpngr```. Add it in your app :

```Then in secrets.cpp you need to add your own decoding code in customDecode method:```

```++
void customDecode(char *str) {
    int c = 13;
    int l = strlen(str);
    const char *alpha[2] = { "abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"};
    int i;
    for (i = 0; i < l; i++)
    {
        if (!isalpha(str[i]))
            continue;
        if (isupper(str[i]))
            str[i] = alpha[1][((int)(tolower(str[i]) - 'a') + c) % 26];
        else
            str[i] = alpha[0][((int)(tolower(str[i]) - 'a') + c) % 26];
    }
}
```
This method is automatically called and will revert the rot13 applied on your key when you will call :

```kotlin
Secrets().getYourSecretKeyName(packageName)
```