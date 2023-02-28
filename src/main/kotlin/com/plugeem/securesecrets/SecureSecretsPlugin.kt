package com.plugeem.securesecrets

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.plugeem.securesecrets.Utils.capitalizeString
import java.io.File
import java.nio.charset.Charset
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.create

/**
 * Available gradle tasks from HiddenSecretsPlugin
 */

fun Project.android(): BaseExtension =
    this.extensions.findByType(BaseExtension::class.java) ?: throw GradleException("Not android project")

fun BaseExtension.variant(): DomainObjectSet<out BaseVariant> = when (this) {
    is AppExtension -> applicationVariants
    else -> throw GradleException("Unsupported extension")
}

open class SecureSecretsPlugin : Plugin<Project> {
    companion object {
        private const val APP_MAIN_FOLDER = "src/main/"
        private const val KEY_PLACEHOLDER = "YOUR_KEY_GOES_HERE"
        private const val PACKAGE_PLACEHOLDER = "YOUR_PACKAGE_GOES_HERE"
        private const val KOTLIN_FILE_NAME = "Secrets.kt"
        private const val EXTENSION_NAME = "secureExtension"

        //Tasks
        const val TASK_UNZIP_HIDDEN_SECRETS = "unzipHiddenSecrets"
        const val TASK_COPY_CPP = "copyCpp"
        const val TASK_COPY_KOTLIN = "copyKotlin"
        const val TASK_OBFUSCATE = "obfuscate"
        const val TASK_PACKAGE_NAME = "packageName"
        const val TASK_FIND_KOTLIN_FILE = "findKotlinFile"
        const val TASK_HIDE_MULTIPLE_SECRETS = "hideMultipleSecrets"

        //Properties
        private const val KEY = "key"
        private const val PACKAGE = "package"

        //Errors
        private const val ERROR_EMPTY_KEY = "No key provided, use argument '-Pkey=yourKey'"
        private const val ERROR_EMPTY_PACKAGE = "Empty package name, use argument '-Ppackage=your.package.name'"
    }

    override fun apply(project: Project) {

        val tmpFolder: String = java.lang.String.format("%s\\hidden-secrets-tmp", project.buildDir)
        val extension = project.extensions.create<SecureSecretsPluginExtension>(EXTENSION_NAME)

        project.android().variant().all {
            createNoEncodedParamTask(project, this, tmpFolder, extension)
        }

        /**
         * Get key param from command line
         */
        @Input
        fun getKeyParam(): String {
            val key: String
            if (project.hasProperty(KEY)) {
                //From command line
                key = project.property(KEY) as String
            } else {
                throw InvalidUserDataException(ERROR_EMPTY_KEY)
            }
            return key
        }

        /**
         * Generate en encoded key from command line params
         */
        fun getObfuscatedKey(): String {
            val key = getKeyParam()
            println("### SECRET ###\n$key\n")

            val packageName = getPackageNameParam(project)
            println("### PACKAGE NAME ###\n$packageName\n")

            val encodedKey = Utils.encodeSecret(key, packageName)
            println("### OBFUSCATED SECRET ###\n$encodedKey")
            return encodedKey
        }

        /**
         * Unzip plugin into tmp directory
         */
        project.tasks.create(
            TASK_UNZIP_HIDDEN_SECRETS, Copy::
            class.java,
            object : Action<Copy?> {
                @TaskAction
                override fun execute(copy: Copy) {
                    // in the case of buildSrc dir
                    copy.from(project.zipTree(javaClass.protectionDomain.codeSource.location!!.toExternalForm()))
                    println("Unzip jar to $tmpFolder")
                    copy.into("$tmpFolder/temp")
                }
            })

        /**
         * Copy C++ files to your project
         */
        project.task(TASK_COPY_CPP)
        {
            doLast {
                copyCppNoEncodedFiles(project, tmpFolder)
            }
        }

        /**
         * Copy Kotlin file to your project
         */
        project.task(TASK_COPY_KOTLIN)
        {
            doLast {
                copyKotlinFile(project, tmpFolder)
            }
        }

        /**
         * Get an obfuscated key from command line
         */
        project.task(TASK_OBFUSCATE)
        {
            doLast {
                getObfuscatedKey()
            }
        }

        /**
         * Print the package name of the app
         */
        project.task(TASK_PACKAGE_NAME)
        {
            doLast {
                println("APP PACKAGE NAME = " + getPackageNameParam(project))
            }
        }

        /**
         * Find Secrets.kt file in the project
         */
        project.task(TASK_FIND_KOTLIN_FILE)
        {
            doLast {
                getKotlinFile(project, tmpFolder)
            }
        }
    }

