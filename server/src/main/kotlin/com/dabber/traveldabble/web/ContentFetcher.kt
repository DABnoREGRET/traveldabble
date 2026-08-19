package com.dabber.traveldabble.web

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.withTimeoutOrNull

object ContentFetcher {
    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
        }
    }

    suspend fun fetchText(url: String, timeoutMillis: Long = 5000): String? {
        return withTimeoutOrNull(timeoutMillis) {
            try {
                val response = client.get(url) {
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                }
                val html = response.bodyAsText()
                extractCleanText(html)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun extractCleanText(html: String): String {
        var text = html
        // Remove script and style elements
        text = text.replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
        text = text.replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
        text = text.replace(Regex("<nav[\\s\\S]*?</nav>", RegexOption.IGNORE_CASE), " ")
        text = text.replace(Regex("<footer[\\s\\S]*?</footer>", RegexOption.IGNORE_CASE), " ")
        text = text.replace(Regex("<header[\\s\\S]*?</header>", RegexOption.IGNORE_CASE), " ")

        // Strip HTML tags
        text = WebSearchService.stripHtml(text)

        // Normalize whitespace
        text = text.replace(Regex("\\s+"), " ").trim()
        return text.take(3000)
    }
}
