package com.lyricsembedder.app.data.local

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.lyricsembedder.app.data.model.AppSettings
import com.lyricsembedder.app.data.model.AudioTrack
import com.lyricsembedder.app.data.model.Mp3TagVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagOptionSingleton
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.reference.ID3V2Version
import java.io.File
import java.io.IOException

/**
 * Reads and writes audio metadata, artwork, and lyrics via SAF URIs using JAudioTagger.
 * Uses an app-private temp file copy-and-write-back pattern so it works reliably on
 * Android 5.0 (API 21) through Android 15 (API 35) Scoped Storage.
 */
class AudioTagManager(private val context: Context) {

    suspend fun readTrack(
        fileUri: Uri,
        folderUri: Uri,
        folderName: String,
        fileName: String,
        externalLyricsPath: String? = null
    ): AudioTrack = withContext(Dispatchers.IO) {
        val tempFile = copyUriToTempFile(fileUri, fileName)
        try {
            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tag
            val header = audioFile.audioHeader
            val durationMs = (header?.trackLength?.toLong() ?: 0L) * 1000L

            AudioTrack(
                uri = fileUri,
                folderUri = folderUri,
                folderName = folderName,
                fileName = fileName,
                title = tag?.getFirst(FieldKey.TITLE).orEmpty().trim(),
                artist = tag?.getFirst(FieldKey.ARTIST).orEmpty().trim(),
                album = tag?.getFirst(FieldKey.ALBUM).orEmpty().trim(),
                albumArtist = tag?.getFirst(FieldKey.ALBUM_ARTIST).orEmpty().trim(),
                composer = tag?.getFirst(FieldKey.COMPOSER).orEmpty().trim(),
                publisher = tag?.getFirst(FieldKey.RECORD_LABEL).orEmpty().trim(),
                genre = tag?.getFirst(FieldKey.GENRE).orEmpty().trim(),
                year = tag?.getFirst(FieldKey.YEAR).orEmpty().trim(),
                trackNumber = tag?.getFirst(FieldKey.TRACK)?.toIntOrNull(),
                trackTotal = tag?.getFirst(FieldKey.TRACK_TOTAL)?.toIntOrNull(),
                discNumber = tag?.getFirst(FieldKey.DISC_NO)?.toIntOrNull(),
                discTotal = tag?.getFirst(FieldKey.DISC_TOTAL)?.toIntOrNull(),
                durationMs = durationMs,
                embeddedLyrics = tag?.getFirst(FieldKey.LYRICS).orEmpty().trim(),
                externalLyricsPath = externalLyricsPath,
                hasArtwork = tag?.firstArtwork != null,
                isInstrumental = tag?.getFirst(FieldKey.LYRICS)?.trim() == INSTRUMENTAL_MARKER
            )
        } catch (_: Exception) {
            AudioTrack(
                uri = fileUri,
                folderUri = folderUri,
                folderName = folderName,
                fileName = fileName,
                externalLyricsPath = externalLyricsPath
            )
        } finally {
            tempFile.delete()
        }
    }

