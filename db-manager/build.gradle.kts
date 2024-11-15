val slf4jApi: String by project
val logbackVersion: String by project
val kotlintestRunner: String by project
val sqliteJdbc: String by project
val h2: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.slf4j:slf4j-api:$slf4jApi")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    runtimeOnly("org.xerial:sqlite-jdbc:$sqliteJdbc")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.kotlintest:kotlintest-runner-junit5:$kotlintestRunner")
    testRuntimeOnly("com.h2database:h2:$h2")
}