import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion: String by project
val logbackVersion: String by project

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":db-manager"))
    implementation(project(":model"))

    implementation(kotlin("stdlib-jdk8"))

    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3")

    implementation("com.auth0:java-jwt:3.3.0")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.9")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testRuntimeOnly("com.h2database:h2:1.4.200")
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf("Main-Class" to "org.aponcet.authserver.MainKt"))
    }
}
