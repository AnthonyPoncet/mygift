import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":db-manager"))

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3")
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes(mapOf("Main-Class" to "org.aponcet.mygift.resetpassword.MainKt"))
    }
}
