plugins {
    id("com.gradle.plugin-publish") version "0.16.0"
    java
    `kotlin-dsl`
    `maven-publish`
}

group = "com.dplg"
version = "1.0.1"

repositories {
    mavenCentral()
    google()
}

gradlePlugin {
    plugins {
        create("SecureSecrets") {
            id = "com.dplg.securesecrets"
            displayName = "Secure Secrets Plugin"
            description = "Secure secrets with native code in android projects"
            implementationClass = "com.dplg.securesecrets.SecureSecrets"
        }
    }
}

pluginBundle {
    mavenCoordinates {
        groupId = "com.dplg"
        artifactId = "SecureSecrets"
        version = "1.0.1"
    }
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
