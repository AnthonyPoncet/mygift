plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.slf4j:slf4j-api:1.7.25")
    runtimeOnly("org.xerial:sqlite-jdbc:3.25.2")

    testImplementation(kotlin("test-junit"))
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.9")
    testRuntimeOnly("com.h2database:h2:1.4.200")
}