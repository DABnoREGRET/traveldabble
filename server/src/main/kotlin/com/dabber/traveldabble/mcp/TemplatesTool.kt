package com.dabber.traveldabble.mcp

import com.dabber.traveldabble.web.SourceCurator
import kotlinx.serialization.json.*

object TemplatesTool {

    private val curatedTemplates = listOf(
        buildJsonObject {
            put("id", "tpl_hanoi_halong_4d")
            put("title", "4-Day Hanoi Heritage & Ha Long Luxury Cruise")
            put("region", "North Vietnam")
            put("duration_days", 4)
            put("pace", "Moderate")
            put("estimated_budget_usd", 450.0)
            put("days", buildJsonArray {
                addJsonObject {
                    put("day", 1)
                    put("title", "Hanoi French Quarter & Street Food")
                    put("highlights", buildJsonArray { add("Check-in Sofitel Metropole / Old Quarter"); add("St. Joseph Cathedral"); add("Café Giảng Egg Coffee"); add("Bún Chả Hương Liên") })
                }
                addJsonObject {
                    put("day", 2)
                    put("title", "Culture, Temples & Old Guild Streets")
                    put("highlights", buildJsonArray { add("Temple of Literature"); add("Ho Chi Minh Complex"); add("Old Quarter Silk Street"); add("Water Puppet Show") })
                }
                addJsonObject {
                    put("day", 3)
                    put("title", "Transfer to Ha Long & Overnight Cruise")
                    put("highlights", buildJsonArray { add("Limousine to Tuan Chau Marina"); add("Sung Sot Cave exploration"); add("Sunset kayaking in Luon Lagoon") })
                }
                addJsonObject {
                    put("day", 4)
                    put("title", "Ti Top Peak & Return to Hanoi")
                    put("highlights", buildJsonArray { add("Morning Tai Chi on sundeck"); add("Climb Ti Top Island for 360° bay view"); add("Limousine transfer back to Hanoi") })
                }
            })
        },
        buildJsonObject {
            put("id", "tpl_central_coast_5d")
            put("title", "5-Day Central Vietnam: Hoi An Lanterns & Da Nang Coast")
            put("region", "Central Vietnam")
            put("duration_days", 5)
            put("pace", "Relaxed")
            put("estimated_budget_usd", 380.0)
            put("days", buildJsonArray {
                addJsonObject {
                    put("day", 1)
                    put("title", "Arrival & Hoi An Ancient Town")
                    put("highlights", buildJsonArray { add("Check in An Bang Beach / Ancient Town"); add("Japanese Covered Bridge"); add("Bánh Mì Phượng"); add("Lantern Night Walk") })
                }
                addJsonObject {
                    put("day", 2)
                    put("title", "Craft Villages & Beach Relaxation")
                    put("highlights", buildJsonArray { add("Tra Que herb village cooking class"); add("Afternoon swim at An Bang Beach"); add("Riverside seafood dinner") })
                }
                addJsonObject {
                    put("day", 3)
                    put("title", "Golden Bridge & Ba Na Hills")
                    put("highlights", buildJsonArray { add("World-record cable car"); add("Golden Bridge giant stone hands"); add("French Village alpine coaster") })
                }
                addJsonObject {
                    put("day", 4)
                    put("title", "Da Nang Coast & Marble Mountains")
                    put("highlights", buildJsonArray { add("Marble Mountain Buddhist grottoes"); add("My Khe beach walk"); add("Dragon Bridge Weekend Fire Show") })
                }
                addJsonObject {
                    put("day", 5)
                    put("title", "Son Tra Peninsula & Departure")
                    put("highlights", buildJsonArray { add("Linh Ung Pagoda giant Lady Buddha"); add("Son Tra monkey pass viewpoint"); add("Airport departure") })
                }
            })
        },
        buildJsonObject {
            put("id", "tpl_hagiang_loop_4d")
            put("title", "4-Day Epic Ha Giang Motorbike Loop")
            put("region", "North Vietnam (Far North)")
            put("duration_days", 4)
            put("pace", "Active / Adventure")
            put("estimated_budget_usd", 220.0)
            put("days", buildJsonArray {
                addJsonObject {
                    put("day", 1)
                    put("title", "Ha Giang City to Dong Van Ancient Town")
                    put("highlights", buildJsonArray { add("Motorbike briefing"); add("Quan Ba Heaven Gate & Fairy Twin Mountains"); add("Yen Minh pine forests"); add("Dong Van Ancient Town") })
                }
                addJsonObject {
                    put("day", 2)
                    put("title", "Ma Pi Leng Pass & Tu San Canyon")
                    put("highlights", buildJsonArray { add("Ma Pi Leng Skywalk"); add("Nho Que River boat cruise in deep canyon"); add("Meo Vac mountain town") })
                }
                addJsonObject {
                    put("day", 3)
                    put("title", "Meo Vac to Du Gia Waterfall Village")
                    put("highlights", buildJsonArray { add("Mau Due highland hairpins"); add("Du Gia crystal blue swimming hole"); add("Tay ethnic minority stilt house dinner") })
                }
                addJsonObject {
                    put("day", 4)
                    put("title", "Du Gia to Ha Giang City & Hanoi")
                    put("highlights", buildJsonArray { add("Morning village stroll"); add("Scenic backroad ride to Ha Giang"); add("Sleeper bus return to Hanoi") })
                }
            })
        },
    )

    suspend fun getTemplates(destination: String? = null, days: Int? = null): JsonElement {
        val destQuery = destination?.lowercase().orEmpty()

        val matching = curatedTemplates.filter { tpl ->
            val matchesDest = destQuery.isBlank() ||
                tpl["region"]?.jsonPrimitive?.content?.lowercase()?.contains(destQuery) == true ||
                tpl["title"]?.jsonPrimitive?.content?.lowercase()?.contains(destQuery) == true

            val matchesDays = days == null || tpl["duration_days"]?.jsonPrimitive?.intOrNull == days

            matchesDest && matchesDays
        }

        if (matching.isNotEmpty()) {
            return buildJsonObject {
                put("status", "success")
                put("source", "curated")
                put("templates", buildJsonArray { matching.forEach { add(it) } })
            }
        }

        // Web search fallback
        val query = "itinerary template plan ${days ?: 3} days in ${destination ?: "Vietnam"}"
        val webResults = SourceCurator.searchAndCurate(query, maxSources = 2)

        return buildJsonObject {
            put("status", "success")
            put("source", "web_search_fallback")
            put("templates", buildJsonArray {
                webResults.forEach { source ->
                    addJsonObject {
                        put("title", source.title)
                        put("summary", source.summary)
                        put("url", source.url)
                    }
                }
            })
        }
    }
}
