import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.5.10" apply false
    id("com.github.johnrengelman.shadow") version "6.1.0" apply false
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
        jcenter()
        maven(url = "https://kotlin.bintray.com/kotlinx")
    }
}