    private fun createNoEncodedParamTask(
        project: Project,
        variant: BaseVariant,
        tmpFolder: String,
        extension: SecureSecretsPluginExtension
    ) {
        // Get different variants of the project
        variant.javaCompileProvider.dependsOn("$TASK_HIDE_MULTIPLE_SECRETS${variant.name}")
        project.task("$TASK_HIDE_MULTIPLE_SECRETS${variant.name}") {
            dependsOn(TASK_UNZIP_HIDDEN_SECRETS)

            doLast {
                val buildTypeIndex: Int?
                var buildTypesSuffix: String? = null

                buildTypeIndex = extension.buildTypesName.get().indexOf(variant.name)

                val buildTypeKey = if (buildTypeIndex != null && buildTypeIndex >= 0) {
                    try {
                        extension.buildTypeKeys.get()[buildTypeIndex]
                    } catch (e: Exception) {
                        variant.name
                    }
                } else {
                    variant.name
                }

                println("Elias-- $buildTypeIndex - ${extension.buildTypesName.get()}")
                if (extension.buildTypesSuffix.get().isNotEmpty() && buildTypeIndex >= 0) {
                    buildTypesSuffix = extension.buildTypesSuffix.get()[buildTypeIndex]
                }

                //Copy files if they don't exist
                copyCppNoEncodedFiles(project, tmpFolder)
                copyKotlinFile(project, tmpFolder)

                val packageName = getPackageNameParam(project)

                val properList: MutableMap<String, *> = project.properties
                val keyMapList = properList.filter { it.key.contains("SECURE_KEY") }
                val keyList = arrayListOf<String>()
                keyMapList.forEach {
                    keyList.add(it.key.replace("SECURE_KEY_", ""))
                }

                //Add method in Kotlin code
                var secretsKotlinB = getKotlinFile(project, tmpFolder)
                if (secretsKotlinB == null) {
                    //File not found in project
                    secretsKotlinB = getKotlinDestination(packageName, KOTLIN_FILE_NAME, project)
                }

                keyList.forEach { key ->
                    var keyName = properList.getValue("SECURE_KEY_${key}").toString()
                    keyName = keyName.replace("_", " ").capitalizeString().replace(" ", "")
                    println("Elias-- $buildTypesSuffix")
                    val obfuscatedKey = properList.getValue("SECURE_VALUE_${buildTypeKey}_${key}").toString()

                    if (secretsKotlinB.exists()) {
                        var text = secretsKotlinB.readText(Charset.defaultCharset())
                        text = text.replace(PACKAGE_PLACEHOLDER, packageName)
                        if (text.contains(keyName)) {
                            println("⚠️ Method already added in Kotlin !")
                        }
                        text = text.dropLast(3)
                        text += CodeGenerator.getKotlinNoEncodedCode(keyName)
                        secretsKotlinB.writeText(text.plus("\n"))
                    } else {
                        error("Missing Kotlin file, please run gradle task : $TASK_COPY_KOTLIN")
                    }

                    //Resolve package name for C++ from the one used in Kotlin file
                    var kotlinPackage = Utils.getKotlinFilePackage(secretsKotlinB)
                    if (kotlinPackage.isEmpty()) {
                        println("Empty package in $KOTLIN_FILE_NAME")
                        kotlinPackage = packageName
                    }

                    //Add obfuscated key in C++ code
                    val secretsCpp = getCppDestination("secrets.cpp", project)
                    if (secretsCpp.exists()) {
                        var text = secretsCpp.readText(Charset.defaultCharset())
                        if (text.contains(obfuscatedKey)) {
                            println("⚠️ Key already added in C++ !")
                        }
                        if (text.contains(KEY_PLACEHOLDER)) {
                            //Edit placeholder key
                            //Replace package name
                            text = text.replace(PACKAGE_PLACEHOLDER, Utils.getSnakeCasePackageName(kotlinPackage))
                            //Replace key name
                            text = text.replace("YOUR_KEY_NAME_GOES_HERE", keyName)
                            //Replace demo key
                            text = text.replace(KEY_PLACEHOLDER, obfuscatedKey)
                            secretsCpp.writeText(text)
                        } else {
                            //Add new key
                            text += CodeGenerator.getCppNoEncodedCode(kotlinPackage, keyName, obfuscatedKey)
                            secretsCpp.writeText(text)
                        }
                    } else {
                        error("Missing C++ file, please run gradle task : $TASK_COPY_CPP")
                    }
                    println("✅ You can now get your secret key by calling : Secrets().get$keyName()")
                }

            }
        }
    }


