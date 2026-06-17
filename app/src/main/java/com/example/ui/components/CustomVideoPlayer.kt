package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.SavedVideo
import com.example.data.SubtitleEntry
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun CustomVideoPlayer(
    video: SavedVideo,
    decoderMode: String,
    playbackSpeed: Float,
    subtitles1: List<SubtitleEntry>,
    subtitles2: List<SubtitleEntry>,
    srtOffset1Ms: Long,
    srtOffset2Ms: Long,
    srtTextSizeSp: Float,
    srtFontFamilyName: String,
    onPlaybackPositionUpdate: (videoId: Int, positionMs: Long) -> Unit,
    onClosePlayer: () -> Unit,
    onChangeDecoder: (String) -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onChangeSrtOffset1: (Long) -> Unit,
    onChangeSrtOffset2: (Long) -> Unit,
    onChangeSrtTextSize: (Float) -> Unit,
    onChangeSrtFontFamilyName: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Create a custom RenderersFactory according to selected decoder mode
    val renderersFactory = remember(decoderMode) {
        DefaultRenderersFactory(context).apply {
            val extensionMode = when (decoderMode) {
                "SW" -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                "HW" -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                else -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON // HW+
            }
            setExtensionRendererMode(extensionMode)
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember(renderersFactory) {
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            playWhenReady = true
        }
    }

    // Load media item
    LaunchedEffect(exoPlayer, video) {
        val uri = video.localUri
        val mediaItem = if (uri.startsWith("/")) {
            MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(uri)))
        } else {
            MediaItem.fromUri(uri)
        }
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.seekTo(video.playbackPositionMs)
        exoPlayer.prepare()
    }

    // Apply speed settings
    LaunchedEffect(exoPlayer, playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Track state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(video.playbackPositionMs) }
    var duration by remember { mutableStateOf(video.durationMs) }
    var showControls by remember { mutableStateOf(true) }
    var showSettingsDrawer by remember { mutableStateOf(false) }

    // Listen for events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Position updates collector
    LaunchedEffect(isPlaying, exoPlayer) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            onPlaybackPositionUpdate(video.id, currentPosition)
            delay(100) // Update position every 100ms
        }
    }

    // Auto-fade controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Subtitle matching
    val activeSub1 = remember(subtitles1, currentPosition, srtOffset1Ms) {
        val targetMs = currentPosition + srtOffset1Ms
        subtitles1.firstOrNull { targetMs in it.startMs..it.endMs }?.text
    }

    val activeSub2 = remember(subtitles2, currentPosition, srtOffset2Ms) {
        val targetMs = currentPosition + srtOffset2Ms
        subtitles2.firstOrNull { targetMs in it.startMs..it.endMs }?.text
    }

    val chosenFontFamily = when (srtFontFamilyName) {
        "Monospace" -> FontFamily.Monospace
        "Serif" -> FontFamily.Serif
        else -> FontFamily.SansSerif
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Player View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Use custom beautiful overlay controls
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Simultaneous Subtitles Layers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Track 1
            if (!activeSub1.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = activeSub1,
                        color = Color.White,
                        fontSize = srtTextSizeSp.sp,
                        fontFamily = chosenFontFamily,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Track 2 (Simultaneous)
            if (!activeSub2.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, GoldMetallic.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = activeSub2,
                        color = BrightGold,
                        fontSize = srtTextSizeSp.sp,
                        fontFamily = chosenFontFamily,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Custom Overlay Controls (Transitions & Fade effects)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                // Top controls bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClosePlayer,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    )

                    // Decoder selection toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("HW", "SW", "HW+").forEach { mode ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (decoderMode == mode) GoldMetallic else Color.Transparent)
                                    .clickable { onChangeDecoder(mode) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (decoderMode == mode) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showSettingsDrawer = true },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustar Legendass / Áudio",
                            tint = GoldMetallic
                        )
                    }
                }

                // Center play controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rebobinar 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play/Pause
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(GoldMetallic, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Avançar 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Seekbar & Timing panel
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )

                        // Playback Speed Toggle Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    val nextSpeed = when (playbackSpeed) {
                                        0.5f -> 1.0f
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 0.5f
                                    }
                                    onChangeSpeed(nextSpeed)
                                }
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Velocidade",
                                tint = GoldMetallic,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = formatTime(duration),
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }

                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = {
                            exoPlayer.seekTo(it.toLong())
                            currentPosition = it.toLong()
                        },
                        valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                        colors = SliderDefaults.colors(
                            activeTrackColor = GoldMetallic,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                            thumbColor = BrightGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Subtitles configuration drawer (Custom overlay slider)
        if (showSettingsDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showSettingsDrawer = false }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .clickable(enabled = false) {} // Prevent click-through
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .navigationBarsPadding()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ajustes de Sincronia e Legendas",
                                color = GoldMetallic,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showSettingsDrawer = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sync Legenda 1 controls
                        Text(
                            text = "Ajuste Sincronia - Legenda 1 (${video.srtLang1 ?: "English"})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onChangeSrtOffset1(-200) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("-200ms", color = Color.White)
                            }
                            Text(
                                text = "${srtOffset1Ms} ms",
                                color = GoldMetallic,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onChangeSrtOffset1(200) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("+200ms", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sync Legenda 2 controls
                        Text(
                            text = "Ajuste Sincronia - Legenda 2 (${video.srtLang2 ?: "Português"})",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onChangeSrtOffset2(-200) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("-200ms", color = Color.White)
                            }
                            Text(
                                text = "${srtOffset2Ms} ms",
                                color = BrightGold,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onChangeSrtOffset2(200) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("+200ms", color = Color.White)
                            }
                        }

                        Divider(color = BorderGray, modifier = Modifier.padding(vertical = 12.dp))

                        // Size subtitle controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tamanho das Legendas (${srtTextSizeSp.toInt()}sp)",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onChangeSrtTextSize(srtTextSizeSp - 2f) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onChangeSrtTextSize(srtTextSizeSp + 2f) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Font Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Fonte das Legendas",
                                color = Color.White,
                                fontSize = 14.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("SansSerif", "Monospace", "Serif").forEach { font ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (srtFontFamilyName == font) GoldMetallic else DarkSurface)
                                            .clickable { onChangeSrtFontFamilyName(font) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = font,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (srtFontFamilyName == font) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
