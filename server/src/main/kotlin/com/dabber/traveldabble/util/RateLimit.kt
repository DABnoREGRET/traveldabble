package com.dabber.traveldabble.util

import com.dabber.traveldabble.model.ApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Plugin
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.util.AttributeKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class RateLimitConfig {
    var limit: Int = 30
    var windowMillis: Long = 60_000
}

class RateLimitPlugin private constructor(private val limit: Int, private val windowMillis: Long) {

    private data class Bucket(var count: AtomicLong, var windowStart: AtomicLong)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    private fun allowed(key: String): Boolean {
        val now = System.currentTimeMillis()
        val bucket = buckets.compute(key) { _, existing ->
            val b = existing ?: Bucket(AtomicLong(0), AtomicLong(now))
            if (now - b.windowStart.get() >= windowMillis) {
                b.count.set(0)
                b.windowStart.set(now)
            }
            b
        }
        return bucket!!.count.incrementAndGet() <= limit
    }

    companion object : Plugin<ApplicationCallPipeline, RateLimitConfig, RateLimitPlugin> {
        override val key = AttributeKey<RateLimitPlugin>("RateLimitPlugin")

        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: RateLimitConfig.() -> Unit,
        ): RateLimitPlugin {
            val config = RateLimitConfig().apply(configure)
            val plugin = RateLimitPlugin(config.limit, config.windowMillis)
            pipeline.intercept(ApplicationCallPipeline.Plugins) {
                val key = call.request.origin.remoteAddress
                if (!plugin.allowed(key)) {
                    call.respond(HttpStatusCode.TooManyRequests, ApiError("Rate limit exceeded. Try again later."))
                    finish()
                }
            }
            return plugin
        }
    }
}

fun Route.rateLimited(limit: Int = 30, windowMillis: Long = 60_000, build: Route.() -> Unit) {
    install(RateLimitPlugin) {
        this.limit = limit
        this.windowMillis = windowMillis
    }
    build(this)
}