    suspend fun writeTrackMetadata(
        track: AudioTrack,
        settings: AppSettings,
        artworkBytes: ByteArray? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            configureTagOptions(settings)
            val tempFile = copyUriToTempFile(track.uri, track.fileName)
            try {
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateAndSetDefault

                setFieldIfNotBlank(tag, FieldKey.TITLE, track.title)
                setFieldIfNotBlank(tag, FieldKey.ARTIST, track.artist)
                setFieldIfNotBlank(tag, FieldKey.ALBUM, track.album)
                setFieldIfNotBlank(tag, FieldKey.ALBUM_ARTIST, track.albumArtist)
                setFieldIfNotBlank(tag, FieldKey.COMPOSER, track.composer)
                setFieldIfNotBlank(tag, FieldKey.RECORD_LABEL, track.publisher)
                setFieldIfNotBlank(tag, FieldKey.GENRE, track.genre)
                setFieldIfNotBlank(tag, FieldKey.YEAR, track.year)
                track.trackNumber?.let { tag.setField(FieldKey.TRACK, it.toString()) }
                track.trackTotal?.let { tag.setField(FieldKey.TRACK_TOTAL, it.toString()) }
                track.discNumber?.let { tag.setField(FieldKey.DISC_NO, it.toString()) }
                track.discTotal?.let { tag.setField(FieldKey.DISC_TOTAL, it.toString()) }

                if (artworkBytes != null && artworkBytes.isNotEmpty() && settings.embedArtworkInFile) {
                    val artTemp = File.createTempFile("cover_", ".jpg", context.cacheDir)
                    try {
                        artTemp.writeBytes(artworkBytes)
                        val artwork = ArtworkFactory.createArtworkFromFile(artTemp)
                        tag.deleteArtworkField()
                        tag.setField(artwork)
                    } finally {
                        artTemp.delete()
                    }
                }

                audioFile.commit()
                copyTempFileBackToUri(tempFile, track.uri)
            } finally {
                tempFile.delete()
            }
        }
    }

    suspend fun embedLyrics(
        track: AudioTrack,
        lyrics: String,
        settings: AppSettings,
        updatedTitle: String? = null,
        updatedArtist: String? = null,
        updatedAlbum: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            configureTagOptions(settings)
            val tempFile = copyUriToTempFile(track.uri, track.fileName)
            try {
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.tagOrCreateAndSetDefault
                tag.setField(FieldKey.LYRICS, lyrics)

                if (settings.updateTrackMetadataOnLyricsSave) {
                    if (!updatedTitle.isNullOrBlank()) tag.setField(FieldKey.TITLE, updatedTitle)
                    if (!updatedArtist.isNullOrBlank()) tag.setField(FieldKey.ARTIST, updatedArtist)
                    if (!updatedAlbum.isNullOrBlank()) tag.setField(FieldKey.ALBUM, updatedAlbum)
                }

                audioFile.commit()
                copyTempFileBackToUri(tempFile, track.uri)
            } finally {
                tempFile.delete()
            }
        }
    }

    /**
     * Saves 1 artwork image file (e.g. Cover.jpg) directly into the album destination folder.
     */
    suspend fun saveArtworkToFolder(
        folderUri: Uri,
        artworkBytes: ByteArray,
        fileName: String = "Cover.jpg"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val folder = DocumentFile.fromTreeUri(context, folderUri)
                ?: throw IOException("Không thể truy cập thư mục đích")
            val targetName = fileName.ifBlank { "Cover.jpg" }
            val existing = folder.findFile(targetName)
            val target = existing ?: folder.createFile("image/jpeg", targetName)
                ?: throw IOException("Không thể tạo tệp ảnh bìa $targetName")

            context.contentResolver.openOutputStream(target.uri, "wt")?.use { out ->
                out.write(artworkBytes)
                out.flush()
            } ?: throw IOException("Không thể ghi tệp ảnh bìa")
        }
    }

    /**
     * Saves external .lrc (for synced lyrics) or .txt (for plain lyrics) in the same folder.
     */
    suspend fun saveLyricsFileToFolder(
        folderUri: Uri,
        audioFileName: String,
        lyrics: String,
        isSynced: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val folder = DocumentFile.fromTreeUri(context, folderUri)
                ?: throw IOException("Không thể truy cập thư mục chứa nhạc")
            val baseName = audioFileName.substringBeforeLast('.')
            val ext = if (isSynced) "lrc" else "txt"
            val targetName = "$baseName.$ext"
            val existing = folder.findFile(targetName)
            val target = existing ?: folder.createFile("text/plain", targetName)
                ?: throw IOException("Không thể tạo tệp lời bài hát $targetName")

            context.contentResolver.openOutputStream(target.uri, "wt")?.use { out ->
                out.write(lyrics.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: throw IOException("Không thể ghi tệp lời bài hát")

            targetName
        }
    }

    private fun configureTagOptions(settings: AppSettings) {
        val options = TagOptionSingleton.getInstance()
        options.setID3V2Version(
            when (settings.mp3TagVersion) {
                Mp3TagVersion.ID3V23 -> ID3V2Version.ID3_V23
                Mp3TagVersion.ID3V24 -> ID3V2Version.ID3_V24
            }
        )
        options.setId3v1Save(settings.alsoSaveId3v1)
        options.setAndroid(true)
    }

    private fun setFieldIfNotBlank(tag: org.jaudiotagger.tag.Tag, key: FieldKey, value: String) {
        if (value.isNotBlank()) {
            tag.setField(key, value)
        }
    }

    private fun copyUriToTempFile(uri: Uri, fileName: String): File {
        val ext = fileName.substringAfterLast('.', "mp3")
        val tempFile = File.createTempFile("tag_", ".$ext", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Không thể đọc tệp âm thanh: $fileName")
        return tempFile
    }

    private fun copyTempFileBackToUri(tempFile: File, targetUri: Uri) {
        context.contentResolver.openOutputStream(targetUri, "wt")?.use { output ->
            tempFile.inputStream().use { input -> input.copyTo(output) }
            output.flush()
        } ?: throw IOException("Không thể ghi đè tệp âm thanh. Hãy cấp quyền ghi thư mục.")
    }

    companion object {
        const val INSTRUMENTAL_MARKER = "[Instrumental]"
    }
}
