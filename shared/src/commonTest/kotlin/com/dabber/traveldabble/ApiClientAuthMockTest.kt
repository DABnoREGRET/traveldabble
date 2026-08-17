package com.dabber.traveldabble

import com.dabber.traveldabble.data.ApiClient
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ApiClientAuthMockTest {

    @AfterTest
    fun tearDown() {
        ApiClient.setMockHttpClient(null)
        ApiClient.setToken(null)
        ApiClient.telemetryOptOut = false
    }

    @Test
    fun testRegisterSuccess() = runTest {
        var capturedRequest: HttpRequestData? = null

        val mockClient = createMockHttpClient { request ->
            capturedRequest = request
            respond(
                content = """{"token":"jwt-token-123","userId":"user-uuid-1","displayName":"Jane Doe","email":"jane@example.com"}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val response = ApiClient.register("janedoe", "jane@example.com", "secret123", "Jane Doe")

        assertEquals("jwt-token-123", response.token)
        assertEquals("user-uuid-1", response.userId)
        assertEquals("Jane Doe", response.displayName)
        assertEquals("jane@example.com", response.email)

        assertNotNull(capturedRequest)
        assertTrue(capturedRequest!!.url.encodedPath.endsWith("/api/auth/register"))
    }

    @Test
    fun testLoginSuccess() = runTest {
        var capturedRequest: HttpRequestData? = null

        val mockClient = createMockHttpClient { request ->
            capturedRequest = request
            respond(
                content = """{"token":"jwt-login-456","userId":"user-uuid-2","displayName":"John Doe","email":"john@example.com"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val response = ApiClient.login("john@example.com", "pass1234")

        assertEquals("jwt-login-456", response.token)
        assertEquals("user-uuid-2", response.userId)
        assertEquals("John Doe", response.displayName)

        assertNotNull(capturedRequest)
        assertTrue(capturedRequest!!.url.encodedPath.endsWith("/api/auth/login"))
    }

    @Test
    fun testGetMePassesBearerToken() = runTest {
        var authHeader: String? = null

        val mockClient = createMockHttpClient { request ->
            authHeader = request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"token":"","userId":"user-uuid-3","displayName":"Logged User","email":"user@example.com"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        ApiClient.setToken("my-secret-access-token")

        val response = ApiClient.getMe()
        assertEquals("user-uuid-3", response.userId)
        assertEquals("Bearer my-secret-access-token", authHeader)
    }

    @Test
    fun testTelemetryOptOutHeaderIncludedWhenEnabled() = runTest {
        var telemetryHeader: String? = null

        val mockClient = createMockHttpClient { request ->
            telemetryHeader = request.headers["X-Telemetry-Opt-Out"]
            respond(
                content = """{"token":"","userId":"user-uuid-4","displayName":"Private User","email":"private@example.com"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        ApiClient.telemetryOptOut = true

        ApiClient.getMe()
        assertEquals("true", telemetryHeader)
    }

    @Test
    fun testTokenGetterAndSetter() {
        assertNull(ApiClient.getToken())
        ApiClient.setToken("token-abc")
        assertEquals("token-abc", ApiClient.getToken())
        ApiClient.setToken(null)
        assertNull(ApiClient.getToken())
    }
}
