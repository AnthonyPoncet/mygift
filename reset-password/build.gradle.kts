import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val logbackVersion: String by project

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":db-manager"))

    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3")

    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf("Main-Class" to "org.aponcet.mygift.resetpassword.MainKt"))
    }
}
