package com.dabber.traveldabble.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Default base URL for the backend API:
 * - Production: https://travel-dabble-server.onrender.com
 * - Configurable via Settings -> Server Connection in the app.
 */
expect val DEFAULT_BASE_URL: String

/**
 * Creates a platform-appropriate [HttpClient].
 *
 * The actual is resolved per-target: Android uses the OkHttp-based Android
 * engine, the JVM target uses CIO. Engine selection must live in platform
 * source sets because engines are not available in common code.
 */
expect fun createHttpClient(): HttpClient

/**
 * Shared client configuration applied by every platform actual.
 * JSON is lenient and ignores unknown keys so new server fields never
 * break older clients; non-2xx responses raise [io.ktor.client.plugins.ResponseException].
 */
internal fun <T : HttpClientEngineConfig> createPlatformClient(engine: HttpClientEngineFactory<T>): HttpClient =
    HttpClient(engine) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }
