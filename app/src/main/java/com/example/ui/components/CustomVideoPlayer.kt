package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
    autoRepeatEnabled: Boolean,
    onPlaybackPositionUpdate: (videoId: Int, positionMs: Long) -> Unit,
    onClosePlayer: () -> Unit,
    onChangeDecoder: (String) -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onChangeSrtOffset1: (Long) -> Unit,
    onChangeSrtOffset2: (Long) -> Unit,
    onChangeSrtTextSize: (Float) -> Unit,
    onChangeSrtFontFamilyName: (String) -> Unit,
    onChangeAutoRepeat: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Read the active dual library backend preference
    val prefs = remember { context.getSharedPreferences("gold_player_settings", android.content.Context.MODE_PRIVATE) }
    val activeLibrary = remember { prefs.getString("dual_library_backend", "vlc") ?: "vlc" }

    // Initialize LibVLC dynamically based on decoderMode and activeLibrary
    val libVlc = remember(decoderMode, activeLibrary) {
        val options = ArrayList<String>()
        options.add("--no-sub-autodetect-file")
        options.add("--extraintf=android_audiotrack")
        // Tune cache sizes for extremely smooth decoding of high bitrate UHD and complicated HEVC files
        options.add("--file-caching=2500")
        options.add("--network-caching=4000")
        options.add("--live-caching=1500")
        
        if (activeLibrary == "ffmpeg") {
            // Prioritize FFmpeg avcodec decoder module
            options.add("--codec=avcodec,all")
            // Ensure async decoding and multi-threading options for FFmpeg
            options.add("--avcodec-threads=4") // Threaded software slice/frame decoding
            options.add("--avcodec-skiploopfilter=4") // Skip loop filter on complex frames for fast, non-blocking rendering
            options.add("--avcodec-fast") // Optimized speed-ups for async rendering
            options.add("--async") // Enable asynchronous audio/video sync
            options.add("--drop-late-frames") // Synchronize and drop late frames asynchronously
            options.add("--clock-synchro=0") // Async clock timing synchronization
        } else {
            if (decoderMode == "HW") {
                // Enable hardware acceleration modules in VLC (MediaCodec)
                options.add("--codec=mediacodec_ndk,mediacodec_jni,all")
            } else {
                // Software decoding only
                options.add("--codec=all")
                // Threaded operations: utilize multi-threaded software decoding for high-complexity codecs
                options.add("--avcodec-threads=4")
                options.add("--avcodec-skiploopfilter=4") // Skip loop filter on high complexity files when decoding slows down
                options.add("--avcodec-fast") // Allow quick-decoding tweaks
                options.add("--drop-late-frames") // Synchronize and drop extremely late video frames if bottle-necks occur
                options.add("--skip-frames") // Prevent system freeze on massive frame jams
            }
        }
        LibVLC(context, options)
    }

    val vlcMediaPlayer = remember(libVlc) {
        MediaPlayer(libVlc)
    }

    // Track state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(video.playbackPositionMs) }
    var duration by remember { mutableStateOf(video.durationMs) }
    var showControls by remember { mutableStateOf(true) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var showThreeDotsMenu by remember { mutableStateOf(false) }
    var audioBoostLevel by remember { mutableStateOf(100) }
    var playbackError by remember { mutableStateOf<PlayerNotification?>(null) }

    // Drag gestures HUD overlay state
    var showGestureHUD by remember { mutableStateOf(false) }
    var gestureHUDType by remember { mutableStateOf("volume") } // "volume" or "brightness"
    var gestureHUDValue by remember { mutableStateOf(100) }
    var brightnessLevel by remember {
        mutableStateOf(
            context.findActivity()?.window?.attributes?.screenBrightness?.let {
                if (it < 0f) 0.5f else it
            } ?: 0.5f
        )
    }

    // Apply Screen Brightness live when it changes
    LaunchedEffect(brightnessLevel) {
        try {
            val activity = context.findActivity()
            activity?.runOnUiThread {
                val layoutParams = activity.window.attributes
                layoutParams.screenBrightness = brightnessLevel.coerceIn(0.01f, 1f)
                activity.window.attributes = layoutParams
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomVideoPlayer", "Erro ao aplicar brilho: ${e.message}")
        }
    }

    // Apply Audio Boost live to VLC when it changes
    LaunchedEffect(audioBoostLevel) {
        try {
            vlcMediaPlayer.volume = audioBoostLevel.coerceIn(0, 300)
        } catch (e: Exception) {
            android.util.Log.e("CustomVideoPlayer", "Erro ao aplicar Audio Boost: ${e.message}")
        }
    }

    // Auto-dismiss the transient self-healing notification after 6 seconds
    LaunchedEffect(playbackError) {
        if (playbackError != null) {
            delay(6000)
            playbackError = null
        }
    }

    // Auto-restore Portrait/Unspecified orientation when leaving player
    DisposableEffect(Unit) {
        onDispose {
            try {
                context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } catch (e: Exception) {}
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

    // Load media item via LibVLC
    LaunchedEffect(video, vlcMediaPlayer) {
        try {
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
            delay(250)
            val totalDuration = if (duration > 0) duration else (if (video.durationMs > 0) video.durationMs else 1L)
            val posToSeek = if (currentPosition > 0) currentPosition else video.playbackPositionMs
            if (posToSeek > 0 && totalDuration > 0) {
                val floatPos = (posToSeek.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                vlcMediaPlayer.position = floatPos
            }
            vlcMediaPlayer.rate = playbackSpeed
            isPlaying = true
        } catch (e: Exception) {
            android.util.Log.e("CustomVideoPlayer", "Erro ao iniciar mídia no player: ${e.message}", e)
        }
    }

    // Apply speed settings
    LaunchedEffect(vlcMediaPlayer, playbackSpeed) {
        try {
            vlcMediaPlayer.rate = playbackSpeed
        } catch (e: Exception) {
            android.util.Log.e("CustomVideoPlayer", "Erro ao alterar velocidade de reprodução: ${e.message}", e)
        }
    }

    val closeAndSaveProgress: () -> Unit = {
        isPlaying = false
        try {
            vlcMediaPlayer.pause()
        } catch (e: Exception) {}
        val isCompleted = duration > 0 && currentPosition >= (duration - 4000)
        val finalPosition = if (isCompleted) 0L else currentPosition
        onPlaybackPositionUpdate(video.id, finalPosition)
        onClosePlayer()
    }

    BackHandler {
        closeAndSaveProgress()
    }

    val currentCloseAndSaveProgress by rememberUpdatedState(closeAndSaveProgress)
    val currentAutoRepeatEnabled by rememberUpdatedState(autoRepeatEnabled)
    val currentOnClosePlayer by rememberUpdatedState(onClosePlayer)

    // Listen for LibVLC events
    DisposableEffect(vlcMediaPlayer) {
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
                        if (currentAutoRepeatEnabled) {
                            coroutineScope.launch {
                                try {
                                    vlcMediaPlayer.stop()
                                    delay(100)
                                    vlcMediaPlayer.play()
                                    isPlaying = true
                                } catch (e: Exception) {}
                            }
                        } else {
                            coroutineScope.launch {
                                currentCloseAndSaveProgress()
                            }
                        }
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
                    MediaPlayer.Event.EncounteredError -> {
                        playbackError = PlayerNotification(
                            title = "ERRO DE DECODIFICAÇÃO",
                            message = "Falha crítica no reprodutor VLC/FFmpeg ao ler este formato.",
                            isSuggestion = false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomVideoPlayer", "Erro ao configurar escuta de eventos do VLC: ${e.message}", e)
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
            } catch (e: Exception) {}
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(maxWidthPx) {
                    var isLeftSide = false
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isLeftSide = offset.x < (maxWidthPx / 2f)
                            showGestureHUD = true
                            gestureHUDType = if (isLeftSide) "brightness" else "volume"
                            if (isLeftSide) {
                                gestureHUDValue = (brightnessLevel * 100).toInt()
                            } else {
                                gestureHUDValue = audioBoostLevel
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                delay(1200)
                                showGestureHUD = false
                            }
                        },
                        onDragCancel = {
                            showGestureHUD = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val sensitivity = 1.5f
                            val delta = -(dragAmount / maxHeightPx) * sensitivity
                            if (isLeftSide) {
                                val newBrightness = (brightnessLevel + delta).coerceIn(0.01f, 1f)
                                brightnessLevel = newBrightness
                                gestureHUDValue = (newBrightness * 100).toInt()
                            } else {
                                val newVol = (audioBoostLevel + (delta * 300f)).coerceIn(0f, 300f)
                                audioBoostLevel = newVol.toInt()
                                gestureHUDValue = newVol.toInt()
                            }
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                }
        ) {
            // Nested content starts here
         // Player View
         key(vlcMediaPlayer) {
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

        // Custom Overlay Controls (Fades in/out smoothly)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(durationMillis = 400)),
            exit = fadeOut(animationSpec = tween(durationMillis = 400))
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
                        onClick = closeAndSaveProgress,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = video.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Backend Engine Badge derived from player context SharedPreferences
                        val badgeText = if (activeLibrary == "vlc") "VLC" else "FFmpeg"
                        val badgeColor = if (activeLibrary == "vlc") Color(0xFFFFA726) else GoldMetallic

                        Box(
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText.uppercase(),
                                color = badgeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

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
                        onClick = {
                            val activity = context.findActivity()
                            if (activity != null) {
                                val currentOrient = activity.requestedOrientation
                                val targetOrient = if (currentOrient == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                                activity.requestedOrientation = targetOrient
                            }
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "Rotacionar tela",
                            tint = GoldMetallic
                        )
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

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showThreeDotsMenu = !showThreeDotsMenu },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Mais opções (Audio Boost)",
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
                                val currentPos = vlcMediaPlayer.time
                                val newPos = (currentPos - 10000).coerceAtLeast(0)
                                vlcMediaPlayer.time = newPos
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
                                    vlcMediaPlayer.pause()
                                    isPlaying = false
                                } else {
                                    vlcMediaPlayer.play()
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
                                val currentPos = vlcMediaPlayer.time
                                val newPos = (currentPos + 10000).coerceAtMost(duration)
                                vlcMediaPlayer.time = newPos
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
                    
                    MetallicSlider(
                        value = sliderValue,
                        onValueChange = {
                            val targetMs = it.toLong()
                            try {
                                if (targetMs < duration) {
                                    if (!vlcMediaPlayer.isPlaying) {
                                        vlcMediaPlayer.play()
                                        isPlaying = true
                                    }
                                    vlcMediaPlayer.time = targetMs
                                } else {
                                    vlcMediaPlayer.time = targetMs
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CustomVideoPlayer", "Erro ao deslizar posição: ${e.message}", e)
                            }
                            currentPosition = targetMs
                        },
                        valueRange = 0f..maxRange,
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

        // Audio Boost three-dots menu overlay
        if (showThreeDotsMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showThreeDotsMenu = false }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .widthIn(max = 350.dp)
                        .border(1.dp, GoldMetallic.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GoldMetallic.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = GoldMetallic,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Amplificador de Áudio",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            
                            IconButton(onClick = { showThreeDotsMenu = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "$audioBoostLevel%",
                            color = GoldMetallic,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )

                        Text(
                            text = when {
                                audioBoostLevel <= 100 -> "Volume Normal (Qualidade Original)"
                                audioBoostLevel <= 150 -> "Ganho Suave (+50% Amplificado)"
                                audioBoostLevel <= 200 -> "Super Volume (+100% Amplificado)"
                                else -> "Astra Turbo Boost (+200% Ultra Amplificado!)"
                            },
                            color = LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Slider(
                            value = audioBoostLevel.toFloat(),
                            onValueChange = { audioBoostLevel = it.toInt() },
                            valueRange = 100f..300f,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldMetallic,
                                activeTrackColor = GoldMetallic,
                                inactiveTrackColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(100, 150, 200, 300).forEach { pct ->
                                val isSelected = audioBoostLevel == pct
                                OutlinedButton(
                                    onClick = { audioBoostLevel = pct },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) GoldMetallic.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (isSelected) GoldMetallic else Color.White
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) GoldMetallic else Color.White.copy(alpha = 0.15f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = if (pct == 100) "Normal" else "${pct}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Nota: O recurso de amplificação via software aumenta os decibéis originais sem distorcer o fluxo de sincronização dos decodificadores nativos.",
                            color = MediumGray,
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Transient Gesture HUD Overlay
        AnimatedVisibility(
            visible = showGestureHUD,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, GoldMetallic.copy(alpha = 0.4f)),
                modifier = Modifier
                    .width(170.dp)
                    .ledGlow(
                        color = GoldMetallic,
                        borderRadius = 20.dp,
                        glowRadius = 15.dp,
                        enabled = true
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val isBrightness = gestureHUDType == "brightness"
                    Icon(
                        imageVector = if (isBrightness) Icons.Default.Brightness5 else {
                            if (gestureHUDValue == 0) Icons.Default.VolumeMute
                            else if (gestureHUDValue < 100) Icons.Default.VolumeDown
                            else Icons.Default.VolumeUp
                        },
                        contentDescription = null,
                        tint = GoldMetallic,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isBrightness) "BRILHO" else "VOLUME",
                        color = LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.3.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBrightness) "$gestureHUDValue%" else {
                            if (gestureHUDValue > 100) "$gestureHUDValue% (BOOST)" else "$gestureHUDValue%"
                        },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        val fraction = if (isBrightness) {
                            (gestureHUDValue / 100f).coerceIn(0f, 1f)
                        } else {
                            (gestureHUDValue / 300f).coerceIn(0f, 1f)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(CircleShape)
                                .background(GoldMetallic)
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

@Composable
fun MetallicSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val rangeStart = valueRange.start
    val rangeEnd = valueRange.endInclusive
    val totalRange = (rangeEnd - rangeStart).coerceAtLeast(1f)
    val progressFraction = ((value - rangeStart) / totalRange).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        
        fun updatePosition(offsetX: Float) {
            val fraction = (offsetX / widthPx).coerceIn(0f, 1f)
            val newValue = rangeStart + (fraction * totalRange)
            onValueChange(newValue)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(widthPx) {
                    detectTapGestures(
                        onPress = { offset ->
                            updatePosition(offset.x)
                        }
                    )
                }
                .pointerInput(widthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onDragEnd = { },
                        onDragCancel = { },
                        onHorizontalDrag = { _, dragAmount ->
                            val currentOffset = progressFraction * widthPx
                            val targetOffset = currentOffset + dragAmount
                            updatePosition(targetOffset)
                        }
                    )
                }
                .padding(vertical = 11.dp) // center the 6dp bar in 28dp height
        ) {
            // Background Track: dark brushed metallic matte gray with premium outline
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF202226))
                    .border(
                        0.5.dp, 
                        Color.White.copy(alpha = 0.05f), 
                        RoundedCornerShape(3.dp)
                    )
            )

            // Active Track: gorgeous horizontal glowing gradient representing premium media
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFC59500), // Shadow gold start
                                Color(0xFFFFD700), // Rich classic gold
                                Color(0xFFFFF7D6), // Specular glow shine
                                Color(0xFFFFD700)  // Tail gold
                            )
                        )
                    )
            )
        }

        // Sleek Modern Knob with subtle glowing LED effect
        val thumbWidth = 14.dp
        val density = androidx.compose.ui.platform.LocalDensity.current
        val thumbWidthPx = with(density) { thumbWidth.toPx() }
        val thumbOffset = (progressFraction * widthPx) - (thumbWidthPx / 2f)
        val maxOffset = widthPx - thumbWidthPx
        val clampedOffsetDp = with(density) { thumbOffset.coerceIn(0f, maxOffset).toDp() }

        Box(
            modifier = Modifier
                .offset(x = clampedOffsetDp)
                .size(thumbWidth)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.5.dp, Color(0xFFFFD700), CircleShape)
                .ledGlow(
                    color = Color(0xFFFFD700),
                    borderRadius = 7.dp,
                    glowRadius = 8.dp,
                    enabled = true
                )
        )
    }
}

// Extension to retrieve Activity safely in Compose
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
