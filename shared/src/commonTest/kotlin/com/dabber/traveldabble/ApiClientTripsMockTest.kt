package com.dabber.traveldabble

import com.dabber.traveldabble.data.ApiClient
import com.dabber.traveldabble.data.CreateTripRequest
import com.dabber.traveldabble.data.Repository
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
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

    @Test
    fun testParseDateStringToLocalDateVariousFormats() {
        val date1 = Repository.parseDateStringToLocalDate("Oct 15, 2026")
        assertNotNull(date1)
        assertEquals(LocalDate(2026, 10, 15), date1)

        val date2 = Repository.parseDateStringToLocalDate("18 Oct 2026")
        assertNotNull(date2)
        assertEquals(LocalDate(2026, 10, 18), date2)

        val date3 = Repository.parseDateStringToLocalDate("2026-10-15")
        assertNotNull(date3)
        assertEquals(LocalDate(2026, 10, 15), date3)
    }

    @Test
    fun testGenerateDayPlanLabelsFourDays() {
        val labels = Repository.generateDayPlanLabels("Oct 15, 2026", "Oct 18, 2026")
        assertEquals(4, labels.size)
        assertEquals("Oct 15, 2026", labels[0])
        assertEquals("Oct 16, 2026", labels[1])
        assertEquals("Oct 17, 2026", labels[2])
        assertEquals("Oct 18, 2026", labels[3])
    }

    @Test
    fun testGenerateDayPlanLabelsSingleDay() {
        val labels = Repository.generateDayPlanLabels("Oct 15, 2026", "Oct 15, 2026")
        assertEquals(1, labels.size)
        assertEquals("Oct 15, 2026", labels[0])
    }

    @Test
    fun testGenerateDayPlanLabelsSevenDays() {
        val labels = Repository.generateDayPlanLabels("Oct 10, 2026", "Oct 16, 2026")
        assertEquals(7, labels.size)
        assertEquals("Oct 10, 2026", labels[0])
        assertEquals("Oct 16, 2026", labels[6])
    }

    @Test
    fun testCreateTripGeneratesExactDays() = runTest {
        val trip = Repository.createTrip(
            title = "Vietnam 4-Day Tour",
            destination = "Hanoi",
            country = "Vietnam",
            startDate = "Oct 15, 2026",
            endDate = "Oct 18, 2026",
            travelers = 2,
        )

        assertNotNull(trip)
        assertEquals(4, trip.days.size)
        assertEquals(1, trip.days[0].dayNumber)
        assertEquals("Oct 15, 2026", trip.days[0].dateLabel)
        assertEquals(2, trip.days[1].dayNumber)
        assertEquals("Oct 16, 2026", trip.days[1].dateLabel)
        assertEquals(3, trip.days[2].dayNumber)
        assertEquals("Oct 17, 2026", trip.days[2].dateLabel)
        assertEquals(4, trip.days[3].dayNumber)
        assertEquals("Oct 18, 2026", trip.days[3].dateLabel)
    }

    @Test
    fun testAddDayToTripAppendsNextSequentialDay() = runTest {
        val trip = Repository.createTrip(
            title = "Mini Trip",
            destination = "Da Nang",
            country = "Vietnam",
            startDate = "Nov 1, 2026",
            endDate = "Nov 2, 2026",
            travelers = 1,
        )

        assertNotNull(trip)
        assertEquals(2, trip.days.size)

        val addedDay = Repository.addDayToTrip(trip.id)
        assertNotNull(addedDay)
        assertEquals(3, addedDay.dayNumber)
        assertEquals("Nov 3, 2026", addedDay.dateLabel)

        val refreshed = Repository.getTrip(trip.id)
        assertNotNull(refreshed)
        assertEquals(3, refreshed.days.size)
    }
}
