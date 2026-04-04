package com.bitchat.android.features.search

import com.bitchat.android.model.BitchatMessage

class SemanticSearch {
    
    companion object {
        @Volatile
        private var instance: SemanticSearch? = null
        
        fun getInstance(): SemanticSearch {
            return instance ?: synchronized(this) {
                instance ?: SemanticSearch().also { instance = it }
            }
        }
    }
    
    private var enabled = true
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    fun search(query: String, messages: List<BitchatMessage>): List<BitchatMessage> {
        if (!enabled || query.isBlank()) return emptyList()
        
        val queryWords = query.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        
        return messages
            .map { message ->
                val messageWords = message.content.lowercase().split(Regex("\\W+")).toSet()
                val score = calculateSimilarity(queryWords, messageWords)
                message to score
            }
            .filter { it.second > 0.1f }
            .sortedByDescending { it.second }
            .map { it.first }
    }
    
    private fun calculateSimilarity(query: Set<String>, message: Set<String>): Float {
        val intersection = query.intersect(message).size
        val union = query.union(message).size
        
        return if (union > 0) intersection.toFloat() / union else 0f
    }
}
