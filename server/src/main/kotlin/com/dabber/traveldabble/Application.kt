package com.dabber.traveldabble

import com.auth0.jwt.JWT
import com.dabber.traveldabble.auth.JwtService
import com.dabber.traveldabble.config.DatabaseFactory
import com.dabber.traveldabble.di.serverModule
import com.dabber.traveldabble.mcp.mcpRoutes
import com.dabber.traveldabble.model.ApiError
import com.dabber.traveldabble.routes.*
import com.dabber.traveldabble.telemetry.TelemetryPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

private val appLogger = LoggerFactory.getLogger("com.dabber.traveldabble.Application")

fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = System.getenv("HOST") ?: "0.0.0.0"
    appLogger.info("Starting Travel Dabble Server on $host:$port...")
    embeddedServer(Netty, port = port, host = host, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init(environment.config)

    if (org.koin.core.context.GlobalContext.getOrNull() == null) {
        install(Koin) {
            slf4jLogger()
            modules(serverModule)
        }
    }

    install(DefaultHeaders)
    install(CallLogging) { level = Level.INFO }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Api-Key")
        allowHeader("X-Telemetry-Opt-Out")
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }

    install(TelemetryPlugin)

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "traveldabble"
            verifier(
                JWT.require(JwtService.algorithm)
                    .withIssuer(JwtService.ISSUER)
                    .withAudience(JwtService.AUDIENCE)
                    .build()
            )
            validate { credential ->
                val subject = credential.payload.subject
                if (subject.isNullOrBlank()) {
                    null
                } else {
                    JWTPrincipal(credential.payload)
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ApiError("Invalid or expired authentication token"))
            }
        }
    }

    install(StatusPages) {
        exception<io.ktor.server.plugins.NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ApiError(cause.message ?: "Resource not found"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError(cause.message ?: "Invalid request"))
        }
        exception<Throwable> { call, cause ->
            appLogger.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("Internal server error: ${cause.message}"))
        }
    }

    routing {
        get("/") { call.respondText("TravelDabble API v1.0") }
        get("/health") { call.respondText("ok") }

        AuthRoutes()
        TripRoutes()
        TripContentRoutes()
        TripMemberRoutes()
        DestinationRoutes()
        RouteRoutes()
        AiRoutes()
        mcpRoutes()
        NotificationRoutes()
        TelemetryRoutes()
    }
}
