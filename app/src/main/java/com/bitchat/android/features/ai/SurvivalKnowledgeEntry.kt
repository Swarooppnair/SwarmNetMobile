package com.bitchat.android.features.ai

data class SurvivalKnowledgeEntry(
    val id: String,
    val title: String,
    val category: String,
    val tags: List<String>,
    val keywords: List<String>,
    val summary: String,
    val steps: List<String>,
    val warnings: List<String>,
    val whenToSeekHelp: String
)
