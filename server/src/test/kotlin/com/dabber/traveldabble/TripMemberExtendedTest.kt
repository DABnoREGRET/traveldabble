package com.dabber.traveldabble

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Extended tests for TripMemberRoutes (covering untested endpoints):
 *   GET    /api/trips/{tripId}/invite         — list invite codes
 *   DELETE /api/trips/{tripId}/invite/{id}    — revoke an invite code
 *   GET    /api/trips/{tripId}/members        — list members
 *   PUT    /api/trips/{tripId}/members/{uid}  — update member role
 *   DELETE /api/trips/{tripId}/members/{uid}  — remove member
 */
class TripMemberExtendedTest {

    private suspend fun register(client: io.ktor.client.HttpClient, suffix: String): String {
        val res = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"mb_$suffix","email":"mb_$suffix@example.com","password":"pass1234","displayName":"Mb $suffix"}""")
        }
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        return body["token"]!!.jsonPrimitive.content
    }

    private suspend fun userId(client: io.ktor.client.HttpClient, token: String): String {
        val res = client.get("/api/auth/me") { bearerAuth(token) }
        return Json.parseToJsonElement(res.bodyAsText()).jsonObject["userId"]!!.jsonPrimitive.content
    }

    private suspend fun createTrip(client: io.ktor.client.HttpClient, token: String): String {
        val res = client.post("/api/trips") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"title":"Group Trip","destination":"Da Nang","country":"Vietnam","startDate":"2026-11-01","endDate":"2026-11-07","travelers":4}""")
        }
        return Json.parseToJsonElement(res.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content
    }

    private suspend fun generateInvite(client: io.ktor.client.HttpClient, token: String, tripId: String): Pair<String, String> {
        val res = client.post("/api/trips/$tripId/invite") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"maxUses":10,"expiresInHours":24}""")
        }
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        return body["id"]!!.jsonPrimitive.content to body["code"]!!.jsonPrimitive.content
    }

    @Test
    fun testListInviteCodes() = testApplication {
        application { module() }
        val token = register(client, "lst_inv1")
        val tripId = createTrip(client, token)
        generateInvite(client, token, tripId) // create one invite

        val res = client.get("/api/trips/$tripId/invite") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, res.status, "listInvites: ${res.bodyAsText()}")
        val arr = Json.parseToJsonElement(res.bodyAsText()).jsonArray
        assertTrue(arr.isNotEmpty(), "Expected at least 1 invite code in the list")
        assertNotNull(arr.first().jsonObject["code"])
    }

    @Test
    fun testListInviteCodesRequiresAuth() = testApplication {
        application { module() }
        val token = register(client, "lst_inv2")
        val tripId = createTrip(client, token)

        val res = client.get("/api/trips/$tripId/invite")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun testRevokeInviteCode() = testApplication {
        application { module() }
        val token = register(client, "rev_inv1")
        val tripId = createTrip(client, token)
        val (inviteId, _) = generateInvite(client, token, tripId)

        val res = client.delete("/api/trips/$tripId/invite/$inviteId") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NoContent, res.status, "revokeInvite: ${res.bodyAsText()}")

        // Verify invite is gone from list
        val list = client.get("/api/trips/$tripId/invite") { bearerAuth(token) }
        val arr = Json.parseToJsonElement(list.bodyAsText()).jsonArray
        assertTrue(arr.none { it.jsonObject["id"]?.jsonPrimitive?.content == inviteId }, "Invite should be gone after deletion")
    }

    @Test
    fun testGetTripMembers() = testApplication {
        application { module() }
        val ownerToken = register(client, "mem_lst1")
        val tripId = createTrip(client, ownerToken)

        // Owner is automatically a member
        val res = client.get("/api/trips/$tripId/members") { bearerAuth(ownerToken) }
        assertEquals(HttpStatusCode.OK, res.status, "listMembers: ${res.bodyAsText()}")
        val members = Json.parseToJsonElement(res.bodyAsText()).jsonArray
        assertTrue(members.isNotEmpty(), "Owner should appear in member list")
        val owner = members.first().jsonObject
        assertEquals("owner", owner["role"]!!.jsonPrimitive.content)
    }

    @Test
    fun testGetTripMembersRequiresAuth() = testApplication {
        application { module() }
        val token = register(client, "mem_lst2")
        val tripId = createTrip(client, token)

        val res = client.get("/api/trips/$tripId/members")
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun testMemberJoinsAndAppearsInList() = testApplication {
        application { module() }
        val ownerToken = register(client, "mem_join1")
        val tripId = createTrip(client, ownerToken)
        val (_, code) = generateInvite(client, ownerToken, tripId)

        val memberToken = register(client, "mem_join2")
        client.post("/api/trips/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(memberToken)
            setBody("""{"code":"$code"}""")
        }

        val res = client.get("/api/trips/$tripId/members") { bearerAuth(ownerToken) }
        val members = Json.parseToJsonElement(res.bodyAsText()).jsonArray
        assertEquals(2, members.size, "Expected owner + 1 member, got: ${res.bodyAsText()}")
        val roles = members.map { it.jsonObject["role"]!!.jsonPrimitive.content }.toSet()
        assertTrue(roles.contains("owner"))
        assertTrue(roles.contains("member"))
    }

    @Test
    fun testUpdateMemberRole() = testApplication {
        application { module() }
        val ownerToken = register(client, "upd_role1")
        val tripId = createTrip(client, ownerToken)
        val (_, code) = generateInvite(client, ownerToken, tripId)

        val memberToken = register(client, "upd_role2")
        val memberId = userId(client, memberToken)
        client.post("/api/trips/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(memberToken)
            setBody("""{"code":"$code"}""")
        }

        // Owner updates member to editor role
        val res = client.put("/api/trips/$tripId/members/$memberId") {
            contentType(ContentType.Application.Json)
            bearerAuth(ownerToken)
            setBody("""{"role":"editor"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status, "updateRole: ${res.bodyAsText()}")
        assertTrue(res.bodyAsText().contains("Role updated"))
    }

    @Test
    fun testNonOwnerCannotUpdateMemberRole() = testApplication {
        application { module() }
        val ownerToken = register(client, "upd_role3")
        val tripId = createTrip(client, ownerToken)
        val (_, code) = generateInvite(client, ownerToken, tripId)

        val member1Token = register(client, "upd_role4")
        client.post("/api/trips/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(member1Token)
            setBody("""{"code":"$code"}""")
        }

        val member2Token = register(client, "upd_role5")
        val member2Id = userId(client, member2Token)
        // Re-generate invite so member2 can join
        val (_, code2) = generateInvite(client, ownerToken, tripId)
        client.post("/api/trips/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(member2Token)
            setBody("""{"code":"$code2"}""")
        }

        // member1 (non-owner) tries to update member2's role
        val res = client.put("/api/trips/$tripId/members/$member2Id") {
            contentType(ContentType.Application.Json)
            bearerAuth(member1Token)
            setBody("""{"role":"editor"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, res.status)
    }

    @Test
    fun testMemberCanLeaveTrip() = testApplication {
        application { module() }
        val ownerToken = register(client, "leave1")
        val tripId = createTrip(client, ownerToken)
        val (_, code) = generateInvite(client, ownerToken, tripId)

        val memberToken = register(client, "leave2")
        val memberId = userId(client, memberToken)
        client.post("/api/trips/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(memberToken)
            setBody("""{"code":"$code"}""")
        }

        // Member removes themselves
        val res = client.delete("/api/trips/$tripId/members/$memberId") { bearerAuth(memberToken) }
        assertEquals(HttpStatusCode.NoContent, res.status, "leave: ${res.bodyAsText()}")

        // Verify they're no longer in the list
        val members = Json.parseToJsonElement(
            client.get("/api/trips/$tripId/members") { bearerAuth(ownerToken) }.bodyAsText()
        ).jsonArray
        assertEquals(1, members.size, "Only owner should remain")
    }

    @Test
    fun testOwnerCanRemoveMember() = testApplication {
        application { module() }
        val ownerToken = register(client, "kick1")
        val tripId = createTrip(client, ownerToken)
        val (_, code) = generateInvite(client, ownerToken, tripId)

        val memberToken = register(client, "kick2")
        val memberId = userId(client, memberToken)
        client.post("/api/trips/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(memberToken)
            setBody("""{"code":"$code"}""")
        }

        // Owner removes member
        val res = client.delete("/api/trips/$tripId/members/$memberId") { bearerAuth(ownerToken) }
        assertEquals(HttpStatusCode.NoContent, res.status, "kick: ${res.bodyAsText()}")
    }

    @Test
    fun testAlreadyMemberCannotJoinAgain() = testApplication {
        application { module() }
        val ownerToken = register(client, "dup_join1")
        val tripId = createTrip(client, ownerToken)
        val (_, code) = generateInvite(client, ownerToken, tripId)

        val memberToken = register(client, "dup_join2")
        client.post("/api/trips/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(memberToken)
            setBody("""{"code":"$code"}""")
        }

        // Try to join again with same code
        val res = client.post("/api/trips/join") {
            contentType(ContentType.Application.Json)
            bearerAuth(memberToken)
            setBody("""{"code":"$code"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
        assertTrue(res.bodyAsText().contains("already a member"))
    }
}
