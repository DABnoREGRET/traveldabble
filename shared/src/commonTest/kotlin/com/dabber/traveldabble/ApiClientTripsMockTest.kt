package com.dabber.traveldabble

import com.dabber.traveldabble.data.ApiClient
import com.dabber.traveldabble.data.CreateTripRequest
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ApiClientTripsMockTest {

    @AfterTest
    fun tearDown() {
        ApiClient.setMockHttpClient(null)
        ApiClient.setToken(null)
    }

    private val sampleTripJson = """
    {
      "id": "trip-uuid-1",
      "title": "Vietnam Highlights",
      "destination": "Hanoi & Ha Long",
      "country": "Vietnam",
      "startDate": "2026-10-01",
      "endDate": "2026-10-07",
      "daysUntil": 45,
      "cover": [-7685642, -1292135],
      "travelers": 2,
      "days": [
        {
          "dayNumber": 1,
          "dateLabel": "Arrival in Hanoi",
          "activities": [
            {
              "id": "act-1",
              "place": {
                "id": "place-1",
                "name": "St. Joseph's Cathedral",
                "category": "SIGHT",
                "lat": 21.0288,
                "lng": 105.8495,
                "rating": 4.7,
                "description": "Neo-Gothic cathedral",
                "openHours": "8:00 - 17:00"
              },
              "startTime": "09:00",
              "endTime": "11:00",
              "note": "Visit Old Quarter after"
            }
          ]
        }
      ],
      "budget": {
        "total": 1200.0,
        "categories": [
          {"first": "Accommodation", "second": 500.0},
          {"first": "Food", "second": 300.0}
        ],
        "expenses": [
          {
            "id": "exp-1",
            "title": "Egg Coffee",
            "category": "Food",
            "amount": 2.50,
            "date": "2026-10-01"
          }
        ]
      }
    }
    """.trimIndent()

    @Test
    fun testGetTripsReturnsParsedList() = runTest {
        val mockClient = createMockHttpClient {
            respond(
                content = "[$sampleTripJson]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val trips = ApiClient.getTrips()

        assertEquals(1, trips.size)
        val trip = trips.first()
        assertEquals("trip-uuid-1", trip.id)
        assertEquals("Vietnam Highlights", trip.title)
        assertEquals("Vietnam", trip.country)
        assertEquals(2, trip.travelers)
        assertEquals(1, trip.days.size)
        assertEquals("St. Joseph's Cathedral", trip.days.first().activities.first().place.name)
        assertEquals(1200.0, trip.budget.total)
        assertEquals(1, trip.budget.expenses.size)
        assertEquals(2.50, trip.budget.spent)
    }

    @Test
    fun testGetTripById() = runTest {
        var requestedPath: String? = null

        val mockClient = createMockHttpClient { request ->
            requestedPath = request.url.encodedPath
            respond(
                content = sampleTripJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val trip = ApiClient.getTrip("trip-uuid-1")

        assertEquals("trip-uuid-1", trip.id)
        assertEquals("Vietnam Highlights", trip.title)
        assertTrue(requestedPath!!.endsWith("/api/trips/trip-uuid-1"))
    }

    @Test
    fun testCreateTrip() = runTest {
        var method: HttpMethod? = null

        val mockClient = createMockHttpClient { request ->
            method = request.method
            respond(
                content = sampleTripJson,
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        val created = ApiClient.createTrip(
            CreateTripRequest(
                title = "Vietnam Highlights",
                destination = "Hanoi & Ha Long",
                country = "Vietnam",
                startDate = "2026-10-01",
                endDate = "2026-10-07",
                travelers = 2
            )
        )

        assertEquals("Vietnam Highlights", created.title)
        assertEquals(HttpMethod.Post, method)
    }

    @Test
    fun testDeleteTrip() = runTest {
        var method: HttpMethod? = null
        var deletedId: String? = null

        val mockClient = createMockHttpClient { request ->
            method = request.method
            deletedId = request.url.encodedPath.substringAfterLast("/")
            respond(
                content = "",
                status = HttpStatusCode.NoContent
            )
        }

        ApiClient.setMockHttpClient(mockClient)
        ApiClient.deleteTrip("trip-delete-123")

        assertEquals(HttpMethod.Delete, method)
        assertEquals("trip-delete-123", deletedId)
    }

    @Test
    fun testTestConnectionReturnsTrueOn200() = runTest {
        val mockClient = createMockHttpClient {
            respond(content = "ok", status = HttpStatusCode.OK)
        }
        ApiClient.setMockHttpClient(mockClient)

        val healthy = ApiClient.testConnection()
        assertTrue(healthy)
    }

    @Test
    fun testTestConnectionReturnsFalseOn500() = runTest {
        val mockClient = createMockHttpClient {
            respond(content = "error", status = HttpStatusCode.InternalServerError)
        }
        ApiClient.setMockHttpClient(mockClient)

        val healthy = ApiClient.testConnection()
        assertFalse(healthy)
    }
}
