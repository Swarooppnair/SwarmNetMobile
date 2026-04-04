package com.bitchat.android.features.ai

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class SurvivalKnowledgeLoader(private val context: Context) {

    fun load(assetPath: String = "survival_knowledge.json"): List<SurvivalKnowledgeEntry> {
        val entries = mutableListOf<SurvivalKnowledgeEntry>()
        try {
            val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                entries.add(parseEntry(obj))
            }
        } catch (e: Exception) {
            Log.e("SurvivalKnowledgeLoader", "Failed to load: ${e.message}")
        }
        return entries
    }

    private fun parseEntry(obj: JSONObject) = SurvivalKnowledgeEntry(
        id = obj.optString("id", "unknown"),
        title = obj.optString("title", "Survival Guide"),
        category = obj.optString("category", "General"),
        tags = toList(obj.optJSONArray("tags")),
        keywords = toList(obj.optJSONArray("keywords")),
        summary = obj.optString("summary", ""),
        steps = toList(obj.optJSONArray("steps")),
        warnings = toList(obj.optJSONArray("warnings")),
        whenToSeekHelp = obj.optString("whenToSeekHelp", "Seek help if unsure.")
    )

    private fun toList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
    }
}
