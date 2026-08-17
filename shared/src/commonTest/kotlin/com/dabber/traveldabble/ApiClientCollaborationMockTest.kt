package com.dabber.traveldabble

import com.dabber.traveldabble.data.ApiClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ApiClientCollaborationMockTest {

    @AfterTest
    fun tearDown() {
        ApiClient.setMockHttpClient(null)
        ApiClient.setToken(null)
    }

    @Test
    fun testGetTripMembers() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = """
                [
                  {"userId":"user-1","displayName":"Alice","email":"alice@example.com","role":"owner","joinedAt":1700000000},
                  {"userId":"user-2","displayName":"Bob","email":"bob@example.com","role":"member","joinedAt":1700001000}
                ]
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val members = ApiClient.getTripMembers("trip-123")

        assertEquals(2, members.size)
        assertEquals("owner", members[0].role)
        assertEquals("Alice", members[0].displayName)
        assertEquals("member", members[1].role)
        assertEquals("Bob", members[1].displayName)
    }

    @Test
    fun testGenerateInviteCode() = runTest {
        var method: HttpMethod? = null

        val mockClient = createMockHttpClient { request ->
            method = request.method
            respond(
                content = """{"id":"inv-1","code":"ABCDEF12","expiresAt":1700086400}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val response = ApiClient.generateInviteCode("trip-123", maxUses = 5, expiresInHours = 24)

        assertEquals(HttpMethod.Post, method)
        assertEquals("ABCDEF12", response.code)
        assertEquals("inv-1", response.id)
    }

    @Test
    fun testGetInviteCodes() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = """[{"id":"inv-1","code":"CODE1","createdAt":1700000000,"useCount":2}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val list = ApiClient.getInviteCodes("trip-123")

        assertEquals(1, list.size)
        assertEquals("CODE1", list[0].code)
        assertEquals(2, list[0].useCount)
    }

    @Test
    fun testDeleteInviteCode() = runTest {
        var method: HttpMethod? = null
        var deletedId: String? = null

        val mockClient = createMockHttpClient { request ->
            method = request.method
            deletedId = request.url.encodedPath.substringAfterLast("/")
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        ApiClient.setMockHttpClient(mockClient)
        ApiClient.deleteInviteCode("trip-123", "inv-999")

        assertEquals(HttpMethod.Delete, method)
        assertEquals("inv-999", deletedId)
    }

    @Test
    fun testJoinTrip() = runTest {
        var method: HttpMethod? = null

        val mockClient = createMockHttpClient { request ->
            method = request.method
            respond(
                content = """{"tripId":"trip-123","message":"Successfully joined trip"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val result = ApiClient.joinTrip("INVITE88")

        assertEquals(HttpMethod.Post, method)
        assertEquals("trip-123", result.tripId)
        assertEquals("Successfully joined trip", result.message)
    }

    @Test
    fun testUpdateMemberRole() = runTest {
        var method: HttpMethod? = null

        val mockClient = createMockHttpClient { request ->
            method = request.method
            respond(
                content = """{"message":"Role updated"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        ApiClient.updateMemberRole("trip-123", "user-2", "editor")

        assertEquals(HttpMethod.Put, method)
    }

    @Test
    fun testRemoveMember() = runTest {
        var method: HttpMethod? = null
        var memberId: String? = null

        val mockClient = createMockHttpClient { request ->
            method = request.method
            memberId = request.url.encodedPath.substringAfterLast("/")
            respond(content = "", status = HttpStatusCode.NoContent)
        }

        ApiClient.setMockHttpClient(mockClient)
        ApiClient.removeMember("trip-123", "user-2")

        assertEquals(HttpMethod.Delete, method)
        assertEquals("user-2", memberId)
    }
}
