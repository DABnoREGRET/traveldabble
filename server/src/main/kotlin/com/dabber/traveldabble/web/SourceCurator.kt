package com.dabber.traveldabble.web

object SourceCurator {

    data class CuratedSource(
        val title: String,
        val url: String,
        val summary: String,
    )

    suspend fun searchAndCurate(query: String, maxSources: Int = 2): List<CuratedSource> {
        val searchResults = WebSearchService.search(query, maxResults = 4)
        if (searchResults.isEmpty()) return emptyList()

        val curated = mutableListOf<CuratedSource>()
        for (result in searchResults.take(maxSources)) {
            val text = ContentFetcher.fetchText(result.url, timeoutMillis = 3000)
            if (!text.isNullOrBlank()) {
                curated.add(
                    CuratedSource(
                        title = result.title,
                        url = result.url,
                        summary = text.take(500),
                    )
                )
            } else if (result.snippet.isNotBlank()) {
                curated.add(
                    CuratedSource(
                        title = result.title,
                        url = result.url,
                        summary = result.snippet,
                    )
                )
            }
        }
        return curated
    }
}
