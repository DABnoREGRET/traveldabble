package com.dabber.traveldabble.mcp

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.*

object WeatherTool {
    private val client by lazy {
        HttpClient(CIO) {
            expectSuccess = false
        }
    }

    private val apiKey: String
        get() = System.getenv("OPENWEATHERMAP_API_KEY") ?: "8da2e2213055242b57a2366f02147783"

    suspend fun getWeatherForecast(destination: String): JsonElement {
        val cleanDestination = destination.trim().split(",").first().trim()
        val url = "https://api.openweathermap.org/data/2.5/forecast?q=${cleanDestination}&appid=$apiKey&units=metric"

        return try {
            val response = client.get(url)
            val responseText = response.bodyAsText()
            val jsonElement = Json.parseToJsonElement(responseText).jsonObject

            val cod = jsonElement["cod"]?.jsonPrimitive?.contentOrNull
            if (cod != "200") {
                return buildJsonObject {
                    put("status", "error")
                    put("message", jsonElement["message"]?.jsonPrimitive?.contentOrNull ?: "Failed to fetch weather forecast")
                    put("destination", destination)
                }
            }

            val cityObj = jsonElement["city"]?.jsonObject
            val cityName = cityObj?.get("name")?.jsonPrimitive?.contentOrNull ?: destination
            val country = cityObj?.get("country")?.jsonPrimitive?.contentOrNull ?: ""
            val list = jsonElement["list"]?.jsonArray ?: JsonArray(emptyList())

            val dailyForecasts = mutableListOf<JsonObject>()
            val processedDates = mutableSetOf<String>()

            for (item in list) {
                val itemObj = item.jsonObject
                val dtTxt = itemObj["dt_txt"]?.jsonPrimitive?.contentOrNull ?: continue
                val date = dtTxt.substringBefore(" ")

                if (!processedDates.contains(date)) {
                    processedDates.add(date)
                    val main = itemObj["main"]?.jsonObject
                    val weatherArr = itemObj["weather"]?.jsonArray
                    val firstWeather = weatherArr?.firstOrNull()?.jsonObject

                    dailyForecasts.add(
                        buildJsonObject {
                            put("date", date)
                            put("time", dtTxt.substringAfter(" "))
                            put("temp_c", main?.get("temp")?.jsonPrimitive?.doubleOrNull ?: 0.0)
                            put("feels_like_c", main?.get("feels_like")?.jsonPrimitive?.doubleOrNull ?: 0.0)
                            put("humidity_percent", main?.get("humidity")?.jsonPrimitive?.intOrNull ?: 0)
                            put("condition", firstWeather?.get("main")?.jsonPrimitive?.contentOrNull ?: "Clear")
                            put("description", firstWeather?.get("description")?.jsonPrimitive?.contentOrNull ?: "")
                            put("icon", firstWeather?.get("icon")?.jsonPrimitive?.contentOrNull ?: "")
                        }
                    )
                }
            }

            buildJsonObject {
                put("status", "success")
                put("city", cityName)
                put("country", country)
                put("forecast_days", JsonArray(dailyForecasts))
                put("source", "OpenWeatherMap 5-day/3-hour Forecast API")
            }
        } catch (e: Exception) {
            buildJsonObject {
                put("status", "error")
                put("message", "Weather service unavailable: ${e.message}")
                put("destination", destination)
            }
        }
    }
}
