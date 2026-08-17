package com.dabber.traveldabble.db

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ItemsTable : Table("items") {
    val id = varchar("id", 64)
    val name = varchar("name", 255)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Trips : UUIDTable("trips") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 200)
    val destination = varchar("destination", 200)
    val country = varchar("country", 100)
    val startDate = varchar("start_date", 50)
    val endDate = varchar("end_date", 50)
    val daysUntil = integer("days_until").nullable()
    val coverColors = text("cover_colors")
    val travelers = integer("travelers")
    val createdAt = long("created_at")
}

object DayPlans : UUIDTable("day_plans") {
    val tripId = reference("trip_id", Trips, onDelete = ReferenceOption.CASCADE)
    val dayNumber = integer("day_number")
    val dateLabel = varchar("date_label", 50)
}

object Places : UUIDTable("places") {
    val name = varchar("name", 200)
    val category = varchar("category", 50)
    val lat = double("lat")
    val lng = double("lng")
    val rating = double("rating")
    val description = text("description")
    val openHours = varchar("open_hours", 100)
}

object Activities : UUIDTable("activities") {
    val dayPlanId = reference("day_plan_id", DayPlans, onDelete = ReferenceOption.CASCADE)
    val placeId = reference("place_id", Places, onDelete = ReferenceOption.CASCADE)
    val startTime = varchar("start_time", 20)
    val endTime = varchar("end_time", 20)
    val note = text("note").nullable()
}

object Budgets : UUIDTable("budgets") {
    val tripId = reference("trip_id", Trips, onDelete = ReferenceOption.CASCADE)
    val total = double("total")
    val categories = text("categories")
}

object Expenses : UUIDTable("expenses") {
    val budgetId = reference("budget_id", Budgets, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 200)
    val category = varchar("category", 100)
    val amount = double("amount")
    val date = varchar("date", 50)
}

object Destinations : UUIDTable("destinations") {
    val name = varchar("name", 200)
    val country = varchar("country", 100)
    val tagline = text("tagline")
    val rating = double("rating")
    val tags = text("tags")
    val coverColors = text("cover_colors")
}

object TripMembers : UUIDTable("trip_members") {
    val tripId = reference("trip_id", Trips, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 20).default("member") // "owner", "editor", "viewer"
    val joinedAt = long("joined_at")

    init {
        uniqueIndex(tripId, userId)
    }
}

object InviteCodes : UUIDTable("invite_codes") {
    val tripId = reference("trip_id", Trips, onDelete = ReferenceOption.CASCADE)
    val code = varchar("code", 20).uniqueIndex()
    val createdBy = reference("created_by", Users, onDelete = ReferenceOption.CASCADE)
    val createdAt = long("created_at")
    val expiresAt = long("expires_at").nullable()
    val maxUses = integer("max_uses").nullable()
    val useCount = integer("use_count").default(0)
}

object Telemetry : Table("telemetry") {
    val id = long("id").autoIncrement()
    val timestamp = long("timestamp")
    val eventType = varchar("event_type", 50)
    val userId = varchar("user_id", 100).nullable()
    val endpoint = varchar("endpoint", 200).nullable()
    val method = varchar("method", 10).nullable()
    val statusCode = integer("status_code").nullable()
    val responseTimeMs = long("response_time_ms").nullable()
    val userAgent = varchar("user_agent", 500).nullable()
    val ipAddress = varchar("ip_address", 50).nullable()
    val metadata = text("metadata").nullable()
    val screenName = varchar("screen_name", 100).nullable()
    val durationMs = long("duration_ms").nullable()
    val connectionType = varchar("connection_type", 20).nullable()
    val memoryMb = integer("memory_mb").nullable()
    val exceptionHash = varchar("exception_hash", 64).nullable()
    val optOut = bool("opt_out").default(false)

    override val primaryKey = PrimaryKey(id)
}

object Notifications : Table("notifications") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 100)
    val type = varchar("type", 50) // "trip_reminder", "collaboration", "deal", "checkin"
    val title = varchar("title", 200)
    val body = text("body")
    val data = text("data").nullable() // JSON payload for deep linking
    val read = bool("read").default(false)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object UserFcmTokens : Table("user_fcm_tokens") {
    val id = long("id").autoIncrement()
    val userId = varchar("user_id", 100)
    val token = varchar("token", 500)
    val platform = varchar("platform", 20) // "android", "ios", "web"
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
