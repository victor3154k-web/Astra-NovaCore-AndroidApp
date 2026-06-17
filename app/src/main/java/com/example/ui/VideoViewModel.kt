package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VideoViewModel(application: Application) : AndroidViewModel(application) {
    private val db = VideoDatabase.getDatabase(application)
    val dao = db.videoDao()
    val repository = VideoRepository(application, dao)

    // UI state flows
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0 for Videos, 1 for Pastas
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Loaded videos list
    val videosFlow: StateFlow<List<SavedVideo>> = repository.allVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered videos
    val filteredVideosFlow: StateFlow<List<SavedVideo>> = combine(videosFlow, searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grouped by directory
    val groupedVideosFlow: StateFlow<Map<String, List<SavedVideo>>> = filteredVideosFlow
        .map { list -> list.groupBy { it.folderName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Video playback state
    private val _activePlayingVideo = MutableStateFlow<SavedVideo?>(null)
    val activePlayingVideo: StateFlow<SavedVideo?> = _activePlayingVideo.asStateFlow()

    // Decoder configurations: HW, SW, HW+
    private val _decoderMode = MutableStateFlow("HW+") // default premium
    val decoderMode: StateFlow<String> = _decoderMode.asStateFlow()

    // Playback Speed
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Interactive Dual Subtitles
    private val _subtitles1 = MutableStateFlow<List<SubtitleEntry>>(emptyList())
    val subtitles1: StateFlow<List<SubtitleEntry>> = _subtitles1.asStateFlow()

    private val _subtitles2 = MutableStateFlow<List<SubtitleEntry>>(emptyList())
    val subtitles2: StateFlow<List<SubtitleEntry>> = _subtitles2.asStateFlow()

    // Offsets/Sincronização manual in milliseconds
    private val _srtOffset1Ms = MutableStateFlow(0L)
    val srtOffset1Ms: StateFlow<Long> = _srtOffset1Ms.asStateFlow()

    private val _srtOffset2Ms = MutableStateFlow(0L)
    val srtOffset2Ms: StateFlow<Long> = _srtOffset2Ms.asStateFlow()

    // Subtitle Customizations
    private val _srtTextSizeSp = MutableStateFlow(20f)
    val srtTextSizeSp: StateFlow<Float> = _srtTextSizeSp.asStateFlow()

    private val _srtFontFamilyName = MutableStateFlow("SansSerif") // SansSerif, Monospace, Serif
    val srtFontFamilyName: StateFlow<String> = _srtFontFamilyName.asStateFlow()

    // Import status tracking
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Init with some demo offline presets for a gorgeous presentation!
    init {
        viewModelScope.launch {
            videosFlow.collect { list ->
                if (list.isEmpty()) {
                    createDefaultDemoVideoPresets()
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun toggleLayoutFormat() {
        _isGridView.value = !_isGridView.value
    }

    fun setDecoderMode(mode: String) {
        _decoderMode.value = mode
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun changeSrtOffset1(offsetDelta: Long) {
        _srtOffset1Ms.value += offsetDelta
    }

    fun changeSrtOffset2(offsetDelta: Long) {
        _srtOffset2Ms.value += offsetDelta
    }

    fun setSrtTextSize(size: Float) {
        _srtTextSizeSp.value = size.coerceIn(12f, 40f)
    }

    fun setSrtFontFamilyName(name: String) {
        _srtFontFamilyName.value = name
    }

    fun selectVideoToPlay(video: SavedVideo?) {
        _activePlayingVideo.value = video
        if (video != null) {
            // Load and parse associated subtitles
            loadSubtitlesForVideo(video)
        } else {
            _subtitles1.value = emptyList()
            _subtitles2.value = emptyList()
            _srtOffset1Ms.value = 0L
            _srtOffset2Ms.value = 0L
        }
    }

    fun updatePlaybackPosition(videoId: Int, positionMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val video = repository.getVideoById(videoId)
            if (video != null) {
                repository.updateVideo(video.copy(playbackPositionMs = positionMs))
            }
        }
    }

    private fun loadSubtitlesForVideo(video: SavedVideo) {
        viewModelScope.launch(Dispatchers.IO) {
            _subtitles1.value = video.srtPath1?.let { SrtParser.parse(File(it)) } ?: emptyList()
            _subtitles2.value = video.srtPath2?.let { SrtParser.parse(File(it)) } ?: emptyList()
        }
    }

    fun importVideoFile(
        title: String,
        videoUri: Uri,
        folderName: String = "Geral",
        srtUri1: Uri? = null,
        srtLang1: String? = null,
        srtUri2: Uri? = null,
        srtLang2: String? = null
    ) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                repository.importVideo(
                    title = title,
                    uri = videoUri,
                    folderName = folderName,
                    srtUri1 = srtUri1,
                    srtLang1 = srtLang1,
                    srtUri2 = srtUri2,
                    srtLang2 = srtLang2
                )
            } catch (e: Exception) {
                Log.e("VideoViewModel", "Import video error: $e")
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun deleteVideo(video: SavedVideo) {
        viewModelScope.launch {
            if (_activePlayingVideo.value?.id == video.id) {
                selectVideoToPlay(null)
            }
            repository.deleteVideo(video)
        }
    }

    // Creates beautiful mock/sample cinematic content inside the app for first launch
    private suspend fun createDefaultDemoVideoPresets() = withContext(Dispatchers.IO) {
        // Create demo files so they are playable immediately offline
        val sub1Dir = File(getApplication<Application>().filesDir, "subtitles").apply { mkdirs() }
        
        // Let's write SRT files physically
        val srtFile1En = File(sub1Dir, "demo_en.srt")
        srtFile1En.writeText("""
1
00:00:01,000 --> 00:00:04,500
<b>[Astra NovaCore]</b> Welcome to gold visual luxury.
        
2
00:00:04,800 --> 00:00:08,200
This is the primary subtitle track (English).
        
3
00:00:08,500 --> 00:00:12,200
Adjust synchronization and sizes seamlessly.
        
4
00:00:12,500 --> 00:00:18,000
Enjoy minimalist interface & peak performance!
        """.trimIndent())

        val srtFile2Pt = File(sub1Dir, "demo_pt.srt")
        srtFile2Pt.writeText("""
1
00:00:01,000 --> 00:00:04,500
<b>[Astra NovaCore]</b> Bem-vindo ao luxo visual dourado.
        
2
00:00:04,800 --> 00:00:08,200
Esta é a legenda secundária simultânea (Português).
        
3
00:00:08,500 --> 00:00:12,200
Ajuste sincronia e tamanho simultaneamente!
        
4
00:00:12,500 --> 00:00:18,000
Desfrute de interface minimalista e alta performance!
        """.trimIndent())

        // Create elegant demo entries
        // Since we may not have massive local video copies initially, we pre-populate
        // with web URIs. But also write a compact dummy local MP4 if needed, or play
        // these gorgeous cinematic samples.
        val sampleVideos = listOf(
            SavedVideo(
                title = "Golden Digital Awakenings (Nature Core)",
                durationMs = 29000,
                sizeBytes = 3355443, // 3.20 MB
                localUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                folderName = "Naturais",
                srtPath1 = srtFile1En.absolutePath,
                srtLang1 = "English SRT",
                srtPath2 = srtFile2Pt.absolutePath,
                srtLang2 = "Português SRT"
            ),
            SavedVideo(
                title = "Hoshimi Miyabi Zenless Zone Zero (Live Wallpaper)",
                durationMs = 8000,
                sizeBytes = 7046430, // 6.72 MB
                localUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                folderName = "Animes",
                srtPath1 = srtFile1En.absolutePath,
                srtLang1 = "English",
                srtPath2 = srtFile2Pt.absolutePath,
                srtLang2 = "Português Dual"
            ),
            SavedVideo(
                title = "Apex Esports Ascent Cinematic",
                durationMs = 30000,
                sizeBytes = 3009310, // 2.87 MB
                localUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                folderName = "Jogos",
                srtPath1 = null,
                srtPath2 = null
            ),
            SavedVideo(
                title = "The Golden Core - 4K Ultra Render",
                durationMs = 59000,
                sizeBytes = 4687130, // 4.47 MB
                localUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                folderName = "Astra NovaCore Original",
                srtPath1 = srtFile1En.absolutePath,
                srtLang1 = "English Main",
                srtPath2 = srtFile2Pt.absolutePath,
                srtLang2 = "Português Secundária"
            ),
            SavedVideo(
                title = "Cosmic Ascent Trailer",
                durationMs = 44000,
                sizeBytes = 4183818, // 3.99 MB
                localUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                folderName = "Ficção",
                srtPath1 = null,
                srtPath2 = null
            )
        )

        for (v in sampleVideos) {
            repository.insertVideo(v)
            Log.d("VideoViewModel", "Inserted demo video preset: ${v.title}")
        }
    }
}
