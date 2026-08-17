package com.dabber.traveldabble.mcp

import com.dabber.traveldabble.web.SourceCurator
import kotlinx.serialization.json.*

object SeasonalTool {

    private val curatedSeasons = mapOf(
        "hanoi" to buildJsonObject {
            put("spring", "Feb-Apr: Cool, drizzly, blossoming peach trees, Lunar New Year festival atmosphere (18-24°C)")
            put("summer", "May-Aug: Hot, humid, sudden showers, vibrant night markets, lotus blossoms in West Lake (28-36°C)")
            put("autumn", "Sep-Nov: IDEAL SEASON. Crisp golden sunshine, mild breezes, blooming milk flowers, best street food weather (20-28°C)")
            put("winter", "Dec-Jan: Chilly, misty, perfect for steaming hot pho and egg coffee (10-18°C)")
            put("recommended_months", buildJsonArray { add("October"); add("November"); add("March"); add("April") })
            put("packing_tips", "Light jacket for evenings in autumn/winter, breathable cotton and umbrella for summer.")
        },
        "ha long" to buildJsonObject {
            put("spring", "Mar-May: Clear skies, calm emerald waters, perfect for kayaking and cruising (22-28°C)")
            put("summer", "Jun-Aug: Warm swimming, long daylight, occasional tropical storms (28-34°C)")
            put("autumn", "Sep-Nov: BEST CRUISING. Crystal visibility, golden sunsets over limestone karsts (20-26°C)")
            put("winter", "Dec-Feb: Moody mist, dramatic atmosphere, cooler sea temperatures (14-20°C)")
            put("recommended_months", buildJsonArray { add("April"); add("May"); add("October"); add("November") })
            put("packing_tips", "Swimwear, light windbreaker for cruise deck at night, sunglasses, waterproof dry-bag.")
        },
        "hoi an" to buildJsonObject {
            put("dry_season", "Feb-Aug: Sunny, ideal for An Bang Beach and cycling to Tra Que herb village (25-34°C)")
            put("rainy_season", "Sep-Jan: High rainfall, periodic river flooding in Old Quarter creating unique photo ops (19-24°C)")
            put("recommended_months", buildJsonArray { add("February"); add("March"); add("April"); add("May") })
            put("packing_tips", "Comfortable walking sandals, sun hat, light linen clothing, quick-dry clothes.")
        },
        "da nang" to buildJsonObject {
            put("dry_season", "Jan-Jul: Warm, low humidity, perfect waves at My Khe Beach, clear views at Ba Na Hills (25-33°C)")
            put("rainy_season", "Aug-Dec: Showers, lush green foliage at Son Tra Peninsula (20-26°C)")
            put("recommended_months", buildJsonArray { add("March"); add("April"); add("May"); add("June") })
            put("packing_tips", "Sunscreen, beach towel, sneakers for Marble Mountain stair climb.")
        },
        "ha giang" to buildJsonObject {
            put("spring", "Feb-Mar: Peach and plum blossoms painted across rocky limestone plateaus (15-22°C)")
            put("summer", "May-Jul: Water pouring season over rice terraces, emerald green mountain passes (22-30°C)")
            put("autumn", "Sep-Nov: Buckwheat flower bloom (Tam Giac Mach) and golden ripe terraced rice fields (15-24°C)")
            put("winter", "Dec-Jan: Cold mountain mist, frost on high peaks, authentic fireside stilt house homestays (5-15°C)")
            put("recommended_months", buildJsonArray { add("October"); add("November"); add("March") })
            put("packing_tips", "Warm thermal layers, sturdy motorbike gloves, hiking boots, rain poncho.")
        },
        "ninh binh" to buildJsonObject {
            put("harvest_season", "May-Jun: Tam Coc golden rice harvest by wooden rowboat (26-34°C)")
            put("lotus_season", "Jun-Jul: Lotus blooms in the shadow of Hang Mua peak (28-35°C)")
            put("autumn", "Sep-Nov: Mild temperatures, pleasant cave boat tours at Trang An (20-27°C)")
            put("recommended_months", buildJsonArray { add("May"); add("September"); add("October") })
            put("packing_tips", "Hat and umbrella for shade during open rowboat tours, slip-resistant shoes for Hang Mua.")
        },
        "ho chi minh" to buildJsonObject {
            put("dry_season", "Dec-Apr: Sunny days, warm breezes, vibrant rooftop lounge culture (26-34°C)")
            put("wet_season", "May-Nov: Afternoon brief tropical downpours that quickly clear, lush delta produce (25-32°C)")
            put("recommended_months", buildJsonArray { add("December"); add("January"); add("February"); add("March") })
            put("packing_tips", "Light summer clothes, compact umbrella, comfortable walking shoes for city walks.")
        },
    )

    suspend fun getRecommendations(destination: String, travelMonth: String? = null): JsonElement {
        val destKey = destination.lowercase()
        val match = curatedSeasons.entries.firstOrNull { destKey.contains(it.key) }?.value

        if (match != null) {
            return buildJsonObject {
                put("destination", destination)
                put("source", "curated")
                put("seasonal_guide", match)
                travelMonth?.let { put("query_month", it) }
            }
        }

        // Web search fallback
        val query = "best time to visit $destination seasons weather month guide"
        val webResults = SourceCurator.searchAndCurate(query, maxSources = 2)

        return buildJsonObject {
            put("destination", destination)
            put("source", "web_search_fallback")
            put("query_month", travelMonth ?: "all")
            put("findings", buildJsonArray {
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
