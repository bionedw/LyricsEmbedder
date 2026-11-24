package com.lyricsembedder.app.ui.navigation

object NavRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SETTINGS_THEME = "settings/theme"
    const val SETTINGS_LYRICS_REQUEST = "settings/lyrics_request"
    const val SETTINGS_LYRICS_STORAGE = "settings/lyrics_storage"
    const val SETTINGS_LYRICS_PROVIDER = "settings/lyrics_provider"
    const val SETTINGS_LYRICS_EDITOR = "settings/lyrics_editor"
    const val SETTINGS_FILTERS = "settings/filters"
    const val LYRICS_EDITOR = "lyrics_editor"
}

enum class MainTab(val title: String) {
    METADATA("Gắn Metadata"),
    LYRICS("Thêm Lyrics")
}
