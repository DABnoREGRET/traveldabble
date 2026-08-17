package com.dabber.traveldabble.config

import com.dabber.traveldabble.db.*
import com.dabber.traveldabble.seed.SeedData
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI
import java.net.URLDecoder
import javax.sql.DataSource

object DatabaseFactory {
    lateinit var dataSource: DataSource
        private set

    fun init(config: ApplicationConfig) {
        val rawUrl = System.getenv("DATABASE_URL")
            ?: System.getenv("DB_URL")
            ?: config.propertyOrNull("database.url")?.getString()
            ?: "jdbc:h2:mem:traveldabble;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE"
        val defaultUser = System.getenv("DATABASE_USER")
            ?: System.getenv("DB_USER")
            ?: config.propertyOrNull("database.user")?.getString()
            ?: "sa"
        val defaultPassword = System.getenv("DATABASE_PASSWORD")
            ?: System.getenv("DB_PASSWORD")
            ?: config.propertyOrNull("database.password")?.getString()
            ?: ""
        val maxPoolSize = System.getenv("DATABASE_MAX_POOL_SIZE")?.toIntOrNull()
            ?: config.propertyOrNull("database.maxPoolSize")?.getString()?.toIntOrNull()
            ?: 10

        init(rawUrl, defaultUser, defaultPassword, maxPoolSize)
    }

    fun init(
        rawUrl: String = "jdbc:h2:mem:traveldabble;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
        defaultUser: String = "sa",
        defaultPassword: String = "",
        maxPoolSize: Int = 10,
    ) {
        val (jdbcUrl, user, password) = resolveDbUrl(rawUrl, defaultUser, defaultPassword)
        dataSource = hikari(jdbcUrl, user, password, maxPoolSize)
        Database.connect(dataSource)
        migrate(dataSource)
        SeedData.seed()
    }

    private fun resolveDbUrl(
        url: String,
        defaultUser: String,
        defaultPassword: String,
    ): Triple<String, String, String> {
        if (url.startsWith("jdbc:")) {
            return Triple(url, defaultUser, defaultPassword)
        }
        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            val uri = URI(url)
            val userInfo = uri.userInfo?.split(":", limit = 2).orEmpty()
            val user = userInfo.getOrNull(0)?.let { URLDecoder.decode(it, "UTF-8") } ?: defaultUser
            val password = userInfo.getOrNull(1)?.let { URLDecoder.decode(it, "UTF-8") } ?: defaultPassword
            val host = uri.host ?: error("DATABASE_URL is missing a host")
            val port = if (uri.port == -1) 5432 else uri.port
            val db = uri.path.removePrefix("/")
            val query = uri.query?.let { "?$it" }.orEmpty()
            return Triple("jdbc:postgresql://$host:$port/$db$query", user, password)
        }
        error("Unsupported DATABASE_URL '$url' — must start with jdbc:, postgres://, or postgresql://")
    }

    private fun hikari(url: String, user: String, password: String, maxPoolSize: Int): HikariDataSource {
        val cfg = HikariConfig().apply {
            driverClassName = if (url.startsWith("jdbc:h2:")) "org.h2.Driver" else "org.postgresql.Driver"
            jdbcUrl = url
            username = user
            this.password = password
            maximumPoolSize = maxPoolSize
            isAutoCommit = false
            transactionIsolation = if (url.startsWith("jdbc:h2:")) "TRANSACTION_READ_COMMITTED" else "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(cfg)
    }

    private fun migrate(ds: DataSource) {
        try {
            Flyway.configure(DatabaseFactory::class.java.classLoader)
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate()
        } catch (e: Exception) {
            println("Flyway migration notice: ${e.message}")
        }

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users,
                Trips,
                DayPlans,
                Places,
                Activities,
                Budgets,
                Expenses,
                Destinations,
                TripMembers,
                InviteCodes,
                Telemetry,
                Notifications,
                UserFcmTokens,
                ItemsTable
            )
        }
    }
}
