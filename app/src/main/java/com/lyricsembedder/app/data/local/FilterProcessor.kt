package com.lyricsembedder.app.data.local

/**
 * Applies user-configured case-insensitive regex/word filters to Title, Artist, and Album
 * fields before querying lyrics providers.
 */
object FilterProcessor {
    fun applyFilters(input: String, patterns: List<String>): String {
        if (input.isBlank() || patterns.isEmpty()) return input.trim()
        var result = input
        for (rawPattern in patterns) {
            val trimmed = rawPattern.trim()
            if (trimmed.isEmpty()) continue
            result = runCatching {
                Regex(trimmed, RegexOption.IGNORE_CASE).replace(result, "")
            }.getOrElse {
                // Fallback to literal case-insensitive replacement if regex syntax is invalid
                Regex(Regex.escape(trimmed), RegexOption.IGNORE_CASE).replace(result, "")
            }
        }
        return result.replace(Regex("\\s{2,}"), " ").trim()
    }
}
