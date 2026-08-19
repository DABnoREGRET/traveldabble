package com.dabber.traveldabble.routes

import com.dabber.traveldabble.db.*
import com.dabber.traveldabble.db.Mappers.toActivityRow
import com.dabber.traveldabble.db.Mappers.toDayPlan
import com.dabber.traveldabble.db.Mappers.toExpense
import com.dabber.traveldabble.model.*
import com.dabber.traveldabble.util.Validation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.TripContentRoutes() {
    authenticate("auth-jwt") {
        route("/api/trips/{tripId}") {
            fun verifyAccess(tripId: UUID, userId: UUID): Boolean = transaction {
                val owned = Trips.selectAll().where { (Trips.id eq tripId) and (Trips.userId eq userId) }.singleOrNull() != null
                val member = TripMembers.selectAll().where { (TripMembers.tripId eq tripId) and (TripMembers.userId eq userId) }.singleOrNull() != null
                owned || member
            }

            post("/days") {
                val userId = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                val tripId = call.parameters["tripId"]?.toUuid() ?: return@post call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                val req = runCatching { call.receive<AddDayPlanRequest>() }.getOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                val errors = mutableListOf<String>().apply {
                    Validation.range(req.dayNumber, "dayNumber", 1, 366)?.let { add(it) }
                    Validation.nonBlank(req.dateLabel, "dateLabel", 50)?.let { add(it) }
                }
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, ApiError(errors.joinToString("; ")))
                if (!verifyAccess(tripId, userId)) return@post call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))

                try {
                    val day = transaction {
                        val id = UUID.randomUUID()
                        DayPlans.insert {
                            it[DayPlans.id] = id
                            it[DayPlans.tripId] = tripId
                            it[DayPlans.dayNumber] = req.dayNumber
                            it[DayPlans.dateLabel] = req.dateLabel.trim()
                        }
                        DayPlans.selectAll().where { DayPlans.id eq id }.single().toDayPlan()
                    }
                    call.respond(HttpStatusCode.Created, day)
                } catch (e: Exception) {
                    call.application.log.error("Failed to add day plan", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to add day plan"))
                }
            }

            delete("/days/{dayId}") {
                val userId = call.userId() ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                val tripId = call.parameters["tripId"]?.toUuid() ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                val dayId = call.parameters["dayId"]?.toUuid() ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiError("Invalid day id"))
                if (!verifyAccess(tripId, userId)) return@delete call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))
                try {
                    val deleted = transaction {
                        val count = DayPlans.deleteWhere { DayPlans.id eq dayId }
                        count > 0
                    }
                    if (deleted) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound, ApiError("Day plan not found"))
                } catch (e: Exception) {
                    call.application.log.error("Failed to delete day plan", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to delete day plan"))
                }
            }

            post("/days/{dayId}/activities") {
                val userId = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                val tripId = call.parameters["tripId"]?.toUuid() ?: return@post call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                val dayId = call.parameters["dayId"]?.toUuid() ?: return@post call.respond(HttpStatusCode.BadRequest, ApiError("Invalid day id"))
                val req = runCatching { call.receive<AddActivityRequest>() }.getOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                val errors = validateActivity(req)
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, ApiError(errors.joinToString("; ")))
                if (!verifyAccess(tripId, userId)) return@post call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))
                try {
                    val activity = transaction {
                        val dayExists = DayPlans.selectAll().where {
                            (DayPlans.id eq dayId) and (DayPlans.tripId eq tripId)
                        }.singleOrNull() != null
                        if (!dayExists) return@transaction null
                        val placeId = UUID.randomUUID()
                        Places.insert {
                            it[Places.id] = placeId
                            it[Places.name] = req.place.name.trim()
                            it[Places.category] = req.place.category.trim()
                            it[Places.lat] = req.place.lat
                            it[Places.lng] = req.place.lng
                            it[Places.rating] = req.place.rating.toDouble()
                            it[Places.description] = req.place.description.trim()
                            it[Places.openHours] = req.place.openHours.trim()
                        }
                        val activityId = UUID.randomUUID()
                        Activities.insert {
                            it[Activities.id] = activityId
                            it[Activities.dayPlanId] = dayId
                            it[Activities.placeId] = placeId
                            it[Activities.startTime] = req.startTime.trim()
                            it[Activities.endTime] = req.endTime.trim()
                            it[Activities.note] = req.note?.trim()
                        }
                        Activities.selectAll().where { Activities.id eq activityId }.single().toActivityRow(placeId)
                    }
                    if (activity == null) call.respond(HttpStatusCode.NotFound, ApiError("Day plan not found"))
                    else call.respond(HttpStatusCode.Created, activity)
                } catch (e: Exception) {
                    call.application.log.error("Failed to add activity", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to add activity"))
                }
            }

            put("/activities/{activityId}") {
                val userId = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                val tripId = call.parameters["tripId"]?.toUuid() ?: return@put call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                val activityId = call.parameters["activityId"]?.toUuid() ?: return@put call.respond(HttpStatusCode.BadRequest, ApiError("Invalid activity id"))
                val req = runCatching { call.receive<UpdateActivityRequest>() }.getOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                val errors = mutableListOf<String>().apply {
                    req.startTime?.let { Validation.time(it, "startTime")?.let { m -> add(m) } }
                    req.endTime?.let { Validation.time(it, "endTime")?.let { m -> add(m) } }
                }
                if (errors.isNotEmpty()) return@put call.respond(HttpStatusCode.BadRequest, ApiError(errors.joinToString("; ")))
                if (!verifyAccess(tripId, userId)) return@put call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))
                try {
                    val updated = transaction {
                        val exists = Activities.selectAll().where { Activities.id eq activityId }.singleOrNull() != null
                        if (!exists) return@transaction false
                        Activities.update({ Activities.id eq activityId }) {
                            req.startTime?.let { t -> it[Activities.startTime] = t.trim() }
                            req.endTime?.let { t -> it[Activities.endTime] = t.trim() }
                            req.note?.let { n -> it[Activities.note] = n.trim() }
                        }
                        true
                    }
                    if (updated) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound, ApiError("Activity not found"))
                } catch (e: Exception) {
                    call.application.log.error("Failed to update activity", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to update activity"))
                }
            }

            delete("/activities/{activityId}") {
                val userId = call.userId() ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                val tripId = call.parameters["tripId"]?.toUuid() ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                val activityId = call.parameters["activityId"]?.toUuid() ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiError("Invalid activity id"))
                if (!verifyAccess(tripId, userId)) return@delete call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))
                try {
                    val deleted = transaction {
                        val activityRow = (Activities innerJoin DayPlans)
                            .selectAll()
                            .where { (Activities.id eq activityId) and (DayPlans.tripId eq tripId) }
                            .singleOrNull()
                        val placeId = activityRow?.get(Activities.placeId)?.value
                        val count = if (activityRow != null) {
                            Activities.deleteWhere { Activities.id eq activityId }
                        } else {
                            0
                        }
                        placeId?.let { pid -> Places.deleteWhere { Places.id eq pid } }
                        count > 0
                    }
                    if (deleted) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound, ApiError("Activity not found"))
                } catch (e: Exception) {
                    call.application.log.error("Failed to delete activity", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to delete activity"))
                }
            }

            put("/budget") {
                val userId = call.userId() ?: return@put call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                val tripId = call.parameters["tripId"]?.toUuid() ?: return@put call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                val req = runCatching { call.receive<UpdateBudgetRequest>() }.getOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                val errors = mutableListOf<String>().apply {
                    if (req.total < 0) add("total must not be negative")
                    req.categories.forEach { (name, amount) ->
                        if (name.isBlank()) add("category name must not be blank")
                        if (amount < 0) add("category amount must not be negative")
                    }
                }
                if (errors.isNotEmpty()) return@put call.respond(HttpStatusCode.BadRequest, ApiError(errors.joinToString("; ")))
                if (!verifyAccess(tripId, userId)) return@put call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))
                try {
                    transaction {
                        val existing = Budgets.selectAll().where { Budgets.tripId eq tripId }.singleOrNull()
                        if (existing == null) {
                            Budgets.insert {
                                it[Budgets.id] = UUID.randomUUID()
                                it[Budgets.tripId] = tripId
                                it[Budgets.total] = req.total
                                it[Budgets.categories] = Mappers.json.encodeToString(req.categories)
                            }
                        } else {
                            Budgets.update({ Budgets.tripId eq tripId }) {
                                it[Budgets.total] = req.total
                                it[Budgets.categories] = Mappers.json.encodeToString(req.categories)
                            }
                        }
                    }
                    call.respond(HttpStatusCode.NoContent)
                } catch (e: Exception) {
                    call.application.log.error("Failed to update budget", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to update budget"))
                }
            }

            post("/budget/expenses") {
                val userId = call.userId() ?: return@post call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                val tripId = call.parameters["tripId"]?.toUuid() ?: return@post call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                val req = runCatching { call.receive<AddExpenseRequest>() }.getOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, ApiError("Invalid request body"))
                val errors = mutableListOf<String>().apply {
                    Validation.nonBlank(req.title, "title", 200)?.let { add(it) }
                    Validation.nonBlank(req.category, "category", 100)?.let { add(it) }
                    if (req.amount < 0) add("amount must not be negative")
                    Validation.date(req.date, "date")?.let { add(it) }
                }
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, ApiError(errors.joinToString("; ")))
                if (!verifyAccess(tripId, userId)) return@post call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))
                try {
                    val expense = transaction {
                        val budgetRow = Budgets.selectAll().where { Budgets.tripId eq tripId }.singleOrNull()
                            ?: run {
                                val id = UUID.randomUUID()
                                Budgets.insert {
                                    it[Budgets.id] = id
                                    it[Budgets.tripId] = tripId
                                    it[Budgets.total] = 0.0
                                    it[Budgets.categories] = "[]"
                                }
                                Budgets.selectAll().where { Budgets.id eq id }.single()
                            }
                        val budgetId = budgetRow[Budgets.id].value
                        val expenseId = UUID.randomUUID()
                        Expenses.insert {
                            it[Expenses.id] = expenseId
                            it[Expenses.budgetId] = budgetId
                            it[Expenses.title] = req.title.trim()
                            it[Expenses.category] = req.category.trim()
                            it[Expenses.amount] = req.amount
                            it[Expenses.date] = req.date.trim()
                        }
                        Expenses.selectAll().where { Expenses.id eq expenseId }.single().toExpense()
                    }
                    call.respond(HttpStatusCode.Created, expense)
                } catch (e: Exception) {
                    call.application.log.error("Failed to add expense", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to add expense"))
                }
            }

            delete("/budget/expenses/{expenseId}") {
                val userId = call.userId() ?: return@delete call.respond(HttpStatusCode.Unauthorized, ApiError("Not authenticated"))
                val tripId = call.parameters["tripId"]?.toUuid() ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiError("Invalid trip id"))
                val expenseId = call.parameters["expenseId"]?.toUuid() ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiError("Invalid expense id"))
                if (!verifyAccess(tripId, userId)) return@delete call.respond(HttpStatusCode.NotFound, ApiError("Trip not found"))
                try {
                    val deleted = transaction {
                        val count = Expenses.deleteWhere { Expenses.id eq expenseId }
                        count > 0
                    }
                    if (deleted) call.respond(HttpStatusCode.NoContent) else call.respond(HttpStatusCode.NotFound, ApiError("Expense not found"))
                } catch (e: Exception) {
                    call.application.log.error("Failed to delete expense", e)
                    call.respond(HttpStatusCode.InternalServerError, ApiError("Failed to delete expense"))
                }
            }
        }
    }
}

private fun String.toUuid(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

private fun validateActivity(req: AddActivityRequest): List<String> {
    val errors = mutableListOf<String>()
    Validation.nonBlank(req.place.name, "place.name", 200)?.let { errors += it }
    if (req.place.category.isBlank() || PlaceCategory.entries.none { it.name == req.place.category }) {
        errors += "place.category must be one of ${PlaceCategory.entries.joinToString { it.name }}"
    }
    if (req.place.lat < -90.0 || req.place.lat > 90.0) errors += "place.lat out of range"
    if (req.place.lng < -180.0 || req.place.lng > 180.0) errors += "place.lng out of range"
    if (req.place.rating < 0f || req.place.rating > 5f) errors += "place.rating must be between 0 and 5"
    Validation.nonBlank(req.place.description, "place.description", 2000)?.let { errors += it }
    Validation.nonBlank(req.place.openHours, "place.openHours", 100)?.let { errors += it }
    Validation.time(req.startTime, "startTime")?.let { errors += it }
    Validation.time(req.endTime, "endTime")?.let { errors += it }
    return errors
}
