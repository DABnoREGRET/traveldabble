package com.dabber.traveldabble

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Creates a mocked [HttpClient] using [MockEngine] configured with the same
 * [ContentNegotiation] rules and JSON serializer as the production client.
 */
fun createMockHttpClient(handler: MockRequestHandler): HttpClient {
    return HttpClient(MockEngine(handler)) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
            )
        }
    }
}
