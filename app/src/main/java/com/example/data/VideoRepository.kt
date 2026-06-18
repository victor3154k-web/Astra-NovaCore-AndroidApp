package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class VideoRepository(
    private val context: Context,
    private val videoDao: VideoDao
) {
    val allVideos: Flow<List<SavedVideo>> = videoDao.getAllVideos()

    suspend fun getVideoById(id: Int): SavedVideo? = withContext(Dispatchers.IO) {
        videoDao.getVideoById(id)
    }

    suspend fun insertVideo(video: SavedVideo): Long = withContext(Dispatchers.IO) {
        videoDao.insertVideo(video)
    }

    suspend fun updateVideo(video: SavedVideo) = withContext(Dispatchers.IO) {
        videoDao.updateVideo(video)
    }

    suspend fun deleteVideo(video: SavedVideo) = withContext(Dispatchers.IO) {
        // Delete local video file if it is in the internal files directory
        if (video.localUri.startsWith(context.filesDir.absolutePath)) {
            val file = File(video.localUri)
            if (file.exists()) {
                file.delete()
            }
        }
        // Delete custom srt files if they exist locally
        video.srtPath1?.let { path ->
            if (path.startsWith(context.filesDir.absolutePath)) {
                val f = File(path)
                if (f.exists()) f.delete()
            }
        }
        video.srtPath2?.let { path ->
            if (path.startsWith(context.filesDir.absolutePath)) {
                val f = File(path)
                if (f.exists()) f.delete()
            }
        }
        // Also delete thumbnail
        val thumbnailFile = File(context.filesDir, "thumbnails/thumb_${video.id}.jpg")
        if (thumbnailFile.exists()) {
            thumbnailFile.delete()
        }

        videoDao.deleteVideo(video)
    }

    // Copy external URI or remote URL to local offline storage and register in Room database
    suspend fun importVideo(
        title: String,
        uri: Uri,
        folderName: String = "Geral",
        srtUri1: Uri? = null,
        srtLang1: String? = null,
        srtUri2: Uri? = null,
        srtLang2: String? = null
    ): SavedVideo = withContext(Dispatchers.IO) {
        val videosDir = File(context.filesDir, "videos").apply { mkdirs() }
        val srtDir = File(context.filesDir, "subtitles").apply { mkdirs() }

        val safeTitle = title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        val extension = getExtension(uri) ?: "mp4"
        val destFile = File(videosDir, "vid_${System.currentTimeMillis()}.$extension")

        var sizeBytes: Long = 0

        // 1. Copy Video file internally for pure offline availability
        try {
            if (uri.scheme == "content" || uri.scheme == "android.resource" || uri.scheme == "file") {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            sizeBytes += bytesRead
                        }
                    }
                }
            } else if (uri.scheme == "http" || uri.scheme == "https") {
                // Remote online URL -> Download internally
                URL(uri.toString()).openStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            sizeBytes += bytesRead
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error copying video file: $e")
            // Fallback: If copy failed, use original URI directly
            destFile.delete()
        }

        val finalVideoPath = if (destFile.exists()) destFile.absolutePath else uri.toString()
        if (sizeBytes == 0L && destFile.exists()) {
            sizeBytes = destFile.length()
        }

        // 2. Retrieve video duration
        var durationMs: Long = 0
        try {
            val retriever = MediaMetadataRetriever()
            if (finalVideoPath.startsWith("/")) {
                retriever.setDataSource(finalVideoPath)
            } else {
                retriever.setDataSource(context, Uri.parse(finalVideoPath))
            }
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: 0L
            retriever.release()
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error reading video duration: $e")
        }

        // 3. Handle SRT files
        val srtPath1 = srtUri1?.let { copySubtitleFile(it, srtDir, "sub1_${System.currentTimeMillis()}.srt") }
        val srtPath2 = srtUri2?.let { copySubtitleFile(it, srtDir, "sub2_${System.currentTimeMillis()}.srt") }

        val video = SavedVideo(
            title = title,
            durationMs = durationMs,
            sizeBytes = sizeBytes,
            localUri = finalVideoPath,
            folderName = folderName,
            srtPath1 = srtPath1,
            srtLang1 = srtLang1 ?: if (srtPath1 != null) "Idioma 1" else null,
            srtPath2 = srtPath2,
            srtLang2 = srtLang2 ?: if (srtPath2 != null) "Idioma 2" else null
        )

        val idLong = videoDao.insertVideo(video)
        val insertedVideo = video.copy(id = idLong.toInt())

        // 4. Extract and save video thumbnail
        extractThumbnail(finalVideoPath, insertedVideo.id)

        insertedVideo
    }

    private fun copySubtitleFile(uri: Uri, destDir: File, destName: String): String? {
        val destFile = File(destDir, destName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists()) destFile.absolutePath else null
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error copying subtitle file: $e")
            null
        }
    }

    private fun extractThumbnail(videoPath: String, videoId: Int) {
        val thumbsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val destFile = File(thumbsDir, "thumb_$videoId.jpg")
        try {
            val retriever = MediaMetadataRetriever()
            if (videoPath.startsWith("/")) {
                retriever.setDataSource(videoPath)
            } else {
                retriever.setDataSource(context, Uri.parse(videoPath))
            }
            val bitmap = retriever.getFrameAtTime(1000000) // Frame at 1s
            retriever.release()

            if (bitmap != null) {
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, out)
                }
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Could not extract thumbnail: $e")
        }
    }

    private fun getExtension(uri: Uri): String {
        try {
            if (uri.scheme == "content") {
                val mimeType = context.contentResolver.getType(uri)
                if (mimeType != null) {
                    val mimeTypeMap = android.webkit.MimeTypeMap.getSingleton()
                    val ext = mimeTypeMap.getExtensionFromMimeType(mimeType)
                    if (!ext.isNullOrBlank()) {
                        return ext.lowercase()
                    }
                }
            }
            val lastSegment = uri.lastPathSegment
            if (lastSegment != null) {
                val dotIndex = lastSegment.lastIndexOf('.')
                if (dotIndex != -1 && dotIndex < lastSegment.length - 1) {
                    val ext = lastSegment.substring(dotIndex + 1).lowercase()
                    if (ext.matches("[a-zA-Z0-9]+".toRegex())) {
                        return ext
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error getting extension: $e", e)
        }
        return "mp4"
    }
}
