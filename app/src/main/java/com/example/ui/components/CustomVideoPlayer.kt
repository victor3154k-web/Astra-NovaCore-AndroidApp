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
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import android.view.SurfaceView
import android.view.SurfaceHolder
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

    // Initialize LibVLC for Software (SW) decoding with high-performance args for complicated codecs
    val libVlc = remember {
        val options = ArrayList<String>()
        options.add("--no-sub-autodetect-file")
        options.add("--extraintf=android_audiotrack")
        // Tune cache sizes for extremely smooth decoding of high bitrate UHD and complicated HEVC files
        options.add("--file-caching=2500")
        options.add("--network-caching=4000")
        options.add("--live-caching=1500")
        options.add("--codec=all")
        // Threaded operations: utilize multi-threaded software decoding for high-complexity codecs
        options.add("--avcodec-threads=4")
        options.add("--avcodec-skiploopfilter=4") // Skip loop filter on high complexity files when decoding slows down
        options.add("--avcodec-fast") // Allow quick-decoding tweaks
        options.add("--drop-late-frames") // Synchronize and drop extremely late video frames if bottle-necks occur
        options.add("--skip-frames") // Prevent system freeze on massive frame jams
        LibVLC(context, options)
    }

    val vlcMediaPlayer = remember(libVlc) {
        MediaPlayer(libVlc)
    }

    // Initialize ExoPlayer once with adaptive renderers (hardware prefer, with robust software fallbacks)
    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }
        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build().apply {
                playWhenReady = true
            }
    }

    // Track state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(video.playbackPositionMs) }
    var duration by remember { mutableStateOf(video.durationMs) }
    var showControls by remember { mutableStateOf(true) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<PlayerNotification?>(null) }

    // Auto-dismiss the transient self-healing notification after 6 seconds
    LaunchedEffect(playbackError) {
        if (playbackError != null) {
            delay(6000)
            playbackError = null
        }
    }

    // Run advanced video parsing on video load to pre-detect codec incompatibilities
    LaunchedEffect(video) {
        val uriStr = video.localUri
        var autoSuggested = false
        
        // 1. Probe by extension first (fast check)
        val lower = uriStr.lowercase()
        if (lower.endsWith(".mkv") || lower.endsWith(".avi") || lower.endsWith(".flv") || 
            lower.endsWith(".ts") || lower.endsWith(".wmv") || lower.endsWith(".mov") || 
            lower.endsWith(".webm") || lower.endsWith(".vob") || lower.endsWith(".mpg") || 
            lower.endsWith(".mpeg") || lower.endsWith(".divx") || lower.endsWith(".ogv") ||
            lower.endsWith(".rmvb") || lower.endsWith(".3gp")
        ) {
            if (decoderMode == "HW") {
                playbackError = PlayerNotification(
                    title = "RECOMENDAÇÃO DE REPRODUÇÃO",
                    message = "Vídeo com contêiner complexo (${lower.substringAfterLast('.')}). Recomendamos usar decodificador de Software (SW) para compatibilidade de áudio e legendas.",
                    isSuggestion = true,
                    actionText = "ATIVAR SW",
                    onAction = {
                        onChangeDecoder("SW")
                    }
                )
                autoSuggested = true
            }
        }
        
        // 2. Probe by media track inspection (accurate check)
        if (!autoSuggested && decoderMode == "HW") {
            try {
                val extractor = android.media.MediaExtractor()
                if (uriStr.startsWith("/")) {
                    extractor.setDataSource(uriStr)
                } else {
                    extractor.setDataSource(context, android.net.Uri.parse(uriStr), null)
                }
                val trackCount = extractor.trackCount
                for (i in 0 until trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                    
                    // Check for high complexity codecs or incompatible audio codecs in default HW framework
                    val isComplexMime = mime.contains("video/hevc") || 
                                        mime.contains("video/h265") ||
                                        mime.contains("video/hevc-hdr") || 
                                        mime.contains("video/x-vnd.on2.vp9") || 
                                        mime.contains("video/av03") ||
                                        mime.contains("video/av01") ||
                                        mime.contains("video/divx") ||
                                        mime.contains("video/flv") ||
                                        mime.contains("video/mp2v") ||
                                        mime.contains("video/x-ms-wmv") ||
                                        mime.contains("audio/ac3") || 
                                        mime.contains("audio/eac3") || 
                                        mime.contains("audio/ac4") ||
                                        mime.contains("audio/truehd") ||
                                        mime.contains("audio/heaac") ||
                                        mime.contains("audio/dts") ||
                                        mime.contains("audio/mpeg-L2")
                    
                    // Check for high resolution (> 1080p, like 1440p, 4K) which often lags/freezes on budget HW chips
                    val width = if (format.containsKey(android.media.MediaFormat.KEY_WIDTH)) format.getInteger(android.media.MediaFormat.KEY_WIDTH) else 0
                    val height = if (format.containsKey(android.media.MediaFormat.KEY_HEIGHT)) format.getInteger(android.media.MediaFormat.KEY_HEIGHT) else 0
                    val isHighRes = width > 1920 || height > 1080

                    if (isComplexMime || isHighRes) {
                        val codecName = mime.substringAfter("/").uppercase()
                        val detail = if (isHighRes) "Resolução Ultra HD ($width x $height)" else "Codec $codecName"
                        playbackError = PlayerNotification(
                            title = "OTIMIZAÇÃO DE REPRODUÇÃO",
                            message = "$detail complexo detectado. Recomendamos decodificador de Software (SW) para máxima estabilidade.",
                            isSuggestion = true,
                            actionText = "ATIVAR SW",
                            onAction = {
                                onChangeDecoder("SW")
                            }
                        )
                        autoSuggested = true
                        break
                    }
                }
                extractor.release()
            } catch (e: Exception) {
                android.util.Log.e("CustomVideoPlayer", "Erro ao sondar codecs: $e")
            }
        }
    }

    // Load media item conditionally based on decoder mode
    LaunchedEffect(video, decoderMode, exoPlayer, vlcMediaPlayer) {
        try {
            if (decoderMode == "SW") {
                // Stop and release ExoPlayer
                try {
                    exoPlayer.stop()
                } catch (e: Exception) {}
                
                // Configure LibVLC media
                val file = java.io.File(video.localUri)
                val media = if (file.exists()) {
                    Media(libVlc, file.absolutePath)
                } else {
                    Media(libVlc, android.net.Uri.parse(video.localUri))
                }
                
                vlcMediaPlayer.media = media
                media.release()
                
                vlcMediaPlayer.play()
                
                // Wait slightly for player to initialize before setting position and options
                delay(200)
                if (video.playbackPositionMs > 0 && video.durationMs > 0) {
                    val floatPos = (video.playbackPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
                    vlcMediaPlayer.position = floatPos
                }
                vlcMediaPlayer.rate = playbackSpeed
                isPlaying = true
            } else {
                // Stop and release VLC MediaPlayer
                try {
                    vlcMediaPlayer.stop()
                } catch (e: Exception) {}
                
                val uri = video.localUri
                val mediaItem = if (uri.startsWith("/")) {
                    MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(uri)))
                } else {
                    MediaItem.fromUri(uri)
                }
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.seekTo(video.playbackPositionMs)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                isPlaying = true
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomVideoPlayer", "Erro ao iniciar mídia no player: ${e.message}", e)
        }
    }

    // Apply speed settings
    LaunchedEffect(exoPlayer, vlcMediaPlayer, playbackSpeed, decoderMode) {
        try {
            if (decoderMode == "SW") {
                vlcMediaPlayer.rate = playbackSpeed
            } else {
                exoPlayer.setPlaybackSpeed(playbackSpeed)
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomVideoPlayer", "Erro ao alterar velocidade de reprodução: ${e.message}", e)
        }
    }

    // Listen for ExoPlayer events
    DisposableEffect(exoPlayer, decoderMode) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                if (decoderMode != "SW") {
                    isPlaying = playing
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (decoderMode != "SW" && playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = PlayerNotification(
                    title = "REPRODUÇÃO SEGURA (SW)",
                    message = "Falha no decodificador físico (HW). Alternando automaticamente para decodificador de Software (SW) para continuar...",
                    isSuggestion = false
                )
                if (decoderMode == "HW") {
                    onChangeDecoder("SW")
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            try {
                exoPlayer.removeListener(listener)
            } catch (e: Exception) {}
        }
    }

    // Listen for LibVLC events
    DisposableEffect(vlcMediaPlayer, decoderMode) {
        if (decoderMode == "SW") {
            try {
                vlcMediaPlayer.setEventListener { event ->
                    when (event.type) {
                        MediaPlayer.Event.Playing -> {
                            isPlaying = true
                        }
                        MediaPlayer.Event.Paused -> {
                            isPlaying = false
                        }
                        MediaPlayer.Event.Stopped -> {
                            isPlaying = false
                        }
                        MediaPlayer.Event.EndReached -> {
                            isPlaying = false
                        }
                        MediaPlayer.Event.TimeChanged -> {
                            currentPosition = event.timeChanged
                            onPlaybackPositionUpdate(video.id, event.timeChanged)
                        }
                        MediaPlayer.Event.LengthChanged -> {
                            if (event.lengthChanged > 0) {
                                duration = event.lengthChanged
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CustomVideoPlayer", "Erro ao configurar escuta de eventos do VLC: ${e.message}", e)
            }
        }
        onDispose {
            try {
                vlcMediaPlayer.setEventListener(null)
                vlcMediaPlayer.getVLCVout().detachViews()
            } catch (e: Exception) {}
        }
    }

    // Lifetime cleanup of VLC engines
    DisposableEffect(Unit) {
        onDispose {
            try {
                vlcMediaPlayer.stop()
                vlcMediaPlayer.release()
                libVlc.release()
                exoPlayer.release()
            } catch (e: Exception) {}
        }
    }

    // Position updates collector for ExoPlayer
    LaunchedEffect(isPlaying, exoPlayer, decoderMode) {
        while (isPlaying) {
            try {
                if (decoderMode != "SW") {
                    currentPosition = exoPlayer.currentPosition
                    onPlaybackPositionUpdate(video.id, currentPosition)
                }
            } catch (e: java.lang.Exception) {
                android.util.Log.e("CustomVideoPlayer", "Erro ao solicitar posição de reprodução do ExoPlayer: ${e.message}")
            }
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
        if (decoderMode == "SW") {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                vlcMediaPlayer.getVLCVout().setVideoView(this@apply)
                                vlcMediaPlayer.getVLCVout().attachViews()
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int
                            ) {
                                vlcMediaPlayer.getVLCVout().setWindowSize(width, height)
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                try {
                                    if (vlcMediaPlayer.isPlaying) {
                                        vlcMediaPlayer.pause()
                                    }
                                } catch (e: Exception) {}
                                vlcMediaPlayer.getVLCVout().detachViews()
                            }
                        })
                    }
                },
                onRelease = {
                    try {
                        vlcMediaPlayer.getVLCVout().detachViews()
                    } catch (e: Exception) {}
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
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
                update = { view ->
                    view.player = exoPlayer
                },
                onRelease = { view ->
                    try {
                        view.player = null
                    } catch (e: Exception) {}
                },
                modifier = Modifier.fillMaxSize()
            )
        }

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
                        listOf("HW", "SW").forEach { mode ->
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
                            try {
                                val currentPos = if (decoderMode == "SW") vlcMediaPlayer.time else exoPlayer.currentPosition
                                val newPos = (currentPos - 10000).coerceAtLeast(0)
                                if (decoderMode == "SW") {
                                    vlcMediaPlayer.time = newPos
                                } else {
                                    exoPlayer.seekTo(newPos)
                                }
                                currentPosition = newPos
                            } catch (e: Exception) {
                                android.util.Log.e("CustomVideoPlayer", "Erro ao retroceder: ${e.message}", e)
                            }
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
                            try {
                                if (isPlaying) {
                                    if (decoderMode == "SW") {
                                        vlcMediaPlayer.pause()
                                    } else {
                                        exoPlayer.pause()
                                    }
                                    isPlaying = false
                                } else {
                                    if (decoderMode == "SW") {
                                        vlcMediaPlayer.play()
                                    } else {
                                        exoPlayer.play()
                                    }
                                    isPlaying = true
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CustomVideoPlayer", "Erro ao pausar/reproduzir: ${e.message}", e)
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
                            try {
                                val currentPos = if (decoderMode == "SW") vlcMediaPlayer.time else exoPlayer.currentPosition
                                val newPos = (currentPos + 10000).coerceAtMost(duration)
                                if (decoderMode == "SW") {
                                    vlcMediaPlayer.time = newPos
                                } else {
                                    exoPlayer.seekTo(newPos)
                                }
                                currentPosition = newPos
                            } catch (e: Exception) {
                                android.util.Log.e("CustomVideoPlayer", "Erro ao avançar: ${e.message}", e)
                            }
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

                    val maxRange = duration.toFloat().coerceAtLeast(1f)
                    val sliderValue = currentPosition.toFloat().coerceIn(0f, maxRange)
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            val targetMs = it.toLong()
                            try {
                                if (decoderMode == "SW") {
                                    vlcMediaPlayer.time = targetMs
                                } else {
                                    exoPlayer.seekTo(targetMs)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CustomVideoPlayer", "Erro ao deslizar posição: ${e.message}", e)
                            }
                            currentPosition = targetMs
                        },
                        valueRange = 0f..maxRange,
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

        // Beautiful, floating, transient, non-blocking notification pill
        AnimatedVisibility(
            visible = playbackError != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .padding(horizontal = 24.dp)
        ) {
            playbackError?.let { note ->
                Surface(
                    color = Color.Black.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .border(1.dp, GoldMetallic.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .widthIn(max = 480.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isSuggestion) Icons.Default.Info else Icons.Default.Warning,
                            contentDescription = "Otimizador de Codec",
                            tint = GoldMetallic,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                              )
                              Spacer(modifier = Modifier.height(2.dp))
                              Text(
                                  text = note.message,
                                  color = Color.LightGray,
                                  fontSize = 11.sp,
                                  lineHeight = 15.sp
                              )
                          }
                          
                          if (note.isSuggestion && note.actionText != null && note.onAction != null) {
                              TextButton(
                                  onClick = {
                                      note.onAction.invoke()
                                      playbackError = null
                                  },
                                  colors = ButtonDefaults.textButtonColors(contentColor = GoldMetallic),
                                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                  modifier = Modifier.height(32.dp)
                              ) {
                                  Text(
                                      text = note.actionText,
                                      fontSize = 11.sp,
                                      fontWeight = FontWeight.Bold
                                  )
                              }
                          }

                          IconButton(
                              onClick = { playbackError = null },
                              modifier = Modifier.size(24.dp)
                          ) {
                              Icon(
                                  imageVector = Icons.Default.Close,
                                  contentDescription = "Fechar",
                                  tint = Color.White.copy(alpha = 0.6f),
                                  modifier = Modifier.size(16.dp)
                              )
                          }
                      }
                  }
              }
          }
    }
}

data class PlayerNotification(
    val title: String,
    val message: String,
    val isSuggestion: Boolean = false,
    val actionText: String? = null,
    val onAction: (() -> Unit)? = null
)

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
