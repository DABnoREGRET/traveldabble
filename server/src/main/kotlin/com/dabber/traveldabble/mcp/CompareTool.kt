package com.dabber.traveldabble.mcp

import com.dabber.traveldabble.db.Destinations
import com.dabber.traveldabble.db.Mappers
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object CompareTool {

    fun compare(destinationA: String, destinationB: String): JsonElement {
        val destA = fetchDestInfo(destinationA)
        val destB = fetchDestInfo(destinationB)

        val comparisonLogic = buildJsonObject {
            put("summary", "Comparison between $destinationA and $destinationB")
            put("recommendation", when {
                destinationA.contains("ha giang", ignoreCase = true) -> "$destinationA is tailored for thrill-seeking adventure riders and dramatic landscapes, whereas $destinationB offers a more relaxing cultural or coastal escape."
                destinationA.contains("hoi an", ignoreCase = true) -> "$destinationA is unrivaled for romantic lantern-lit charm, historic architecture, and artisan cuisine, while $destinationB delivers a distinct vibe."
                destinationA.contains("hanoi", ignoreCase = true) -> "$destinationA is the cultural and historical heartbeat with legendary culinary depth, compared to the dynamic character of $destinationB."
                else -> "Both destinations offer distinct Vietnamese travel experiences. Choose based on whether your priority is nature/adventure or culture/relaxation."
            })
        }

        return buildJsonObject {
            put("status", "success")
            put("destination_a", destA)
            put("destination_b", destB)
            put("analysis", comparisonLogic)
        }
    }

    private fun fetchDestInfo(name: String): JsonElement = transaction {
        val row = Destinations.selectAll().toList().firstOrNull {
            it[Destinations.name].contains(name, ignoreCase = true) || name.contains(it[Destinations.name], ignoreCase = true)
        }

        if (row != null) {
            buildJsonObject {
                put("name", row[Destinations.name])
                put("country", row[Destinations.country])
                put("tagline", row[Destinations.tagline])
                put("rating", row[Destinations.rating])
                put("tags", Mappers.json.parseToJsonElement(row[Destinations.tags]))
            }
        } else {
            buildJsonObject {
                put("name", name)
                put("note", "Custom destination outside default catalog")
            }
        }
    }
}
