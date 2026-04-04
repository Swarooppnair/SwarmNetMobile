package com.bitchat.android.features.ai

class TranslationManager {
    
    companion object {
        @Volatile
        private var instance: TranslationManager? = null
        
        fun getInstance(): TranslationManager {
            return instance ?: synchronized(this) {
                instance ?: TranslationManager().also { instance = it }
            }
        }
        
        // Basic dictionary for common phrases
        private val dictionary = mapOf(
            "en" to mapOf(
                "hello" to mapOf("hi" to "नमस्ते", "kn" to "ನಮಸ್ಕಾರ"),
                "help" to mapOf("hi" to "मदद", "kn" to "ಸಹಾಯ"),
                "emergency" to mapOf("hi" to "आपातकाल", "kn" to "ತುರ್ತು"),
                "water" to mapOf("hi" to "पानी", "kn" to "ನೀರು"),
                "food" to mapOf("hi" to "खाना", "kn" to "ಆಹಾರ"),
                "danger" to mapOf("hi" to "खतरा", "kn" to "ಅಪಾಯ")
            )
        )
    }
    
    private var enabled = true
    
    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
    
    fun translate(text: String, targetLang: String): String {
        if (!enabled) return text
        if (targetLang == "en") return text
        
        val words = text.lowercase().split(" ")
        val translated = words.map { word ->
            dictionary["en"]?.get(word)?.get(targetLang) ?: word
        }
        
        return translated.joinToString(" ")
    }
    
    fun getSupportedLanguages(): List<String> {
        return listOf("en", "hi", "kn")
    }
}
