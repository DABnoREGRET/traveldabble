package com.dabber.traveldabble.mcp

import com.dabber.traveldabble.web.SourceCurator
import kotlinx.serialization.json.*

object EventsTool {

    private val curatedEvents = listOf(
        buildJsonObject {
            put("name", "Tet Nguyen Dan (Vietnamese Lunar New Year)")
            put("timing", "Late January to Mid-February (Annually)")
            put("locations", buildJsonArray { add("Nationwide"); add("Hanoi"); add("Ho Chi Minh City") })
            put("description", "Vietnam's most sacred celebration. Streets decorated with kumquat trees and peach blossoms, fireworks around Hoan Kiem Lake and Nguyen Hue Walking Street.")
            put("travel_tip", "Book long-distance transport and hotels weeks ahead as locals return to home provinces.")
        },
        buildJsonObject {
            put("name", "Hoi An Monthly Lantern Festival (Full Moon)")
            put("timing", "14th night of every lunar calendar month")
            put("locations", buildJsonArray { add("Hoi An Ancient Town") })
            put("description", "All electric streetlights in the Ancient Town are switched off. Thousands of colorful silk lanterns illuminate the streets and floating paper flower lanterns drift on the Hoai River.")
            put("travel_tip", "Arrive along the riverside before sunset (17:30) to rent a wooden sampan boat for floating lanterns.")
        },
        buildJsonObject {
            put("name", "Da Nang International Fireworks Festival (DIFF)")
            put("timing", "June - July (Weekends)")
            put("locations", buildJsonArray { add("Da Nang (Han River)") })
            put("description", "World-class pyrotechnic teams compete in synchronized musical firework displays over the Han River and Dragon Bridge.")
            put("travel_tip", "Book riverside rooftop lounges or Han River cruise tickets in advance.")
        },
        buildJsonObject {
            put("name", "Khau Vai Love Market Festival")
            put("timing", "26th-27th day of the 3rd lunar month (usually April/May)")
            put("locations", buildJsonArray { add("Meo Vac, Ha Giang") })
            put("description", "Centuries-old tribal gathering where ethnic minority men and women meet their ex-lovers in traditional colorful attire with singing, flutes, and horse races.")
            put("travel_tip", "Combine with riding the Ma Pi Leng Pass.")
        },
        buildJsonObject {
            put("name", "Mid-Autumn Festival (Tet Trung Thu)")
            put("timing", "15th day of the 8th lunar month (September/October)")
            put("locations", buildJsonArray { add("Hanoi Old Quarter (Hang Ma Street)"); add("Hoi An"); add("Saigon Chinatown") })
            put("description", "Children carrying glowing star lanterns, energetic lion and dragon dances in the streets, and traditional mooncakes.")
            put("travel_tip", "Hang Ma Street in Hanoi transforms into a dazzling wonderland of traditional and glowing toys.")
        },
        buildJsonObject {
            put("name", "Buckwheat Flower Festival (Tam Giac Mach)")
            put("timing", "October - November")
            put("locations", buildJsonArray { add("Dong Van Karst Plateau, Ha Giang") })
            put("description", "Valley slopes and cliffside plateaus burst into carpets of pink, purple, and white buckwheat blossoms.")
            put("travel_tip", "Best photographed in early morning light against the limestone karst backdrop.")
        },
    )

    suspend fun getLocalEvents(destination: String? = null, month: String? = null): JsonElement {
        val query = destination?.lowercase().orEmpty()
        val monthQuery = month?.lowercase().orEmpty()

        val matching = curatedEvents.filter { event ->
            val matchesDest = query.isBlank() || event["locations"]?.jsonArray?.any {
                it.jsonPrimitive.content.lowercase().contains(query) || it.jsonPrimitive.content.equals("Nationwide", ignoreCase = true)
            } == true || event["name"]?.jsonPrimitive?.content?.lowercase()?.contains(query) == true

            val matchesMonth = monthQuery.isBlank() || event["timing"]?.jsonPrimitive?.content?.lowercase()?.contains(monthQuery) == true

            matchesDest && matchesMonth
        }

        if (matching.isNotEmpty()) {
            return buildJsonObject {
                put("status", "success")
                put("source", "curated")
                destination?.let { put("destination", it) }
                month?.let { put("month", it) }
                put("events", buildJsonArray {
                    matching.forEach { add(it) }
                })
            }
        }

        // Web search fallback
        val searchQuery = if (destination != null) "festivals local events customs $destination $monthQuery" else "Vietnam festivals events calendar $monthQuery"
        val webResults = SourceCurator.searchAndCurate(searchQuery, maxSources = 2)

        return buildJsonObject {
            put("status", "success")
            put("source", "web_search_fallback")
            destination?.let { put("destination", it) }
            put("events", buildJsonArray {
                webResults.forEach { source ->
                    addJsonObject {
                        put("name", source.title)
                        put("description", source.summary)
                        put("url", source.url)
                    }
                }
            })
        }
    }
}
