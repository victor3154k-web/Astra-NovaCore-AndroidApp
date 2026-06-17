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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VideoViewModel
import com.example.ui.components.CustomVideoPlayer
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: VideoViewModel = viewModel()
                val activePlayingVideo by viewModel.activePlayingVideo.collectAsState()

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
                            onChangeSrtFontFamilyName = { viewModel.setSrtFontFamilyName(it) }
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
