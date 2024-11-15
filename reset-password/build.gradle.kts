import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion: String by project
val kotlinxCli: String by project
val slf4jApi: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":db-manager"))

    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlinxCli")

    implementation("org.slf4j:slf4j-api:$slf4jApi")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf("Main-Class" to "org.aponcet.mygift.resetpassword.MainKt"))
    }
}
