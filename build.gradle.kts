import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("com.github.johnrengelman.shadow") version "7.1.1" apply false
}

allprojects {
    group = "org.aponcet"
    version = "0.1"

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
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