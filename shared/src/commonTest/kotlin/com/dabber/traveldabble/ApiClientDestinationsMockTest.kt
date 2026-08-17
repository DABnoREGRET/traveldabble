package com.dabber.traveldabble

import com.dabber.traveldabble.data.ApiClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ApiClientDestinationsMockTest {

    @AfterTest
    fun tearDown() {
        ApiClient.setMockHttpClient(null)
        ApiClient.setToken(null)
    }

    private val destinationsJson = """
    [
      {
        "id": "dest-1",
        "name": "Hanoi",
        "country": "Vietnam",
        "tagline": "Old Quarter streets and egg coffee",
        "rating": 4.8,
        "tags": ["Culture", "Food", "Heritage"],
        "cover": [-7685642, -1292135]
      },
      {
        "id": "dest-2",
        "name": "Kyoto",
        "country": "Japan",
        "tagline": "Temples and bamboo groves",
        "rating": 4.9,
        "tags": ["Culture", "Nature"],
        "cover": [-5685642, -3292135]
      },
      {
        "id": "dest-3",
        "name": "Da Nang",
        "country": "Vietnam",
        "tagline": "Golden Bridge and coastal sunsets",
        "rating": 4.7,
        "tags": ["Beach", "Adventure"],
        "cover": [-4685642, -2292135]
      }
    ]
    """.trimIndent()

    @Test
    fun testGetDestinationsReturnsList() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = destinationsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val list = ApiClient.getDestinations()

        assertEquals(3, list.size)
        assertEquals("Hanoi", list[0].name)
        assertEquals("Kyoto", list[1].name)
        assertEquals(4.8f, list[0].rating)
    }

    @Test
    fun testGetDestinationById() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = """
                {
                  "id": "dest-1",
                  "name": "Hanoi",
                  "country": "Vietnam",
                  "tagline": "Old Quarter streets and egg coffee",
                  "rating": 4.8,
                  "tags": ["Culture", "Food", "Heritage"],
                  "cover": [-7685642, -1292135]
                }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val dest = ApiClient.getDestination("dest-1")

        assertEquals("dest-1", dest.id)
        assertEquals("Hanoi", dest.name)
        assertEquals("Vietnam", dest.country)
    }

    @Test
    fun testSearchDestinationsFiltersByQuery() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = destinationsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)

        val foodResults = ApiClient.searchDestinations(query = "coffee")
        assertEquals(1, foodResults.size)
        assertEquals("Hanoi", foodResults[0].name)

        val beachResults = ApiClient.searchDestinations(query = "sunset")
        assertEquals(1, beachResults.size)
        assertEquals("Da Nang", beachResults[0].name)
    }

    @Test
    fun testSearchDestinationsFiltersByCountry() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = destinationsJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)

        val vietnamDestinations = ApiClient.searchDestinations(query = "", country = "Vietnam")
        assertEquals(2, vietnamDestinations.size)
        assertTrue(vietnamDestinations.all { it.country == "Vietnam" })

        val japanDestinations = ApiClient.searchDestinations(query = "", country = "Japan")
        assertEquals(1, japanDestinations.size)
        assertEquals("Kyoto", japanDestinations[0].name)
    }
}
