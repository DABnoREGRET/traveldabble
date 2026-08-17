package com.dabber.traveldabble

import com.dabber.traveldabble.config.DatabaseFactory
import com.dabber.traveldabble.db.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertTrue

class DatabaseFactoryTest {

    @Test
    fun testDatabaseInitializationAndSchemaCreation() {
        val testUrl = "jdbc:h2:mem:test_db_${System.currentTimeMillis()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
        DatabaseFactory.init(rawUrl = testUrl, defaultUser = "sa", defaultPassword = "")

        transaction {
            val userCount = Users.selectAll().count()
            assertTrue(userCount >= 1, "Expected demo user to be seeded in Users table")

            val tripsCount = Trips.selectAll().count()
            assertTrue(tripsCount >= 1, "Expected trips to be seeded in Trips table")

            val destCount = Destinations.selectAll().count()
            assertTrue(destCount >= 1, "Expected destinations to be seeded in Destinations table")

            val placesCount = Places.selectAll().count()
            assertTrue(placesCount >= 1, "Expected places to be seeded in Places table")
        }
    }
}
