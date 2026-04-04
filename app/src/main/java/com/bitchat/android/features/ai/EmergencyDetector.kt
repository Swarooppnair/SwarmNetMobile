package com.bitchat.android.features.ai

import android.util.Log

class EmergencyDetector {
    
    companion object {
        private const val TAG = "EmergencyDetector"
        
        private val EMERGENCY_KEYWORDS = setOf(
            "help", "fire", "attack", "injured", "danger", "emergency", "sos",
            "911", "police", "ambulance", "rescue", "critical", "urgent",
            "bleeding", "unconscious", "trapped", "drowning", "choking",
            "heart attack", "stroke", "seizure", "overdose", "suicide"
        )
        
        private val EMERGENCY_PATTERNS = listOf(
            Regex("help\\s+(me|us|someone)", RegexOption.IGNORE_CASE),
            Regex("need\\s+(help|assistance|rescue)", RegexOption.IGNORE_CASE),
            Regex("call\\s+(911|police|ambulance)", RegexOption.IGNORE_CASE),
            Regex("someone\\s+(is\\s+)?(hurt|injured|dying)", RegexOption.IGNORE_CASE)
        )
        
        @Volatile
        private var instance: EmergencyDetector? = null
        
        fun getInstance(): EmergencyDetector {
            return instance ?: synchronized(this) {
                instance ?: EmergencyDetector().also { instance = it }
            }
        }
    }
    
    private var enabled = true
    private val emergencyCallbacks = mutableListOf<(String) -> Unit>()
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    fun isEmergency(text: String): Boolean {
        if (!enabled) return false
        
        val lowerText = text.lowercase()
        
        // Check keywords
        val hasKeyword = EMERGENCY_KEYWORDS.any { lowerText.contains(it) }
        
        // Check patterns
        val matchesPattern = EMERGENCY_PATTERNS.any { it.containsMatchIn(text) }
        
        val isEmergency = hasKeyword || matchesPattern
        
        if (isEmergency) {
            Log.w(TAG, "EMERGENCY DETECTED: $text")
            notifyEmergency(text)
        }
        
        return isEmergency
    }
    
    fun registerEmergencyCallback(callback: (String) -> Unit) {
        emergencyCallbacks.add(callback)
    }
    
    private fun notifyEmergency(message: String) {
        emergencyCallbacks.forEach { it(message) }
    }
    
    fun getEmergencyLevel(text: String): Int {
        if (!isEmergency(text)) return 0
        
        val lowerText = text.lowercase()
        var level = 1
        
        // Critical keywords increase level
        if (lowerText.contains("dying") || lowerText.contains("critical") || 
            lowerText.contains("unconscious") || lowerText.contains("bleeding")) {
            level = 3
        } else if (lowerText.contains("injured") || lowerText.contains("hurt") ||
                   lowerText.contains("fire") || lowerText.contains("attack")) {
            level = 2
        }
        
        return level
    }
}
