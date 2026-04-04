package com.bitchat.android.features.ai

class SurvivalResponseFormatter {

    fun format(matches: List<SurvivalKnowledgeEntry>): String {
        if (matches.isEmpty()) {
            return "I couldn't find a direct match for that topic.\n\nStay calm, conserve energy, and prioritize:\n• Finding clean water\n• Building shelter\n• Signaling for rescue\n\nTry asking about: bleeding, burns, water purification, snake bite, fire, shelter, hypothermia, CPR, or navigation."
        }

        val best = matches.first()
        val sb = StringBuilder()

        sb.append("🚨 **${best.title}**\n")
        sb.append("*${best.category}*\n\n")
        sb.append(best.summary).append("\n\n")

        if (best.steps.isNotEmpty()) {
            sb.append("✅ **Steps:**\n")
            best.steps.forEach { sb.append("$it\n") }
            sb.append("\n")
        }

        if (best.warnings.isNotEmpty()) {
            sb.append("⚠️ **Do NOT:**\n")
            best.warnings.forEach { sb.append("• $it\n") }
            sb.append("\n")
        }

        if (best.whenToSeekHelp.isNotBlank()) {
            sb.append("🏥 ${best.whenToSeekHelp}")
        }

        if (matches.size > 1) {
            sb.append("\n\n---\n*Related: ${matches.drop(1).joinToString(", ") { it.title }}*")
        }

        return sb.toString().trim()
    }
}
