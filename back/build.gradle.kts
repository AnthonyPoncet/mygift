import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion: String by project

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":auth-server"))
    implementation(project(":db-manager"))
    implementation(project(":front"))
    implementation(project(":model"))

    implementation(kotlin("stdlib-jdk8"))

    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3")
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes(mapOf("Main-Class" to "org.aponcet.mygift.MainKt"))
    }
}
