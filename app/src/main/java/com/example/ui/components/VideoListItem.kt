package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.SavedVideo
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldMetallic
import com.example.ui.theme.MediumGray
import java.io.File
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberVideoThumbnail(video: SavedVideo): Bitmap? {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, keys = arrayOf(video.id, video.localUri)) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Try local cache file
                val cachedFile = File(context.filesDir, "thumbnails/thumb_${video.id}.jpg")
                if (cachedFile.exists() && cachedFile.length() > 0) {
                    try {
                        val bmp = BitmapFactory.decodeFile(cachedFile.absolutePath)
                        if (bmp != null) {
                            withContext(Dispatchers.Main) {
                                value = bmp
                            }
                            return@withContext
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("VideoListItem", "Error decoding cached thumbnail: ${e.message}")
                    }
                }

                // 2. Try MediaStore or content URI loading on Android Q+
                if (video.localUri.startsWith("content://")) {
                    val uri = Uri.parse(video.localUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            val bmp = context.contentResolver.loadThumbnail(uri, Size(640, 480), null)
                            if (bmp != null) {
                                // Cache it so next time we load instantly
                                try {
                                    val thumbsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
                                    val destFile = File(thumbsDir, "thumb_${video.id}.jpg")
                                    FileOutputStream(destFile).use { out ->
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                                    }
                                } catch (ce: Exception) {
                                    android.util.Log.e("VideoListItem", "Error compressing/saving loadThumbnail resource: ${ce.message}")
                                }

                                withContext(Dispatchers.Main) {
                                    value = bmp
                                }
                                return@withContext
                            }
                        } catch (e: Exception) {
                            android.util.Log.d("VideoListItem", "loadThumbnail failed: ${e.message}")
                        }
                    }
                }

                // 3. Fallback: MediaMetadataRetriever
                val retriever = MediaMetadataRetriever()
                try {
                    if (video.localUri.startsWith("content://")) {
                        val uri = Uri.parse(video.localUri)
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor)
                        }
                    } else if (video.localUri.startsWith("http://") || video.localUri.startsWith("https://")) {
                        retriever.setDataSource(video.localUri, HashMap<String, String>())
                    } else if (video.localUri.startsWith("rtsp://") || video.localUri.startsWith("rtmp://")) {
                        // RTSP/RTMP doesn't support easy metadata retrieval directly
                    } else {
                        retriever.setDataSource(video.localUri)
                    }
                    
                    var bmp: Bitmap? = null
                    
                    // A. Try getting a pre-scaled frame at first frame / sync frame (O_MR1+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        try {
                            bmp = retriever.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 640, 480)
                        } catch (e: Exception) {
                            android.util.Log.d("VideoListItem", "getScaledFrameAtTime -1 failed: ${e.message}")
                        }
                        if (bmp == null) {
                            try {
                                bmp = retriever.getScaledFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 640, 480)
                            } catch (e: Exception) {
                                android.util.Log.d("VideoListItem", "getScaledFrameAtTime 1s failed: ${e.message}")
                            }
                        }
                    }
                    
                    // B. Fallback to full-resolution frames and resize manually
                    if (bmp == null) {
                        try {
                            bmp = retriever.getFrameAtTime(-1)
                        } catch (e: Exception) {
                            android.util.Log.d("VideoListItem", "getFrameAtTime -1 failed: ${e.message}")
                        }
                    }
                    if (bmp == null) {
                        try {
                            bmp = retriever.getFrameAtTime(1000000)
                        } catch (e: Exception) {
                            android.util.Log.d("VideoListItem", "getFrameAtTime 1s failed: ${e.message}")
                        }
                    }
                    
                    try {
                        retriever.release()
                    } catch (e: Exception) {}

                    if (bmp != null) {
                        // Rescale if it is too massive because we want to safeguard low-end devices from memory exhaustion
                        val finalBmp = if (bmp.width > 960 || bmp.height > 720) {
                            val aspectRatio = bmp.width.toFloat() / bmp.height.toFloat()
                            val targetWidth = 640
                            val targetHeight = (640 / aspectRatio).toInt().coerceAtLeast(1)
                            Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true)
                        } else {
                            bmp
                        }
                        
                        // Save this generated thumbnail to cache file
                        try {
                            val thumbsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
                            val destFile = File(thumbsDir, "thumb_${video.id}.jpg")
                            FileOutputStream(destFile).use { out ->
                                finalBmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                        } catch (ce: Exception) {
                            android.util.Log.e("VideoListItem", "Error saving retriever thumb: ${ce.message}")
                        }

                        withContext(Dispatchers.Main) {
                            value = finalBmp
                        }
                    }
                } catch (e: java.lang.IllegalArgumentException) {
                    android.util.Log.e("VideoListItem", "DataSource selection failed: ${e.message}")
                } catch (e: Exception) {
                    android.util.Log.d("VideoListItem", "MediaMetadataRetriever failed: ${e.message}")
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoListItem", "Error getting video thumbnail: ${e.message}")
            }
        }
    }.value
}

@Composable
fun VideoListItem(
    video: SavedVideo,
    onVideoClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isGridView: Boolean = false,
    tintColor: Color = GoldMetallic
) {
    val context = LocalContext.current
    val thumbnailBitmap = rememberVideoThumbnail(video)

    if (isGridView) {
        // Grid card layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .clickable { onVideoClick() }
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        painter = rememberAsyncImagePainter(thumbnailBitmap),
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Sem Miniatura",
                            tint = tintColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                
                // Duration overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationMs),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = video.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatSize(video.sizeBytes),
                    color = MediumGray,
                    fontSize = 12.sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (video.srtPath1 != null || video.srtPath2 != null) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Legendas",
                            tint = tintColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    } else {
        // List item row layout (exactly matches user image)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onVideoClick() }
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Icon - Enlarge standard thumbnail layout slightly for robust readability
            Box(
                modifier = Modifier
                    .size(width = 125.dp, height = 75.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray.copy(alpha = 0.4f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        painter = rememberAsyncImagePainter(thumbnailBitmap),
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproduzir",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Duration duration on low left corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationMs),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Metadata info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatSize(video.sizeBytes),
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                    
                    if (video.srtPath1 != null || video.srtPath2 != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Subtitles,
                                contentDescription = "Dual Legendas",
                                tint = tintColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "DUAL",
                                color = tintColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = video.folderName,
                            color = MediumGray,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Options / Delete trigger
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remover vídeo",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.size - 1) {
        value /= 1024.0
        unitIndex++
    }
    return String.format("%.2f %s", value, units[unitIndex]).replace(".", ",")
}
