package com.dabber.traveldabble.db

import org.jetbrains.exposed.dao.id.UUIDTable

object Users : UUIDTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 200)
    val displayName = varchar("display_name", 100)
    val createdAt = long("created_at")
}
