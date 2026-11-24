package com.lyricsembedder.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MbReleaseSearchResponse(
    val count: Int = 0,
    val releases: List<MbReleaseSummary> = emptyList()
)

@Serializable
data class MbReleaseSummary(
    val id: String,
    val title: String = "",
    val status: String? = null,
    val country: String? = null,
    val date: String? = null,
    @SerialName("release-group")
    val releaseGroup: MbReleaseGroup? = null,
    @SerialName("artist-credit")
    val artistCredit: List<MbArtistCredit> = emptyList(),
    @SerialName("label-info")
    val labelInfo: List<MbLabelInfo> = emptyList(),
    val media: List<MbMedium> = emptyList()
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
}

@Serializable
data class MbReleaseDetail(
    val id: String,
    val title: String = "",
    val status: String? = null,
    val country: String? = null,
    val date: String? = null,
    @SerialName("release-group")
    val releaseGroup: MbReleaseGroup? = null,
    @SerialName("artist-credit")
    val artistCredit: List<MbArtistCredit> = emptyList(),
    @SerialName("label-info")
    val labelInfo: List<MbLabelInfo> = emptyList(),
    val media: List<MbMedium> = emptyList(),
    val genres: List<MbGenre> = emptyList(),
    val tags: List<MbTag> = emptyList()
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

/**
 * Editable album metadata model matching the MusicBee-style preview panel.
 */
data class AlbumMetadataDraft(
    val releaseId: String = "",
    val artworkUrl: String? = null,
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

/**
 * 1-to-1 mapping row between a fetched MusicBrainz track and a local audio file.
 */
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
