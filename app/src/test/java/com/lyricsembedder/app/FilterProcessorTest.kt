package com.lyricsembedder.app

import com.lyricsembedder.app.data.local.FilterProcessor
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterProcessorTest {
    @Test
    fun removesCaseInsensitiveRegexPatterns() {
        val result = FilterProcessor.applyFilters(
            input = "All Too Well (Taylor's Version) [Official Audio]",
            patterns = listOf("\\[official audio\\]", "\\(remastered\\)")
        )
        assertEquals("All Too Well (Taylor's Version)", result)
    }
}
