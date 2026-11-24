package com.lyricsembedder.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MbReleaseSearchResponse(
    val count: Int = 0,
    val releases: List<MbReleaseSummary> = emptyList()
)

enum class MetadataSource(val displayName: String) {
    ALL("Tất cả / All"),
    MUSICBRAINZ("MusicBrainz"),
    ITUNES("iTunes / Apple Music"),
    DEEZER("Deezer")
}

enum class ReleaseFormatFilter {
    ALL,
    DIGITAL,
    CD,
    VINYL,
    OTHER
}

@Serializable
data class MbReleaseSummary(
    val id: String,
    val title: String = "",
    val status: String? = null,
    val country: String? = null,
    val date: String? = null,
    val disambiguation: String? = null,
    @SerialName("release-group")
    val releaseGroup: MbReleaseGroup? = null,
    @SerialName("artist-credit")
    val artistCredit: List<MbArtistCredit> = emptyList(),
    @SerialName("label-info")
    val labelInfo: List<MbLabelInfo> = emptyList(),
    val media: List<MbMedium> = emptyList(),
    @kotlinx.serialization.Transient
    val source: MetadataSource = MetadataSource.MUSICBRAINZ,
    @kotlinx.serialization.Transient
    val artworkPreviewUrl: String? = null
) {
    val primaryArtist: String
        get() = artistCredit.joinToString("") { (it.name ?: it.artist?.name.orEmpty()) + it.joinphrase.orEmpty() }
            .ifBlank { "Unknown Artist" }

    val primaryLabel: String
        get() = labelInfo.firstOrNull()?.label?.name.orEmpty()

    val releaseType: String
        get() {
            val primary = releaseGroup?.primaryType ?: "Album"
            val statusPart = status ?: ""
            return listOf(primary, statusPart).filter { it.isNotBlank() }.joinToString(" ")
        }

    val year: String
        get() = date?.take(4).orEmpty()

    val totalDiscs: Int
        get() = media.size.coerceAtLeast(1)

    val totalTracks: Int
        get() = media.sumOf { it.trackCount }

    val mediaFormatDisplay: String
        get() {
            if (media.isEmpty()) return ""
            val grouped = media.groupingBy { it.format?.ifBlank { "Unknown" } ?: "Unknown" }.eachCount()
            return grouped.entries.joinToString(" + ") { (fmt, count) ->
                if (count > 1) "${count}×$fmt" else fmt
            }
        }

    val formatCategory: ReleaseFormatFilter
        get() {
            val fmt = mediaFormatDisplay.lowercase()
            return when {
                "digital" in fmt -> ReleaseFormatFilter.DIGITAL
                "cd" in fmt || "sacd" in fmt -> ReleaseFormatFilter.CD
                "vinyl" in fmt -> ReleaseFormatFilter.VINYL
                else -> ReleaseFormatFilter.OTHER
            }
        }
}

@Serializable
data class MbReleaseDetail(
    val id: String,
    val title: String = "",
    val status: String? = null,
    val country: String? = null,
    val date: String? = null,
    val disambiguation: String? = null,
    @SerialName("release-group")
    val releaseGroup: MbReleaseGroup? = null,
    @SerialName("artist-credit")
    val artistCredit: List<MbArtistCredit> = emptyList(),
    @SerialName("label-info")
    val labelInfo: List<MbLabelInfo> = emptyList(),
    val media: List<MbMedium> = emptyList(),
    val genres: List<MbGenre> = emptyList(),
    val tags: List<MbTag> = emptyList(),
    @kotlinx.serialization.Transient
    val directArtworkUrl: String? = null
) {
    val primaryArtist: String
        get() = artistCredit.joinToString("") { (it.name ?: it.artist?.name.orEmpty()) + it.joinphrase.orEmpty() }
            .ifBlank { "Unknown Artist" }

    val primaryLabel: String
        get() = labelInfo.firstOrNull()?.label?.name.orEmpty()

    val year: String
        get() = date?.take(4).orEmpty()

    val primaryGenre: String
        get() = genres.maxByOrNull { it.count }?.name
            ?: tags.maxByOrNull { it.count }?.name
            ?: ""
}

@Serializable
data class MbReleaseGroup(
    val id: String = "",
    @SerialName("primary-type")
    val primaryType: String? = null
)

@Serializable
data class MbArtistCredit(
    val name: String? = null,
    val joinphrase: String? = null,
    val artist: MbArtist? = null
)

@Serializable
data class MbArtist(
    val id: String = "",
    val name: String = ""
)

@Serializable
data class MbLabelInfo(
    val label: MbLabel? = null
)

@Serializable
data class MbLabel(
    val id: String = "",
    val name: String = ""
)

@Serializable
data class MbMedium(
    val position: Int = 1,
    val format: String? = null,
    @SerialName("track-count")
    val trackCount: Int = 0,
    val tracks: List<MbTrack> = emptyList()
)

@Serializable
data class MbTrack(
    val id: String = "",
    val number: String = "",
    val position: Int = 1,
    val title: String = "",
    val length: Long? = null,
    @SerialName("artist-credit")
    val artistCredit: List<MbArtistCredit> = emptyList()
) {
    val artistDisplay: String
        get() = artistCredit.joinToString("") { (it.name ?: it.artist?.name.orEmpty()) + it.joinphrase.orEmpty() }

    val formattedDuration: String
        get() {
            val ms = length ?: return "--:--"
            val totalSec = ms / 1000
            return "%d:%02d".format(totalSec / 60, totalSec % 60)
        }
}

@Serializable
data class MbGenre(
    val name: String = "",
    val count: Int = 0
)

@Serializable
data class MbTag(
    val name: String = "",
    val count: Int = 0
)

enum class ArtworkSourceMode {
    REMOTE_SOURCE,
    LOCAL_IMAGE,
    NONE
}

data class AlbumMetadataDraft(
    val releaseId: String = "",
    val releaseGroupId: String = "",
    val artworkUrl: String? = null,
    val localArtworkUriString: String? = null,
    val artworkSourceMode: ArtworkSourceMode = ArtworkSourceMode.REMOTE_SOURCE,
    val applyAlbumArtist: Boolean = true,
    val albumArtist: String = "",
    val applyComposer: Boolean = false,
    val composer: String = "",
    val applyAlbum: Boolean = true,
    val album: String = "",
    val applyPublisher: Boolean = true,
    val publisher: String = "",
    val applyDiscNumber: Boolean = true,
    val discNumber: Int = 1,
    val discTotal: Int = 1,
    val applyYear: Boolean = true,
    val year: String = "",
    val applyGenre: Boolean = true,
    val genre: String = ""
)

data class TrackMappingItem(
    val included: Boolean = true,
    val trackNumber: Int,
    val fetchedArtist: String,
    val fetchedTitle: String,
    val fetchedDurationMs: Long?,
    val localTrack: AudioTrack
) {
    val formattedFetchedDuration: String
        get() {
            val ms = fetchedDurationMs ?: return "--:--"
            val totalSec = ms / 1000
            return "%d:%02d".format(totalSec / 60, totalSec % 60)
        }
}
