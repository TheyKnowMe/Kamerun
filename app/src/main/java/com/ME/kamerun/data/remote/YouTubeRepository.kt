package com.ME.kamerun.data.remote

import android.content.Context
import android.util.Log
import com.ME.kamerun.data.local.SongDao
import com.ME.kamerun.data.local.entities.SongEntity
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "YouTubeRepo"

@Singleton
class YouTubeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
) {
    private val musicDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "Music")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val thumbDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null), "Thumbnails")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    suspend fun importPlaylist(
        playlistUrl: String,
        onProgress: (current: Int, total: Int, songTitle: String) -> Unit = { _, _, _ -> },
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting import: $playlistUrl")

            val infoRequest = YoutubeDLRequest(playlistUrl)
            infoRequest.addOption("--flat-playlist")
            infoRequest.addOption("--dump-json")

            val infoResponse = YoutubeDL.getInstance().execute(infoRequest)
            val lines = infoResponse.out.trim().split("\n").filter { it.isNotBlank() }

            if (lines.isEmpty()) {
                return@withContext Result.failure(Exception("Keine Videos in der Playlist gefunden"))
            }

            val total = lines.size
            var importedCount = 0
            Log.d(TAG, "Found $total videos in playlist")

            for ((index, line) in lines.withIndex()) {
                try {
                    val flatJson = org.json.JSONObject(line)
                    val videoId = flatJson.optString("id", "")
                    val flatTitle = flatJson.optString("title", "Unbekannt")

                    if (videoId.isBlank()) continue

                    val existing = songDao.getSongByVideoId(videoId)
                    if (existing != null) {
                        Log.d(TAG, "Skipping duplicate: $flatTitle")
                        onProgress(index + 1, total, "$flatTitle (übersprungen)")
                        continue
                    }

                    onProgress(index + 1, total, flatTitle)
                    Log.d(TAG, "Processing ${index + 1}/$total: $flatTitle ($videoId)")

                    val videoUrl = "https://www.youtube.com/watch?v=$videoId"

                    // MP3 herunterladen
                    val audioFile = downloadAudio(videoUrl, videoId)

                    // Thumbnail herunterladen
                    val thumbUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
                    val thumbFile = downloadThumbnail(thumbUrl, videoId)

                    // Artist/Title parsen
                    val channelTitle = flatJson.optString("channel",
                        flatJson.optString("uploader", ""))
                    val (artist, cleanTitle) = parseArtistTitle(flatTitle, channelTitle)
                    val duration = flatJson.optLong("duration", 0)

                    Log.d(TAG, "  SAVING: audio=${audioFile?.absolutePath} thumb=${thumbFile?.absolutePath}")

                    songDao.insertSong(
                        SongEntity(
                            youtubeVideoId = videoId,
                            title = cleanTitle,
                            artist = artist,
                            thumbnailUrl = thumbUrl,
                            thumbnailPath = thumbFile?.absolutePath,
                            audioPath = audioFile?.absolutePath,
                            duration = if (duration > 0) duration else null,
                            importedFrom = "YouTube Import",
                        )
                    )
                    importedCount++

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing song at index $index", e)
                    continue
                }
            }

            Log.d(TAG, "Import complete: $importedCount/$total songs")
            Result.success(importedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            Result.failure(e)
        }
    }

    private fun downloadAudio(videoUrl: String, videoId: String): File? {
        return try {
            val outputFile = File(musicDir, "$videoId.mp3")
            if (outputFile.exists() && outputFile.length() > 1000) {
                Log.d(TAG, "  Audio already exists: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
                return outputFile
            }

            Log.d(TAG, "  Downloading audio for $videoId to $musicDir")

            val request = YoutubeDLRequest(videoUrl)
            request.addOption("--extract-audio")
            request.addOption("--audio-format", "mp3")
            request.addOption("--audio-quality", "0")
            request.addOption("--no-check-certificates")
            request.addOption("--no-update")
            request.addOption("--extractor-args", "youtube:player_client=mediaconnect")
            request.addOption("-o", File(musicDir, "$videoId.%(ext)s").absolutePath)

            val response = YoutubeDL.getInstance().execute(request)
            Log.d(TAG, "  yt-dlp stdout: ${response.out.take(200)}")
            Log.d(TAG, "  yt-dlp stderr: ${response.err.take(200)}")

            // Alle Dateien im musicDir listen
            val allFiles = musicDir.listFiles() ?: emptyArray()
            Log.d(TAG, "  Files in musicDir (${allFiles.size}):")
            allFiles.forEach { f ->
                Log.d(TAG, "    ${f.name} (${f.length()} bytes)")
            }

            val mp3File = File(musicDir, "$videoId.mp3")
            if (mp3File.exists() && mp3File.length() > 1000) {
                Log.d(TAG, "  Audio OK: ${mp3File.absolutePath}")
                return mp3File
            }

            // Suche nach Datei mit VideoId-Prefix
            val found = allFiles.find { it.name.startsWith(videoId) && it.length() > 1000 }
            if (found != null) {
                Log.d(TAG, "  Audio found (different ext): ${found.absolutePath}")
                return found
            }

            Log.e(TAG, "  Audio FAILED: no file found for $videoId")
            null
        } catch (e: Exception) {
            Log.e(TAG, "  downloadAudio exception for $videoId", e)
            null
        }
    }

    private fun downloadThumbnail(thumbUrl: String, videoId: String): File? {
        val outputFile = File(thumbDir, "$videoId.jpg")
        if (outputFile.exists() && outputFile.length() > 1000) return outputFile

        val urls = mutableListOf<String>()
        if (thumbUrl.isNotBlank()) urls.add(thumbUrl)
        urls.add("https://img.youtube.com/vi/$videoId/maxresdefault.jpg")
        urls.add("https://img.youtube.com/vi/$videoId/mqdefault.jpg")
        urls.add("https://img.youtube.com/vi/$videoId/hqdefault.jpg")

        for (url in urls) {
            try {
                val bytes = URL(url).readBytes()
                if (bytes.size > 1000) {
                    outputFile.writeBytes(bytes)
                    return outputFile
                }
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    private fun parseArtistTitle(title: String, channelTitle: String): Pair<String, String> {
        val separators = listOf(" - ", " – ", " — ", " | ")
        for (sep in separators) {
            if (title.contains(sep)) {
                val parts = title.split(sep, limit = 2)
                if (parts.size == 2) {
                    return Pair(parts[0].trim(), parts[1].trim())
                }
            }
        }

        val artist = channelTitle
            .replace(" - Topic", "")
            .replace("VEVO", "")
            .trim()

        return Pair(artist.ifBlank { "Unbekannt" }, title.trim())
    }
}
