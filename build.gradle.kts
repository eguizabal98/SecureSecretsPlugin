plugins {
    id("com.gradle.plugin-publish") version "0.16.0"
    java
    `kotlin-dsl`
    `maven-publish`
}

group = "com.plugeem"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

gradlePlugin {
    plugins {
        create("SecureSecretsPlugin") {
            id = "com.plugeem.secureSecrets"
            displayName = "Secure Secrets Plugin"
            description = "This plugin allows any Android developer" +
                    " to deeply hide secrets in its project to prevent credentials harvesting."
            implementationClass = "com.plugeem.securesecrets.SecureSecretsPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/eguizabal98/SecureSecretsPlugin"
    vcsUrl = "https://github.com/eguizabal98/SecureSecretsPlugin.git"
    tags = listOf("plugin", "android", "hide", "secret", "key", "string", "obfuscate")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.android.tools.build:gradle:4.1.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<Copy> {
    //Required by Gradle 7.0
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.withType<Test> {
    useJUnitPlatform()
}
