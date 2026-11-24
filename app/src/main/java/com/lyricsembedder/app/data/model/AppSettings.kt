package com.lyricsembedder.app.data.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class PreferredLyricsType {
    SYNCED,
    TEXT_ONLY
}

enum class Mp3TagVersion {
    ID3V23,
    ID3V24
}

data class AppSettings(
    // 1. Theme (Chủ đề)
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,

    // 2. Lyrics Request (Yêu cầu lời bài hát)
    val useExtendedSearch: Boolean = false,
    val notifyOnFailure: Boolean = true,
    val replacePreviousNotification: Boolean = false,
    val preferredLyricsType: PreferredLyricsType = PreferredLyricsType.SYNCED,
    val markInstrumentalTracks: Boolean = false,

    // 3. Lyrics & Tag Storage (Cài đặt lưu trữ lời bài hát & tag)
    val sendLyricsToPoweramp: Boolean = true,
    val saveLyricsAsFile: Boolean = false,
    val embedLyricsInFile: Boolean = true,
    val updateTrackMetadataOnLyricsSave: Boolean = false,
    val mp3TagVersion: Mp3TagVersion = Mp3TagVersion.ID3V23,
    val alsoSaveId3v1: Boolean = true,
    val embedArtworkInFile: Boolean = true,
    val saveArtworkCopyToFolder: Boolean = true,
    val artworkFileName: String = "Cover.jpg",
    val powerampFolders: List<String> = emptyList(),
    val customFolders: List<String> = emptyList(),

    // 4. Lyrics Providers (Nhà cung cấp lời bài hát)
    val lrcLibApiUrls: List<String> = listOf(DEFAULT_LRCLIB_API),
    val activeLrcLibApiUrl: String = DEFAULT_LRCLIB_API,

    // 5. Lyrics Editor (Trình chỉnh sửa lời bài hát)
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.5-flash",
    val openRouterApiKey: String = "",
    val openRouterModel: String = "openrouter/free",
    val timestampStepMs: Long = 100L, // 10 phần trăm giây = 100ms

    // 6. Filters (Bộ lọc - regex supported, case-insensitive)
    val titleFilters: List<String> = emptyList(),
    val artistFilters: List<String> = emptyList(),
    val albumFilters: List<String> = emptyList()
) {
    companion object {
        const val DEFAULT_LRCLIB_API = "https://lrclib.net/api"
    }
}