    private fun copyCppNoEncodedFiles(project: Project, tmpFolder: String) {
        project.file("$tmpFolder/temp/cpp/").listFiles()?.forEach {
            val cppDestination = getCppDestination(it.name, project)
            if (cppDestination.exists()) {
                println("${it.name} already exists")
                cppDestination.delete()
                it.copyTo(cppDestination, true)
            } else {
                println("Copy $it.name to\n$cppDestination")
                it.copyTo(cppDestination, true)
            }
        }
    }

    /**
     * Copy Kotlin file Secrets.kt from the lib to the Android project if it does not exist yet
     */
    private fun copyKotlinFile(project: Project, tmpFolder: String) {
        getKotlinFile(project, tmpFolder)?.let {
            println("$KOTLIN_FILE_NAME already exists")
        }
        val packageName = getPackageNameParam(project)
        project.file("$tmpFolder/temp/kotlin/").listFiles()?.forEach {
            val ktDestination = getKotlinDestination(packageName, it.name, project)
            if (ktDestination.exists()) {
                println("${it.name} already exists")
                ktDestination.delete()
                it.copyTo(ktDestination, true)
            } else {
                println("Copy $it.name to\n$ktDestination")
                it.copyTo(ktDestination, true)
            }
        }
    }

    /**
     * If found, returns the Secrets.kt file in the Android app
     */
    private fun getKotlinFile(project: Project, tmpFolder: String): File? {
        return Utils.findFileInProject(project, "$tmpFolder/$APP_MAIN_FOLDER", KOTLIN_FILE_NAME)
    }

    @OutputFile
    fun getCppDestination(fileName: String, project: Project): File {
        return project.file(APP_MAIN_FOLDER + "cpp/$fileName")
    }

    /**
     * Get package name param from command line
     */
    @Input
    fun getPackageNameParam(project: Project): String {
        var packageName: String? = null
        if (project.hasProperty(PACKAGE)) {
            //From command line
            packageName = project.property(PACKAGE) as String?
        }
        if (packageName.isNullOrEmpty()) {
            //From Android app
            packageName = getAppPackageName(project)
        }
        if (packageName.isNullOrEmpty()) {
            throw InvalidUserDataException(ERROR_EMPTY_PACKAGE)
        }
        return packageName
    }

    /**
     * Get the package name of the Android app on which this plugin is used
     */
    private fun getAppPackageName(project: Project): String? {
        val androidExtension = project.extensions.getByName("android")

        if (androidExtension is AppExtension) {
            return androidExtension.defaultConfig.applicationId
        }
        return null
    }

    @OutputFile
    fun getKotlinDestination(packageName: String, fileName: String, project: Project): File {
        var path = APP_MAIN_FOLDER + "java/"
        packageName.split(".").forEach {
            path += "$it/"
        }
        val directory = project.file(path)
        if (!directory.exists()) {
            println("Directory $path does not exist in the project, you might have selected a wrong package.")
        }
        path += fileName
        return project.file(path)
    }
}
