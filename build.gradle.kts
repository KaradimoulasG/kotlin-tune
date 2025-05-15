
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.h2)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)

    //Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)

    //Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1") //0.61.0
    implementation(libs.postgresql)
    implementation(libs.postgresql)
    implementation("com.zaxxer:HikariCP:6.3.0")

    // Audio processing
//    implementation("com.googlecode.soundlibs:jlayer:1.0.1")
    implementation("javazoom:jlayer:1.0.1")
    implementation("org.apache.commons:commons-math3:3.6.1") // For the FFT I'm not going to reinvent the wheel


    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.7")

    //Tests
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
