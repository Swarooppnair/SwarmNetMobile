package com.bitchat.android.features.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SurvivalMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

class SurvivalViewModel(application: Application) : AndroidViewModel(application) {

    private val loader = SurvivalKnowledgeLoader(application.applicationContext)
    private val formatter = SurvivalResponseFormatter()
    private lateinit var retriever: SurvivalRetriever

    private val _messages = MutableStateFlow<List<SurvivalMessage>>(emptyList())
    val messages: StateFlow<List<SurvivalMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val knowledge = loader.load("survival_knowledge.json")
            retriever = SurvivalRetriever(knowledge)
            val welcome = SurvivalMessage(
                text = "Hello! I'm your offline Survival Assistant powered by a local knowledge base of ${knowledge.size} topics.\n\nAsk me about: bleeding, burns, water purification, snake bites, fire starting, shelter, hypothermia, CPR, navigation, and more.",
                isUser = false
            )
            _messages.value = listOf(welcome)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = SurvivalMessage(text.trim(), isUser = true)
        val loadingMsg = SurvivalMessage("...", isUser = false, isLoading = true)
        _messages.value = _messages.value + userMsg + loadingMsg
        _isLoading.value = true

        viewModelScope.launch {
            val response = withContext(Dispatchers.IO) {
                try {
                    if (!::retriever.isInitialized) {
                        val knowledge = loader.load("survival_knowledge.json")
                        retriever = SurvivalRetriever(knowledge)
                    }
                    val matches = retriever.findMatches(text.trim(), maxResults = 3)
                    formatter.format(matches)
                } catch (e: Exception) {
                    "Sorry, I encountered an error: ${e.localizedMessage}"
                }
            }

            // Replace loading bubble with real response
            val current = _messages.value.toMutableList()
            val loadingIndex = current.indexOfLast { it.isLoading }
            if (loadingIndex >= 0) current[loadingIndex] = SurvivalMessage(response, isUser = false)
            else current.add(SurvivalMessage(response, isUser = false))

            _messages.value = current
            _isLoading.value = false
        }
    }
}
