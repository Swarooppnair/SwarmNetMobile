package com.bitchat.android.features.ai

import android.util.Log

enum class MessagePriority {
    URGENT, NORMAL, INFO, SPAM
}

class ContentIntelligence {
    
    companion object {
        private const val TAG = "ContentIntelligence"
        
        private val URGENT_KEYWORDS = setOf(
            "urgent", "emergency", "help", "sos", "fire", "attack", "danger", "critical",
            "injured", "accident", "911", "police", "ambulance", "rescue", "alert"
        )
        
        private val SPAM_KEYWORDS = setOf(
            "click here", "buy now", "limited offer", "act now", "free money",
            "congratulations you won", "claim your prize", "viagra", "casino"
        )
        
        private val INFO_KEYWORDS = setOf(
            "fyi", "info", "note", "reminder", "update", "announcement", "notice"
        )
        
        @Volatile
        private var instance: ContentIntelligence? = null
        
        fun getInstance(): ContentIntelligence {
            return instance ?: synchronized(this) {
                instance ?: ContentIntelligence().also { instance = it }
            }
        }
    }
    
    private var enabled = true
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    fun classify(text: String): MessagePriority {
        if (!enabled) return MessagePriority.NORMAL
        
        val lowerText = text.lowercase()
        
        // Check for spam first
        if (SPAM_KEYWORDS.any { lowerText.contains(it) }) {
            Log.d(TAG, "Classified as SPAM: $text")
            return MessagePriority.SPAM
        }
        
        // Check for urgent
        if (URGENT_KEYWORDS.any { lowerText.contains(it) }) {
            Log.d(TAG, "Classified as URGENT: $text")
            return MessagePriority.URGENT
        }
        
        // Check for info
        if (INFO_KEYWORDS.any { lowerText.contains(it) }) {
            return MessagePriority.INFO
        }
        
        return MessagePriority.NORMAL
    }
    
    fun summarize(text: String): String {
        if (!enabled || text.length < 200) return text
        
        // Simple extractive summarization
        val sentences = text.split(Regex("[.!?]\\s+"))
        if (sentences.size <= 2) return text
        
        // Take first sentence and last sentence
        val summary = "${sentences.first().trim()}... ${sentences.last().trim()}"
        
        Log.d(TAG, "Summarized: $summary")
        return summary
    }
    
    fun extractKeywords(text: String): List<String> {
        if (!enabled) return emptyList()
        
        val words = text.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 3 }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
        
        return words
    }
}
