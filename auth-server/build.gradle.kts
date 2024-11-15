import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion: String by project
val kotlinxCli: String by project
val logbackVersion: String by project
val auth0: String by project
val kotlintestRunner: String by project
val h2: String by project

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":db-manager"))
    implementation(project(":model"))

    implementation(kotlin("stdlib-jdk8"))

    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlinxCli")

    implementation("com.auth0:java-jwt:$auth0")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlintestRunner")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testRuntimeOnly("com.h2database:h2:$h2")
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf("Main-Class" to "org.aponcet.authserver.MainKt"))
    }
}
