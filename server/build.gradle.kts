plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

group = "com.dabber.traveldabble"
version = "1.0.0"

application {
    mainClass.set("com.dabber.traveldabble.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.logback)

    // Ktor server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Ktor client (for OSRM, OpenWeatherMap, OpenRouter proxy, Web search)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // DI
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.hikaricp)
    implementation(libs.postgres)
    implementation(libs.h2)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)

    // New dependencies
    implementation(libs.bcrypt)
    implementation(libs.java.jwt)

    // Test
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

tasks.test {
    maxParallelForks = 1
}

