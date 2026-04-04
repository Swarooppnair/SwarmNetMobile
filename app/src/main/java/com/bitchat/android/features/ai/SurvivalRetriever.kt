package com.bitchat.android.features.ai

class SurvivalRetriever(private val knowledge: List<SurvivalKnowledgeEntry>) {

    fun findMatches(query: String, maxResults: Int = 3, threshold: Double = 0.30): List<SurvivalKnowledgeEntry> {
        val clean = query.lowercase().trim()
        if (clean.isBlank()) return emptyList()

        val words = clean.split("\\W+".toRegex()).filter { it.length > 2 }

        val scored = knowledge.map { entry ->
            var score = 0.0
            val titleLower = entry.title.lowercase()

            // Exact / partial title match
            if (titleLower == clean) score += 3.0
            else if (titleLower.contains(clean)) score += 1.5

            // Keyword match
            entry.keywords.forEach { kw ->
                val kwl = kw.lowercase()
                if (clean.contains(kwl)) score += 1.0
                words.forEach { w -> if (kwl.contains(w)) score += 0.5 }
            }

            // Tag match
            entry.tags.forEach { tag ->
                if (clean.contains(tag.lowercase())) score += 0.7
                words.forEach { w -> if (tag.lowercase().contains(w)) score += 0.3 }
            }

            // Summary word overlap
            words.forEach { w ->
                if (entry.summary.lowercase().contains(w)) score += 0.2
            }

            entry to score
        }

        return scored
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .take(maxResults)
            .map { it.first }
    }
}
