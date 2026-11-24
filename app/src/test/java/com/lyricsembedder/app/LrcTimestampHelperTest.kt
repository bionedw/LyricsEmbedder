package com.lyricsembedder.app

import com.lyricsembedder.app.util.LrcTimestampHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcTimestampHelperTest {
    @Test
    fun shiftsSyncedLrcTimestampsForwardAndBackward() {
        val input = "[00:10.50] First line\n[01:02.00] Second line"
        val forward = LrcTimestampHelper.shiftAllTimestamps(input, 100L)
        assertEquals("[00:10.60] First line\n[01:02.10] Second line", forward)

        val backward = LrcTimestampHelper.shiftAllTimestamps(input, -500L)
        assertEquals("[00:10.00] First line\n[01:01.50] Second line", backward)
    }

    @Test
    fun detectsSyncedLyrics() {
        assertTrue(LrcTimestampHelper.isSyncedLyrics("[00:12.34] Hello world"))
    }
}
