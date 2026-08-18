package com.dabber.traveldabble.util

import com.dabber.traveldabble.model.CreateTripRequest
import com.dabber.traveldabble.model.LoginRequest
import com.dabber.traveldabble.model.RegisterRequest

object Validation {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
    private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}\$")
    private val TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d\$")

    fun nonBlank(value: String, field: String, max: Int = Int.MAX_VALUE): String? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return "$field is required"
        if (trimmed.length > max) return "$field must be at most $max characters"
        return null
    }

    fun email(value: String): String? {
        if (value.isBlank()) return "email is required"
        if (!EMAIL_REGEX.matches(value.trim())) return "email is not a valid address"
        if (value.length > 200) return "email is too long"
        return null
    }

    fun password(value: String): String? {
        if (value.isBlank()) return "password is required"
        if (value.length < 6) return "password must be at least 6 characters"
        if (value.length > 128) return "password is too long"
        return null
    }

    fun range(value: Int, field: String, min: Int, max: Int): String? {
        if (value < min || value > max) return "$field must be between $min and $max"
        return null
    }

    fun date(value: String, field: String): String? {
        if (value.isBlank()) return "$field is required"
        return null
    }

    fun time(value: String, field: String): String? {
        if (value.isBlank()) return "$field is required"
        if (!TIME_REGEX.matches(value.trim())) return "$field must use HH:MM (24h) format"
        return null
    }

    fun daysUntil(dateStr: String): Int? {
        if (dateStr.isBlank()) return null
        return runCatching {
            if (DATE_REGEX.matches(dateStr)) {
                val start = java.time.LocalDate.parse(dateStr)
                val today = java.time.LocalDate.now()
                val days = java.time.temporal.ChronoUnit.DAYS.between(today, start).toInt()
                if (days < 0) 0 else days
            } else {
                null
            }
        }.getOrNull()
    }

    fun validateRegister(req: RegisterRequest): List<String> {
        val errors = mutableListOf<String>()
        nonBlank(req.username, "username", 50)?.let { errors += it }
        if (req.username.isNotBlank() && req.username.trim().length < 3) {
            errors += "username must be at least 3 characters"
        }
        nonBlank(req.displayName, "displayName", 100)?.let { errors += it }
        email(req.email)?.let { errors += it }
        password(req.password)?.let { errors += it }
        return errors
    }

    fun validateLogin(req: LoginRequest): List<String> {
        val errors = mutableListOf<String>()
        if (req.email.trim().isBlank()) errors += "email or username is required"
        if (req.password.isBlank()) errors += "password is required"
        return errors
    }

    fun validateCreateTrip(req: CreateTripRequest): List<String> {
        val errors = mutableListOf<String>()
        nonBlank(req.title, "title", 200)?.let { errors += it }
        nonBlank(req.destination, "destination", 200)?.let { errors += it }
        nonBlank(req.country, "country", 100)?.let { errors += it }
        nonBlank(req.startDate, "startDate", 50)?.let { errors += it }
        nonBlank(req.endDate, "endDate", 50)?.let { errors += it }
        range(req.travelers, "travelers", 1, 99)?.let { errors += it }
        return errors
    }
}
