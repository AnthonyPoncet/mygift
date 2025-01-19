import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion: String by project
val logbackVersion: String by project
val kotlinxCli: String by project
val kotlintestRunner: String by project
val auth0: String by project

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(project(":auth-server"))
    implementation(project(":db-manager"))
    implementation(project(":front-vue"))
    implementation(project(":model"))

    implementation(kotlin("stdlib-jdk8"))

    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-http-redirect:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlinxCli")

    implementation("it.skrape:skrapeit:1.2.2")

    //implementation("com.github.librepdf:openpdf:1.4.2")
    //implementation("org.sejda.imageio:webp-imageio:0.1.6")
    runtimeOnly("com.twelvemonkeys.imageio:imageio-webp:3.12.0")
    implementation("org.apache.pdfbox:pdfbox:3.0.3")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlintestRunner")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("com.auth0:java-jwt:$auth0")
}

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf("Main-Class" to "org.aponcet.mygift.MainKt"))
    }
}
