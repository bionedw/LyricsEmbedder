package com.lyricsembedder.app.data.local

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.lyricsembedder.app.data.model.AudioTrack
import com.lyricsembedder.app.data.model.FolderSource
import com.lyricsembedder.app.data.model.MusicFolder
import com.lyricsembedder.app.data.model.SupportedAudioFormats
import com.lyricsembedder.app.util.TrackOrderHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans SAF folders for taggable audio files only, supporting both top-level album folders
 * and nested subfolders.
 */
class AudioScanner(
    private val context: Context,
    private val tagManager: AudioTagManager
) {
    suspend fun scanFolderSummary(
        folderUriString: String,
        source: FolderSource
    ): MusicFolder? = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(folderUriString) }.getOrNull() ?: return@withContext null
        val doc = DocumentFile.fromTreeUri(context, uri) ?: return@withContext null
        val name = doc.name ?: extractDisplayPath(uri).substringAfterLast('/')
        val audioCount = countAudioFilesRecursive(doc)
        MusicFolder(
            uri = uri,
            displayName = name.ifBlank { "Thư mục nhạc" },
            displayPath = extractDisplayPath(uri),
            source = source,
            trackCount = audioCount
        )
    }

    /**
     * Returns direct subfolders (album folders) inside a root folder, or the folder itself
     * if it directly contains taggable audio files.
     */
    suspend fun listAlbumSubfolders(rootUri: Uri, source: FolderSource): List<MusicFolder> =
        withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()
            val results = mutableListOf<MusicFolder>()
            collectAlbumFolders(root, source, results)
            results.sortedBy { it.displayName.lowercase() }
        }

    suspend fun scanTracksInFolder(folderUri: Uri, recursive: Boolean = false): List<AudioTrack> =
        withContext(Dispatchers.IO) {
            val root = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
            val tracks = mutableListOf<AudioTrack>()
            collectTracks(root, folderUri, recursive, tracks)
            TrackOrderHelper.sortTracksForAlbum(tracks)
        }

    private fun collectAlbumFolders(
        dir: DocumentFile,
        source: FolderSource,
        out: MutableList<MusicFolder>
    ) {
        val children = dir.listFiles()
        val directAudioCount = children.count { it.isFile && SupportedAudioFormats.isSupported(it.name.orEmpty()) }
        if (directAudioCount > 0) {
            out += MusicFolder(
                uri = dir.uri,
                displayName = dir.name ?: extractDisplayPath(dir.uri).substringAfterLast('/'),
                displayPath = extractDisplayPath(dir.uri),
                source = source,
                trackCount = directAudioCount
            )
        }
        children.filter { it.isDirectory }.forEach { child ->
            collectAlbumFolders(child, source, out)
        }
    }

    private suspend fun collectTracks(
        dir: DocumentFile,
        rootUri: Uri,
        recursive: Boolean,
        out: MutableList<AudioTrack>
    ) {
        val children = dir.listFiles()
        val lyricFilesByBase = children
            .filter { it.isFile }
            .mapNotNull { file ->
                val name = file.name ?: return@mapNotNull null
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext == "lrc" || ext == "txt") {
                    name.substringBeforeLast('.').lowercase() to name
                } else null
            }
            .toMap()

        val folderName = dir.name ?: extractDisplayPath(dir.uri).substringAfterLast('/')
        for (file in children) {
            if (file.isFile) {
                val name = file.name ?: continue
                if (!SupportedAudioFormats.isSupported(name)) continue
                val baseLower = name.substringBeforeLast('.').lowercase()
                val extLyrics = lyricFilesByBase[baseLower]
                out += tagManager.readTrack(
                    fileUri = file.uri,
                    folderUri = dir.uri,
                    folderName = folderName,
                    fileName = name,
                    externalLyricsPath = extLyrics
                )
            } else if (recursive && file.isDirectory) {
                collectTracks(file, rootUri, true, out)
            }
        }
    }

    private fun countAudioFilesRecursive(dir: DocumentFile): Int {
        var count = 0
        for (child in dir.listFiles()) {
            if (child.isFile && SupportedAudioFormats.isSupported(child.name.orEmpty())) {
                count++
            } else if (child.isDirectory) {
                count += countAudioFilesRecursive(child)
            }
        }
        return count
    }

    fun extractDisplayPath(uri: Uri): String {
        val lastSegment = Uri.decode(uri.lastPathSegment ?: uri.toString())
        return lastSegment.substringAfter("tree/", lastSegment)
    }
}
