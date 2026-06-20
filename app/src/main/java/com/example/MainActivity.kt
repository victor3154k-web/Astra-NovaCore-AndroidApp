package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOutQuad
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VideoViewModel
import com.example.ui.components.CustomVideoPlayer
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DisneySplashScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: VideoViewModel = viewModel()
                val activePlayingVideo by viewModel.activePlayingVideo.collectAsState()
                var showSplash by remember { mutableStateOf(true) }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 800, easing = EaseInOutQuad),
                    label = "splash_transition"
                ) { isSplash ->
                    if (isSplash) {
                        DisneySplashScreen(onSplashFinished = { showSplash = false })
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            if (activePlayingVideo != null) {
                                val video = activePlayingVideo!!
                                
                                // Collect player config variables dynamically
                                val decoderMode by viewModel.decoderMode.collectAsState()
                                val playbackSpeed by viewModel.playbackSpeed.collectAsState()
                                val subtitles1 by viewModel.subtitles1.collectAsState()
                                val subtitles2 by viewModel.subtitles2.collectAsState()
                                val srtOffset1 by viewModel.srtOffset1Ms.collectAsState()
                                val srtOffset2 by viewModel.srtOffset2Ms.collectAsState()
                                val srtTextSize by viewModel.srtTextSizeSp.collectAsState()
                                val srtFontFamilyName by viewModel.srtFontFamilyName.collectAsState()
                                val videoAutoRepeat by viewModel.videoAutoRepeat.collectAsState()

                                CustomVideoPlayer(
                                    video = video,
                                    decoderMode = decoderMode,
                                    playbackSpeed = playbackSpeed,
                                    subtitles1 = subtitles1,
                                    subtitles2 = subtitles2,
                                    srtOffset1Ms = srtOffset1,
                                    srtOffset2Ms = srtOffset2,
                                    srtTextSizeSp = srtTextSize,
                                    srtFontFamilyName = srtFontFamilyName,
                                    autoRepeatEnabled = videoAutoRepeat,
                                    onPlaybackPositionUpdate = { id, pos ->
                                        viewModel.updatePlaybackPosition(id, pos)
                                    },
                                    onClosePlayer = {
                                        viewModel.selectVideoToPlay(null)
                                    },
                                    onChangeDecoder = { viewModel.setDecoderMode(it) },
                                    onChangeSpeed = { viewModel.setPlaybackSpeed(it) },
                                    onChangeSrtOffset1 = { viewModel.changeSrtOffset1(it) },
                                    onChangeSrtOffset2 = { viewModel.changeSrtOffset2(it) },
                                    onChangeSrtTextSize = { viewModel.setSrtTextSize(it) },
                                    onChangeSrtFontFamilyName = { viewModel.setSrtFontFamilyName(it) },
                                    onChangeAutoRepeat = { viewModel.setVideoAutoRepeat(it) }
                                )
                            } else {
                                val showSettingsScreen by viewModel.showSettingsScreen.collectAsState()
                                if (showSettingsScreen) {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onClose = { viewModel.setShowSettingsScreen(false) }
                                    )
                                } else {
                                    DashboardScreen(
                                        viewModel = viewModel,
                                        onPlayVideo = { video ->
                                            viewModel.selectVideoToPlay(video)
                                        }
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
