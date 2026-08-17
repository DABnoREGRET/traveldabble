package com.dabber.traveldabble.web

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import java.net.URLDecoder

data class SearchResult(
    val title: String,
    val snippet: String,
    val url: String,
)

object WebSearchService {
    private val client by lazy {
        HttpClient(CIO) {
            expectSuccess = false
        }
    }

    private val resultRegex = Regex(
        """<a\s+class="result__url"\s+href="([^"]+)">.*?<a\s+class="result__snippet[^>]*>(.*?)</a>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    private val linkRegex = Regex(
        """<a\s+class="result__snippet[^"]*"\s+href="([^"]+)">(.*?)</a>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    private val titleRegex = Regex(
        """<a\s+class="result__a"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )

    suspend fun search(query: String, maxResults: Int = 5): List<SearchResult> {
        return try {
            val response = client.submitForm(
                url = "https://html.duckduckgo.com/html/",
                formParameters = Parameters.build {
                    append("q", query)
                }
            ) {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            }

            val html = response.bodyAsText()
            parseSearchResults(html).take(maxResults)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseSearchResults(html: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        val matches = titleRegex.findAll(html)

        for (match in matches) {
            val rawUrl = match.groupValues[1]
            val rawTitle = match.groupValues[2]

            val url = cleanDuckDuckGoUrl(rawUrl)
            val title = stripHtml(rawTitle)

            if (url.isNotBlank() && title.isNotBlank()) {
                results.add(
                    SearchResult(
                        title = title,
                        snippet = "",
                        url = url
                    )
                )
            }
        }
        return results
    }

    private fun cleanDuckDuckGoUrl(rawUrl: String): String {
        return if (rawUrl.contains("uddg=")) {
            val encoded = rawUrl.substringAfter("uddg=").substringBefore("&")
            runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(rawUrl)
        } else {
            rawUrl
        }
    }

    fun stripHtml(input: String): String {
        return input
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }
}
