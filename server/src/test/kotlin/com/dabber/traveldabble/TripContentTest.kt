package com.dabber.traveldabble

import com.dabber.traveldabble.db.DayPlans
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.*

/**
 * Tests for TripContentRoutes:
 *   POST   /api/trips/{tripId}/days
 *   DELETE /api/trips/{tripId}/days/{dayId}
 *   POST   /api/trips/{tripId}/days/{dayId}/activities
 *   PUT    /api/trips/{tripId}/activities/{activityId}
 *   DELETE /api/trips/{tripId}/activities/{activityId}
 *   PUT    /api/trips/{tripId}/budget
 *   POST   /api/trips/{tripId}/budget/expenses
 *   DELETE /api/trips/{tripId}/budget/expenses/{expenseId}
 */
class TripContentTest {

    private suspend fun registerAndLogin(client: io.ktor.client.HttpClient, suffix: String): String {
        val res = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"user_$suffix","email":"user_$suffix@example.com","password":"pass1234","displayName":"User $suffix"}""")
        }
        return Json.parseToJsonElement(res.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content
    }

    private suspend fun createTrip(client: io.ktor.client.HttpClient, token: String): String {
        val res = client.post("/api/trips") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"title":"Test Trip","destination":"Hoi An","country":"Vietnam","startDate":"2026-10-01","endDate":"2026-10-05","travelers":1}""")
        }
        return Json.parseToJsonElement(res.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content
    }

    private suspend fun createDay(client: io.ktor.client.HttpClient, token: String, tripId: String): String {
        client.post("/api/trips/$tripId/days") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"dayNumber":1,"dateLabel":"Day One"}""")
        }
        return transaction {
            DayPlans.selectAll()
                .where { DayPlans.tripId eq UUID.fromString(tripId) }
                .single()[DayPlans.id].value.toString()
        }
    }

    // ---- Day Plan Tests ----

    @Test
    fun testAddDayPlan() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "day_add1")
        val tripId = createTrip(client, token)

        val res = client.post("/api/trips/$tripId/days") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"dayNumber":1,"dateLabel":"Day 1 - Arrival"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status, "addDay: ${res.bodyAsText()}")
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        assertEquals(1, body["dayNumber"]!!.jsonPrimitive.int)
        assertEquals("Day 1 - Arrival", body["dateLabel"]!!.jsonPrimitive.content)
    }

    @Test
    fun testAddDayPlanRequiresAuth() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "day_auth1")
        val tripId = createTrip(client, token)

        val res = client.post("/api/trips/$tripId/days") {
            contentType(ContentType.Application.Json)
            setBody("""{"dayNumber":1,"dateLabel":"Unauthorized"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun testAddDayPlanInvalidDayNumber() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "day_val1")
        val tripId = createTrip(client, token)

        val res = client.post("/api/trips/$tripId/days") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"dayNumber":0,"dateLabel":"Bad"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun testDeleteDayPlan() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "day_del1")
        val tripId = createTrip(client, token)
        val dayId = createDay(client, token, tripId)

        val res = client.delete("/api/trips/$tripId/days/$dayId") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NoContent, res.status)
    }

    @Test
    fun testDeleteNonExistentDayPlan() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "day_del2")
        val tripId = createTrip(client, token)

        val res = client.delete("/api/trips/$tripId/days/00000000-0000-0000-0000-000000000001") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    // ---- Activity Tests ----

    @Test
    fun testAddActivity() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "act_add1")
        val tripId = createTrip(client, token)
        val dayId = createDay(client, token, tripId)

        val res = client.post("/api/trips/$tripId/days/$dayId/activities") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"place":{"name":"Hoi An Ancient Town","category":"SIGHT","lat":15.8801,"lng":108.338,"rating":4.8,"description":"UNESCO site","openHours":"08:00 - 22:00"},"startTime":"09:00","endTime":"12:00","note":"Book tickets"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status, "addActivity: ${res.bodyAsText()}")
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        assertTrue(body["id"]!!.jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun testAddActivityInvalidCategory() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "act_val1")
        val tripId = createTrip(client, token)
        val dayId = createDay(client, token, tripId)

        val res = client.post("/api/trips/$tripId/days/$dayId/activities") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"place":{"name":"Place","category":"NOT_REAL","lat":0.0,"lng":0.0,"rating":3.0,"description":"desc","openHours":"09:00 - 17:00"},"startTime":"09:00","endTime":"10:00","note":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun testAddActivityLatOutOfRange() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "act_val2")
        val tripId = createTrip(client, token)
        val dayId = createDay(client, token, tripId)

        val res = client.post("/api/trips/$tripId/days/$dayId/activities") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"place":{"name":"Place","category":"SIGHT","lat":999.0,"lng":0.0,"rating":3.0,"description":"desc","openHours":"09:00 - 17:00"},"startTime":"09:00","endTime":"10:00","note":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun testUpdateActivity() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "act_upd1")
        val tripId = createTrip(client, token)
        val dayId = createDay(client, token, tripId)

        val addRes = client.post("/api/trips/$tripId/days/$dayId/activities") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"place":{"name":"Marble Mountains","category":"ACTIVITY","lat":16.0,"lng":108.26,"rating":4.3,"description":"Marble formations","openHours":"07:30 - 17:30"},"startTime":"10:00","endTime":"13:00","note":""}""")
        }
        val activityId = Json.parseToJsonElement(addRes.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val res = client.put("/api/trips/$tripId/activities/$activityId") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"startTime":"11:00","endTime":"14:00","note":"Wear comfortable shoes"}""")
        }
        assertEquals(HttpStatusCode.NoContent, res.status, "updateActivity: ${res.bodyAsText()}")
    }

    @Test
    fun testDeleteActivity() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "act_del1")
        val tripId = createTrip(client, token)
        val dayId = createDay(client, token, tripId)

        val addRes = client.post("/api/trips/$tripId/days/$dayId/activities") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"place":{"name":"Cooking Class","category":"FOOD","lat":15.88,"lng":108.34,"rating":5.0,"description":"Vietnamese cooking","openHours":"08:00 - 12:00"},"startTime":"08:00","endTime":"12:00","note":""}""")
        }
        val activityId = Json.parseToJsonElement(addRes.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val res = client.delete("/api/trips/$tripId/activities/$activityId") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NoContent, res.status)
    }

    // ---- Budget Tests ----

    @Test
    fun testSetBudget() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "bud_set1")
        val tripId = createTrip(client, token)

        val res = client.put("/api/trips/$tripId/budget") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"total":1500.0,"categories":[{"first":"Accommodation","second":600.0},{"first":"Food","second":300.0}]}""")
        }
        assertEquals(HttpStatusCode.NoContent, res.status, "setBudget: ${res.bodyAsText()}")
    }

    @Test
    fun testUpdateBudgetIsIdempotent() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "bud_upd1")
        val tripId = createTrip(client, token)

        client.put("/api/trips/$tripId/budget") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"total":1000.0,"categories":[{"first":"Food","second":400.0}]}""")
        }
        // Update again — should upsert without error
        val res = client.put("/api/trips/$tripId/budget") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"total":2000.0,"categories":[{"first":"Food","second":500.0},{"first":"Accommodation","second":800.0}]}""")
        }
        assertEquals(HttpStatusCode.NoContent, res.status, "updateBudget: ${res.bodyAsText()}")
    }

    @Test
    fun testBudgetNegativeTotalRejected() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "bud_val1")
        val tripId = createTrip(client, token)

        val res = client.put("/api/trips/$tripId/budget") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"total":-100.0,"categories":[]}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun testBudgetRequiresAuth() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "bud_auth1")
        val tripId = createTrip(client, token)

        val res = client.put("/api/trips/$tripId/budget") {
            contentType(ContentType.Application.Json)
            setBody("""{"total":1000.0,"categories":[]}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    // ---- Expense Tests ----

    @Test
    fun testAddExpense() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "exp_add1")
        val tripId = createTrip(client, token)

        val res = client.post("/api/trips/$tripId/budget/expenses") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"title":"Banh Mi breakfast","category":"Food","amount":2.50,"date":"2026-10-02"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status, "addExpense: ${res.bodyAsText()}")
        val body = Json.parseToJsonElement(res.bodyAsText()).jsonObject
        assertTrue(body["id"]!!.jsonPrimitive.content.isNotBlank())
        assertEquals("Banh Mi breakfast", body["title"]!!.jsonPrimitive.content)
    }

    @Test
    fun testExpenseAutoCreatesBudget() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "exp_add2")
        val tripId = createTrip(client, token)
        // No budget set — adding expense should auto-create one at 0 total
        val res = client.post("/api/trips/$tripId/budget/expenses") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"title":"Cab","category":"Transport","amount":8.00,"date":"2026-10-01"}""")
        }
        assertEquals(HttpStatusCode.Created, res.status, "expense auto-create budget: ${res.bodyAsText()}")
    }

    @Test
    fun testDeleteExpense() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "exp_del1")
        val tripId = createTrip(client, token)

        val addRes = client.post("/api/trips/$tripId/budget/expenses") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"title":"Hotel night","category":"Accommodation","amount":45.0,"date":"2026-10-03"}""")
        }
        val expenseId = Json.parseToJsonElement(addRes.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val res = client.delete("/api/trips/$tripId/budget/expenses/$expenseId") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NoContent, res.status)
    }

    @Test
    fun testDeleteNonExistentExpense() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "exp_del2")
        val tripId = createTrip(client, token)

        val res = client.delete("/api/trips/$tripId/budget/expenses/00000000-0000-0000-0000-000000000002") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    @Test
    fun testExpenseNegativeAmountRejected() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "exp_val1")
        val tripId = createTrip(client, token)

        val res = client.post("/api/trips/$tripId/budget/expenses") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"title":"Refund","category":"Misc","amount":-50.0,"date":"2026-10-03"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun testExpenseBlankDateRejected() = testApplication {
        application { module() }
        val token = registerAndLogin(client, "exp_val2")
        val tripId = createTrip(client, token)

        val res = client.post("/api/trips/$tripId/budget/expenses") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody("""{"title":"Bad date","category":"Food","amount":5.0,"date":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }
}
