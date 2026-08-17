package com.dabber.traveldabble.mcp

import com.dabber.traveldabble.web.SourceCurator
import kotlinx.serialization.json.*

object AdvisoryTool {

    private val curatedAdvisories = mapOf(
        "vietnam" to buildJsonObject {
            put("safety_rating", "High (Very Safe for Solo & Family Travelers)")
            put("visa_policy", "E-visa available for all countries (90 days multi-entry). Visa exemption for 25+ nationalities (15-45 days).")
            put("currency", "Vietnamese Dong (VND). Cash is essential for street vendors; credit cards widely accepted in hotels and restaurants.")
            put("transport_safety", "Use Grab app or reputable taxi brands (Mai Linh: 028 3838 3838, Vinasun: 028 3827 2727). Always wear helmets on motorbikes.")
            put("connectivity", "Viettel, Vinaphone, and Mobifone eSIMs/physical SIMs readily available at airports and official stores ($5-10 for 30GB+).")
            put("emergency_numbers", buildJsonObject {
                put("police", "113")
                put("ambulance", "115")
                put("fire", "114")
                put("tourist_hotline", "1080")
            })
            put("cultural_etiquette", buildJsonArray {
                add("Dress respectfully when visiting pagodas and temples (cover shoulders and knees)")
                add("Remove shoes before entering homes and sacred temple areas")
                add("Avoid pointing feet directly at religious shrines or elders")
                add("Use both hands when handing or receiving money/business cards")
            })
            put("common_scams_to_avoid", buildJsonArray {
                add("Unmetered rogue airport taxis — always use Grab or pre-booked airport counters")
                add("Shoe repair/cleaning aggressive street offers in Hanoi Old Quarter — politely decline immediately")
                add("Street fruit photo vendors demanding extortionate tip after putting shoulder poles on you — negotiate before photos")
            })
        },
        "ha giang" to buildJsonObject {
            put("special_advisory", "Ha Giang Loop Motorbike Regulations & Permits")
            put("permit_required", "Foreigners need a border area permit for Dong Van/Meo Vac ($10, easily arranged via hotel or immigration office).")
            put("road_conditions", "Steep mountain hairpins, sudden fog, construction zones. Only ride if experienced, or book an Easy Rider local driver.")
            put("fuel_and_cash", "ATMs are sparse outside Ha Giang City and Dong Van. Carry sufficient cash and refuel when passing town centers.")
        },
    )

    suspend fun getAdvisory(destination: String): JsonElement {
        val destKey = destination.lowercase()
        val match = if (destKey.contains("ha giang")) {
            curatedAdvisories["ha giang"]
        } else if (destKey.contains("vietnam") || destKey.contains("hanoi") || destKey.contains("saigon") || destKey.contains("da nang") || destKey.contains("hoi an")) {
            curatedAdvisories["vietnam"]
        } else {
            null
        }

        if (match != null) {
            return buildJsonObject {
                put("destination", destination)
                put("source", "curated")
                put("advisory", match)
            }
        }

        // Web search fallback
        val query = "travel advisory safety tips scams visa regulations $destination"
        val webResults = SourceCurator.searchAndCurate(query, maxSources = 2)

        return buildJsonObject {
            put("destination", destination)
            put("source", "web_search_fallback")
            put("advisory_search_results", buildJsonArray {
                webResults.forEach { source ->
                    addJsonObject {
                        put("title", source.title)
                        put("url", source.url)
                        put("summary", source.summary)
                    }
                }
            })
        }
    }
}
