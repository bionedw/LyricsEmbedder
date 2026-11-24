package com.lyricsembedder.app

import com.lyricsembedder.app.util.TrackOrderHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackOrderHelperTest {
    @Test
    fun extractsOrderFromUntaggedTrackFilenames() {
        assertEquals(1, TrackOrderHelper.extractOrderNumber("Track 1.flac"))
        assertEquals(2, TrackOrderHelper.extractOrderNumber("02 Track 2.flac"))
        assertEquals(10, TrackOrderHelper.extractOrderNumber("10 Track 10.flac"))
        assertEquals(15, TrackOrderHelper.extractOrderNumber("15 - Starlight.mp3"))
    }

    @Test
    fun prefersEmbeddedTrackNumberWhenPresent() {
        assertEquals(7, TrackOrderHelper.extractOrderNumber("Track 1.flac", embeddedTrackNumber = 7))
    }
}
