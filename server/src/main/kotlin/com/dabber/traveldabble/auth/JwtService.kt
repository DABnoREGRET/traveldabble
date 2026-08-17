package com.dabber.traveldabble.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtService {
    val secret: String = System.getenv("JWT_SECRET") ?: "traveldabble-secret-key-change-in-production-2026"
    const val ISSUER: String = "traveldabble"
    const val AUDIENCE: String = "traveldabble-users"

    val algorithm: Algorithm = Algorithm.HMAC256(secret)

    fun createToken(userId: String): String =
        JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000))
            .sign(algorithm)
}
