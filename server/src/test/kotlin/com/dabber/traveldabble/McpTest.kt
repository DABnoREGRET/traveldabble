package com.dabber.traveldabble

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.*

class McpTest {

    @Test
    fun testMcpDiscovery() = testApplication {
        application {
            module()
        }
        val response = client.get("/mcp")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("TravelDabble", body["name"]!!.jsonPrimitive.content)
        assertEquals("1.0.0", body["version"]!!.jsonPrimitive.content)
        assertEquals("2024-11-05", body["protocolVersion"]!!.jsonPrimitive.content)
        val capabilities = body["capabilities"]!!.jsonObject
        assertTrue(capabilities["tools"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun testMcpInitialize() = testApplication {
        application {
            module()
        }
        val response = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody("""{"jsonrpc":"2.0","id":1,"method":"initialize"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("2024-11-05", body["result"]!!.jsonObject["protocolVersion"]!!.jsonPrimitive.content)
        assertEquals(1, body["id"]!!.jsonPrimitive.int)
    }

    @Test
    fun testMcpToolsList() = testApplication {
        application {
            module()
        }
        val response = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody("""{"jsonrpc":"2.0","id":2,"method":"tools/list"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val tools = body["result"]!!.jsonObject["tools"]!!.jsonArray
        assertTrue(tools.isNotEmpty())
        val toolNames = tools.map { it.jsonObject["name"]!!.jsonPrimitive.content }
        assertTrue("search_destinations" in toolNames)
        assertTrue("weather_forecast" in toolNames)
        assertTrue("seasonal_recommendations" in toolNames)
        assertTrue("travel_advisory" in toolNames)
        assertTrue("local_events" in toolNames)
        assertTrue("itinerary_templates" in toolNames)
        assertTrue("compare_destinations" in toolNames)
    }

    @Test
    fun testMcpToolsCallSearchDestinations() = testApplication {
        application {
            module()
        }
        val response = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody("""
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"search_destinations","arguments":{"query":"Vietnam"}}}
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["result"])
    }

    @Test
    fun testMcpToolsCallSeasonalRecommendations() = testApplication {
        application {
            module()
        }
        val response = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody("""
                {"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"seasonal_recommendations","arguments":{"destination":"Hanoi"}}}
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["result"])
    }

    @Test
    fun testMcpMethodNotFound() = testApplication {
        application {
            module()
        }
        val response = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody("""{"jsonrpc":"2.0","id":6,"method":"unknown_method"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(body["error"])
        assertEquals(-32601, body["error"]!!.jsonObject["code"]!!.jsonPrimitive.int)
    }

    @Test
    fun testMcpToolNotFound() = testApplication {
        application {
            module()
        }
        val response = client.post("/mcp") {
            contentType(ContentType.Application.Json)
            setBody("""
                {"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"nonexistent_tool","arguments":{}}}
            """.trimIndent())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val result = body["result"]!!.jsonObject
        assertNotNull(result["error"])
        assertTrue(result["error"]!!.jsonPrimitive.content.contains("nonexistent_tool"))
    }
}
