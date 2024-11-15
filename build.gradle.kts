import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "2.1.0-RC" apply false
    id("com.github.johnrengelman.shadow") version "7.1.2" apply false
}

allprojects {
    group = "org.aponcet"
    version = "0.1"

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

subprojects {
    repositories {
        maven(url = "https://kotlin.bintray.com/kotlinx")
        mavenCentral()
    }
}