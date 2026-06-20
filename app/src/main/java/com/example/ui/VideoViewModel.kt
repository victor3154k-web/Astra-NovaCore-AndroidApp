package com.example.ui

import android.app.Application
import android.content.Context
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
import okhttp3.OkHttpClient
import okhttp3.Request

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

    // Grouped by directory including custom folders (populated or empty)
    private val prefs = application.getSharedPreferences("anc_play_prefs", Context.MODE_PRIVATE)

    // Custom manually created folders. Default options: "Meus Vídeos", "Geral", "Streams"
    private val _customFolders = MutableStateFlow<Set<String>>(
        prefs.getStringSet("custom_folders_list", null) ?: setOf("Meus Vídeos", "Geral", "Streams")
    )
    val customFolders: StateFlow<Set<String>> = _customFolders.asStateFlow()

    fun createFolder(name: String) {
        if (name.isNotBlank()) {
            val updated = _customFolders.value + name.trim()
            _customFolders.value = updated
            prefs.edit().putStringSet("custom_folders_list", updated).apply()
        }
    }

    fun deleteFolder(name: String) {
        val updated = _customFolders.value - name
        _customFolders.value = updated
        prefs.edit().putStringSet("custom_folders_list", updated).apply()
    }

    val allGroupedFoldersFlow: StateFlow<Map<String, List<SavedVideo>>> = combine(
        filteredVideosFlow,
        _customFolders
    ) { videos, customFold ->
        val result = customFold.associateWith { emptyList<SavedVideo>() }.toMutableMap()
        val grouped = videos.groupBy { it.folderName }
        grouped.forEach { (folderName, list) ->
            result[folderName] = list
        }
        result.toSortedMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // We keep the old name to prevent breaking other files or we can map it
    val groupedVideosFlow: StateFlow<Map<String, List<SavedVideo>>> = allGroupedFoldersFlow

    // Video playback state
    private val _activePlayingVideo = MutableStateFlow<SavedVideo?>(null)
    val activePlayingVideo: StateFlow<SavedVideo?> = _activePlayingVideo.asStateFlow()

    // Decoder configurations: HW, SW (HW+ removed)
    private val _decoderMode = MutableStateFlow("HW") // default hardware
    val decoderMode: StateFlow<String> = _decoderMode.asStateFlow()

    // Playback Speed
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Video Auto Repeat (Default: false)
    private val _videoAutoRepeat = MutableStateFlow(prefs.getBoolean("video_auto_repeat", false))
    val videoAutoRepeat: StateFlow<Boolean> = _videoAutoRepeat.asStateFlow()

    fun setVideoAutoRepeat(enabled: Boolean) {
        _videoAutoRepeat.value = enabled
        prefs.edit().putBoolean("video_auto_repeat", enabled).apply()
    }

    // Title Logo selection state (classic, cyber_cat, cute_alien, fun_gif)
    private val _titleLogoType = MutableStateFlow(prefs.getString("title_logo_type", "classic") ?: "classic")
    val titleLogoType: StateFlow<String> = _titleLogoType.asStateFlow()

    fun setTitleLogoType(type: String) {
        _titleLogoType.value = type
        prefs.edit().putString("title_logo_type", type).apply()
    }

    // Custom Logo File Path (imported local image/gif)
    private val _customLogoPath = MutableStateFlow(prefs.getString("custom_logo_path", null))
    val customLogoPath: StateFlow<String?> = _customLogoPath.asStateFlow()

    fun setCustomLogoPath(path: String?) {
        _customLogoPath.value = path
        if (path != null) {
            prefs.edit().putString("custom_logo_path", path).apply()
        } else {
            prefs.edit().remove("custom_logo_path").apply()
        }
    }

    // CPU Cores selection (Performance Setting)
    private val _cpuCores = MutableStateFlow(
        prefs.getInt("cpu_cores_allocated", Runtime.getRuntime().availableProcessors())
            .coerceIn(1, maxOf(1, Runtime.getRuntime().availableProcessors()))
    )
    val cpuCores: StateFlow<Int> = _cpuCores.asStateFlow()

    fun setCpuCores(cores: Int) {
        val maxCores = maxOf(1, Runtime.getRuntime().availableProcessors())
        val coerced = cores.coerceIn(1, maxCores)
        _cpuCores.value = coerced
        prefs.edit().putInt("cpu_cores_allocated", coerced).apply()
    }

    // Dual Library backend configuration (vlc = MobileVLCKit, ffmpeg = FFmpeg-Kit)
    private val _dualLibrary = MutableStateFlow(prefs.getString("dual_library_backend", "vlc") ?: "vlc")
    val dualLibrary: StateFlow<String> = _dualLibrary.asStateFlow()

    fun setDualLibrary(backend: String) {
        _dualLibrary.value = backend
        prefs.edit().putString("dual_library_backend", backend).apply()
    }

    // Settings Screen visibility state
    private val _showSettingsScreen = MutableStateFlow(false)
    val showSettingsScreen: StateFlow<Boolean> = _showSettingsScreen.asStateFlow()

    fun setShowSettingsScreen(show: Boolean) {
        _showSettingsScreen.value = show
    }

    // Flag to auto-expand Themes Drawer in Settings Screen
    private val _expandThemesInSettings = MutableStateFlow(false)
    val expandThemesInSettings: StateFlow<Boolean> = _expandThemesInSettings.asStateFlow()

    fun setExpandThemesInSettings(expand: Boolean) {
        _expandThemesInSettings.value = expand
    }

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

    // Theme state: "gold", "led_warm", "led_cold"
    private val _currentTheme = MutableStateFlow(prefs.getString("current_theme", "gold") ?: "gold")
    val currentTheme: StateFlow<String> = _currentTheme.asStateFlow()

    fun setCurrentTheme(theme: String) {
        _currentTheme.value = theme
        prefs.edit().putString("current_theme", theme).apply()
    }

    // LED Customization parameters
    private val _ledGlowRadius = MutableStateFlow(prefs.getInt("led_glow_radius", 14))
    val ledGlowRadius: StateFlow<Int> = _ledGlowRadius.asStateFlow()

    fun setLedGlowRadius(radius: Int) {
        val coerced = radius.coerceIn(5, 30)
        _ledGlowRadius.value = coerced
        prefs.edit().putInt("led_glow_radius", coerced).apply()
    }

    private val _ledPulseEnabled = MutableStateFlow(prefs.getBoolean("led_pulse_enabled", true))
    val ledPulseEnabled: StateFlow<Boolean> = _ledPulseEnabled.asStateFlow()

    fun setLedPulseEnabled(enabled: Boolean) {
        _ledPulseEnabled.value = enabled
        prefs.edit().putBoolean("led_pulse_enabled", enabled).apply()
    }

    // Import status tracking
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val httpClient = OkHttpClient.Builder().build()

    // GitHub Personal Access Token to prevent Rate Limit (Auth Cloud)
    private val _githubToken = MutableStateFlow(prefs.getString("github_token", "") ?: "")
    val githubToken: StateFlow<String> = _githubToken.asStateFlow()

    fun setGithubToken(token: String) {
        _githubToken.value = token.trim()
        prefs.edit().putString("github_token", token.trim()).apply()
    }

    // Live Sync progress status & files feed logs
    private val _syncLog = MutableStateFlow("Sincronização inativa.")
    val syncLog: StateFlow<String> = _syncLog.asStateFlow()

    private val _scannedVideosCount = MutableStateFlow(0)
    val scannedVideosCount: StateFlow<Int> = _scannedVideosCount.asStateFlow()

    // Bookmark / Synced History of GitHub Public Repo URLs
    private val _syncedReposList = MutableStateFlow<List<String>>(
        prefs.getString("synced_github_repos", "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    )
    val syncedReposList: StateFlow<List<String>> = _syncedReposList.asStateFlow()

    fun addSyncedRepoToHistory(repo: String) {
        val currentList = _syncedReposList.value.toMutableList()
        val cleaned = repo.trim().removeSuffix("/")
        if (cleaned.isNotBlank()) {
            if (currentList.contains(cleaned)) {
                currentList.remove(cleaned)
            }
            currentList.add(0, cleaned)
            val clamped = currentList.take(5)
            _syncedReposList.value = clamped
            prefs.edit().putString("synced_github_repos", clamped.joinToString(",")).apply()
        }
    }

    fun removeSyncedRepoFromHistory(repo: String) {
        val currentList = _syncedReposList.value.filter { it != repo.trim().removeSuffix("/") }
        _syncedReposList.value = currentList
        prefs.edit().putString("synced_github_repos", currentList.joinToString(",")).apply()
    }

    // Clear previous demo videos on startup to have 0 preset test videos
    init {
        viewModelScope.launch(Dispatchers.IO) {
            val allVideos = dao.getAllVideosDirect()
            allVideos.forEach { v ->
                // Clean any default demo links or preset files
                if (v.localUri.contains("commondatastorage.googleapis.com") || 
                    v.folderName in listOf("Naturais", "Animes", "Jogos", "Ficção", "Astra NovaCore Original")) {
                    dao.deleteVideo(v)
                }
            }
        }
    }

    // Register raw Github stream or HLS streams directly in Room
    fun registerStreamVideo(title: String, url: String, folderName: String = "Streams") {
        viewModelScope.launch(Dispatchers.IO) {
            val video = SavedVideo(
                title = title,
                localUri = url,
                folderName = folderName.trim().ifBlank { "Streams" },
                durationMs = 0L,
                sizeBytes = 0L
            )
            repository.insertVideo(video)
        }
    }

    // Parse a GitHub URL format "user/repo" or full "https://github.com/user/repo"
    private fun parseGithubUrl(url: String): Pair<String, String>? {
        val cleanUrl = url.trim().removeSuffix("/")
        return if (cleanUrl.contains("github.com/")) {
            val parts = cleanUrl.split("github.com/")
            if (parts.size > 1) {
                val path = parts[1]
                val subParts = path.split("/")
                if (subParts.size >= 2) {
                    Pair(subParts[0], subParts[1])
                } else null
            } else null
        } else if (cleanUrl.contains("/")) {
            val parts = cleanUrl.split("/")
            if (parts.size == 2) {
                Pair(parts[0], parts[1])
            } else null
        } else null
    }

    // Parse nested M3U playlist files during repo indexing
    private fun parseM3uPlaylist(url: String, playlistName: String, list: MutableList<SavedVideo>) {
        try {
            val reqBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "Astra-NovaCore-Player")
            val token = _githubToken.value
            if (token.isNotBlank()) {
                reqBuilder.header("Authorization", "token $token")
            }
            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return
                val body = response.body?.string() ?: return
                val lines = body.lines()
                var currentTitle = ""
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("#EXTINF:")) {
                        val commaIdx = trimmed.indexOf(",")
                        currentTitle = if (commaIdx != -1) {
                            trimmed.substring(commaIdx + 1).trim()
                        } else {
                            ""
                        }
                    } else if (trimmed.startsWith("http") && !trimmed.contains(" ")) {
                        val lowerUrl = trimmed.lowercase()
                        if (lowerUrl.contains(".mp4") || lowerUrl.contains(".m3u8") || lowerUrl.contains(".mkv") || lowerUrl.contains(".mov") || lowerUrl.contains(".avi") || lowerUrl.contains(".webm")) {
                            val finalTitle = if (currentTitle.isNotBlank()) currentTitle else trimmed.substringAfterLast("/").substringBeforeLast(".")
                            list.add(
                                SavedVideo(
                                    title = "[M3U] $finalTitle",
                                    localUri = trimmed,
                                    folderName = "Cloud Playlist: $playlistName",
                                    durationMs = 0L,
                                    sizeBytes = 0L
                                )
                            )
                        }
                        currentTitle = ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VideoViewModel", "Erro ao processar playlist M3U: ${e.message}")
        }
    }

    // Recursively scan repository directories with token header support and live logs
    private fun scanDirectoryRecursively(owner: String, repo: String, path: String, list: MutableList<SavedVideo>) {
        val url = if (path.isEmpty()) {
            "https://api.github.com/repos/$owner/$repo/contents"
        } else {
            "https://api.github.com/repos/$owner/$repo/contents/$path"
        }
        
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "Astra-NovaCore-Player")
        
        val token = _githubToken.value
        if (token.isNotBlank()) {
            requestBuilder.header("Authorization", "token $token")
        }
        
        val request = requestBuilder.build()
            
        httpClient.newCall(request).execute().use { response ->
            if (response.code == 403) {
                val limitHeader = response.header("X-RateLimit-Remaining")
                if (limitHeader == "0") {
                    throw Exception("Limite de requisições da API pública do GitHub excedido (60 req/hr). Adicione um Token de Acesso pessoal nas configurações do Auth Cloud para liberar limites corporativos.")
                }
            }
            if (!response.isSuccessful) {
                throw Exception("Servidor respondeu com código ${response.code} ao varrer /$path")
            }
            val body = response.body?.string() ?: return
            val array = org.json.JSONArray(body)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val type = item.optString("type")
                val name = item.optString("name")
                val itemPath = item.optString("path")
                val downloadUrl = item.optString("download_url")
                
                if (type == "dir") {
                    _syncLog.value = "Varrendo pasta: /$itemPath"
                    scanDirectoryRecursively(owner, repo, itemPath, list)
                } else if (type == "file") {
                    val lowerName = name.lowercase()
                    if (lowerName.endsWith(".m3u") || (lowerName.endsWith(".m3u8") && !downloadUrl.isNullOrEmpty() && !downloadUrl.contains("m3u8"))) {
                        _syncLog.value = "Analisando playlist: $name"
                        parseM3uPlaylist(downloadUrl, name.substringBeforeLast("."), list)
                    } else if (isVideoExtension(name)) {
                        _syncLog.value = "Localizado vídeo: $name"
                        _scannedVideosCount.value = _scannedVideosCount.value + 1
                        val folderName = if (path.isEmpty()) "Cloud:$repo" else "Cloud:$repo/${itemPath.substringBeforeLast("/")}"
                        list.add(
                            SavedVideo(
                                title = name.substringBeforeLast("."),
                                localUri = downloadUrl,
                                folderName = folderName,
                                durationMs = 0L,
                                sizeBytes = item.optLong("size", 0L)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun isVideoExtension(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp4") || 
               lower.endsWith(".m3u8") || 
               lower.endsWith(".m3u") || 
               lower.endsWith(".mkv") || 
               lower.endsWith(".mov") || 
               lower.endsWith(".avi") || 
               lower.endsWith(".3gp") || 
               lower.endsWith(".webm")
    }

    // Sincronizar repositório público do Github com logs e estatísticas em tempo real
    fun syncGithubRepo(repoUrl: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = parseGithubUrl(repoUrl)
            if (parsed == null) {
                withContext(Dispatchers.Main) {
                    onError("Formato de URL inválido. Use 'usuario/projeto' ou o link completo do repo.")
                }
                return@launch
            }
            val (owner, repo) = parsed
            _isImporting.value = true
            _scannedVideosCount.value = 0
            _syncLog.value = "Iniciando conexão segura com a API do GitHub..."
            
            try {
                val videosFound = mutableListOf<SavedVideo>()
                scanDirectoryRecursively(owner, repo, "", videosFound)
                
                _syncLog.value = "Gravando ${videosFound.size} mídias encontradas no banco de dados local..."
                var insertedCount = 0
                for (video in videosFound) {
                    val exists = dao.getVideoByUri(video.localUri)
                    if (exists == null) {
                        dao.insertVideo(video)
                        insertedCount++
                    }
                }
                
                // Add to persistent quick connect bookmarks stack
                addSyncedRepoToHistory("$owner/$repo")
                
                _syncLog.value = "Concluído! $insertedCount novas mídias adicionadas com sucesso."
                withContext(Dispatchers.Main) {
                    onSuccess(insertedCount)
                }
            } catch (e: Exception) {
                Log.e("VideoViewModel", "Github sync failed: $e")
                _syncLog.value = "Falha: ${e.message}"
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: e.message ?: "Erro de conexão")
                }
            } finally {
                _isImporting.value = false
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
