package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedVideo
import com.example.ui.VideoViewModel
import com.example.ui.components.VideoListItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: VideoViewModel,
    onPlayVideo: (SavedVideo) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeAccentColor = rememberDynamicAccentColor(viewModel)
    val currentThemeState by viewModel.currentTheme.collectAsState()
    val isLedMode = remember(currentThemeState) { currentThemeState.startsWith("led_") }

    // ViewModel State variables
    val videos by viewModel.filteredVideosFlow.collectAsState()
    val groupedVideos by viewModel.groupedVideosFlow.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    // UI state
    var showAddDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var activeSearchMode by remember { mutableStateOf(false) }

    // Fade-in animation for title logo icon
    val iconAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        iconAlpha.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 1500)
        )
    }

    // Dialog state - Local File
    var inputTitle by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf("") }
    var inputFolder by remember { mutableStateOf("Meus Vídeos") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedSrtUri1 by remember { mutableStateOf<Uri?>(null) }
    var selectedSrtUri2 by remember { mutableStateOf<Uri?>(null) }
    var srtLanguage1 by remember { mutableStateOf("Inglês") }
    var srtLanguage2 by remember { mutableStateOf("Português") }

    // Dialog state - Raw / HLS Stream
    var streamTitle by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var streamFolder by remember { mutableStateOf("Streams") }

    // Launchers for Picking Files
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            if (inputTitle.isBlank()) {
                inputTitle = uri.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".") ?: "Vídeo Importado"
            }
        }
    }

    val srtPicker1Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedSrtUri1 = uri
    }

    val srtPicker2Launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedSrtUri2 = uri
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(Black)
                    .statusBarsPadding()
            ) {
                // Top Action Bar exactly matching user attached aesthetic
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeSearchMode) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Pesquisar vídeos...", color = MediumGray, fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = activeAccentColor,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = activeAccentColor)
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    activeSearchMode = false
                                    viewModel.setSearchQuery("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Fechar busca", tint = Color.White)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            singleLine = true
                        )
                    } else {
                        // Title with beautiful High-Res Launcher Icon side-by-side
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Image(
                                painter = painterResource(id = com.example.R.drawable.anc_play_icon_1781702460435),
                                contentDescription = "ANC Play Logo",
                                modifier = Modifier
                                    .size(42.dp)
                                    .alpha(iconAlpha.value)
                                    .clip(CircleShape)
                            )
                            Text(
                                text = "Astra NovaCore",
                                color = Color.White,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Grid/List toggle (The dots icon from photo)
                            IconButton(onClick = { viewModel.toggleLayoutFormat() }) {
                                Icon(
                                    imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                                    contentDescription = "Alternar Layout",
                                    tint = Color.White
                                )
                            }

                            // Search trigger icon
                            IconButton(onClick = { activeSearchMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = Color.White
                                )
                            }

                            // Info details dropdown menu
                            var showMoreMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Mais Opções",
                                        tint = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier
                                        .background(DarkSurface)
                                        .border(0.5.dp, activeAccentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Estilo & Temas", color = Color.White, fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = activeAccentColor, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showThemeDialog = true
                                            showMoreMenu = false
                                        }
                                    )
                                    Divider(color = BorderGray, modifier = Modifier.padding(vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = { Text("Stream URL Raw / HLS", color = Color.White, fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = activeAccentColor, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            showUrlDialog = true
                                            showMoreMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Expanded 3 navigation options layout (Vídeos, Pastas, Auth Cloud)
            Column(
                modifier = Modifier
                    .background(Black)
                    .navigationBarsPadding()
            ) {
                Divider(color = BorderGray, thickness = 0.5.dp)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .ledGlow(
                            color = activeAccentColor,
                            borderRadius = 0.dp,
                            glowRadius = 12.dp,
                            enabled = isLedMode
                        ),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabLabels = listOf("Vídeos", "Pastas", "Auth Cloud", "Memória")
                    val tabIcons = listOf(Icons.Default.Movie, Icons.Default.Folder, Icons.Default.CloudQueue, Icons.Default.Memory)
                    
                    tabLabels.forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { viewModel.setSelectedTab(index) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tabIcons[index],
                                contentDescription = label,
                                tint = if (isSelected) activeAccentColor else MediumGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                color = if (isSelected) activeAccentColor else MediumGray,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(if (isSelected) activeAccentColor else Color.Transparent)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Dynamic Floating Action Button that glows based on LED Theme Mode
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = activeAccentColor,
                contentColor = Black,
                icon = { Icon(Icons.Default.Add, contentDescription = "Carregar Vídeo Off-line") },
                text = { Text("Carregar Vídeo", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .ledGlow(
                        color = activeAccentColor,
                        borderRadius = 16.dp,
                        glowRadius = 14.dp,
                        enabled = isLedMode
                    )
            )
        },
        containerColor = Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Black)
        ) {
            // Fluid animated transitions between tabs
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "AbasPrincipais"
            ) { tab ->
                when (tab) {
                    0 -> {
                        // VÍDEOS TAB
                        if (videos.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "Sem Vídeos",
                                    tint = activeAccentColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(76.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Nenhum Conteúdo Carregado",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Adicione vídeos locais da galeria ou streams da internet para assisti-los off-line com legendas simultâneas.",
                                    color = MediumGray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            if (isGridView) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(videos, key = { it.id }) { video ->
                                        VideoListItem(
                                            video = video,
                                            onVideoClick = { onPlayVideo(video) },
                                            onDeleteClick = { viewModel.deleteVideo(video) },
                                            isGridView = true,
                                            tintColor = activeAccentColor
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(videos, key = { it.id }) { video ->
                                        VideoListItem(
                                            video = video,
                                            onVideoClick = { onPlayVideo(video) },
                                            onDeleteClick = { viewModel.deleteVideo(video) },
                                            isGridView = false,
                                            tintColor = activeAccentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // PASTAS TAB (Folders grouping list)
                        if (groupedVideos.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "Sem Pastas",
                                    tint = activeAccentColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(76.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Nenhuma Pasta Criada",
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "As pastas serão listadas aqui de acordo com a categoria que você definir ao carregar seus vídeos.",
                                    color = MediumGray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                groupedVideos.forEach { (folderName, folderVideos) ->
                                    item {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(0.5.dp, activeAccentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        ) {
                                            var expanded by remember { mutableStateOf(false) }
                                            Column {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { expanded = !expanded }
                                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Folder,
                                                            contentDescription = "Pasta",
                                                            tint = activeAccentColor
                                                        )
                                                        Column {
                                                            Text(
                                                                text = folderName,
                                                                color = Color.White,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "${folderVideos.size} ${if (folderVideos.size == 1) "vídeo" else "vídeos"}",
                                                                color = MediumGray,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }
                                                    Icon(
                                                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                        contentDescription = "Expandir",
                                                        tint = Color.White
                                                    )
                                                }
                                                
                                                if (expanded) {
                                                    Column(
                                                        modifier = Modifier
                                                            .padding(horizontal = 12.dp)
                                                            .padding(bottom = 12.dp)
                                                    ) {
                                                        Divider(color = BorderGray, modifier = Modifier.padding(bottom = 6.dp))
                                                        folderVideos.forEach { video ->
                                                            VideoListItem(
                                                                video = video,
                                                                onVideoClick = { onPlayVideo(video) },
                                                                onDeleteClick = { viewModel.deleteVideo(video) },
                                                                isGridView = false,
                                                                tintColor = activeAccentColor
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
                    2 -> {
                        // AUTH CLOUD TAB (GitHub Public Repository Video Scanner)
                        var repoUrl by remember { mutableStateOf("") }
                        var statusMessage by remember { mutableStateOf<String?>(null) }
                        var isError by remember { mutableStateOf(false) }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, activeAccentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Auth Cloud Sync",
                                        tint = activeAccentColor,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Importador Nuvem Github",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Conecte mídias diretamente de um repositório público do GitHub. O player irá ler a estrutura de subpastas de forma automática e importar as mídias compatíveis.",
                                        color = LightGray,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            OutlinedTextField(
                                value = repoUrl,
                                onValueChange = { repoUrl = it },
                                label = { Text("Usuário/Projeto ou Link do GitHub", color = LightGray, fontSize = 12.sp) },
                                placeholder = { Text("ex: google/iosched", color = MediumGray, fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = activeAccentColor,
                                    unfocusedBorderColor = BorderGray,
                                    focusedLabelColor = activeAccentColor,
                                    unfocusedLabelColor = LightGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    if (repoUrl.isBlank()) {
                                        statusMessage = "Erro: Digite a URL ou identificador do repositório."
                                        isError = true
                                        return@Button
                                    }
                                    statusMessage = "Conectando ao GitHub & analisando arquivos de mídias..."
                                    isError = false
                                    viewModel.syncGithubRepo(
                                        repoUrl = repoUrl,
                                        onSuccess = { count ->
                                            statusMessage = "Sincronização concluída! $count novos vídeos importados da nuvem."
                                            isError = false
                                        },
                                        onError = { error ->
                                            statusMessage = "Falha ao sincronizar: $error. Verifique se o repositório é público."
                                            isError = true
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeAccentColor,
                                    contentColor = Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sincronizar Repositório", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            
                            if (statusMessage != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isError) Color(0xFF321417) else Color(0xFF14321B)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, if (isError) Color.Red.copy(0.6f) else Color.Green.copy(0.6f), RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        text = statusMessage!!,
                                        color = if (isError) Color(0xFFFFDAD6) else Color(0xFFE3FFDB),
                                        modifier = Modifier.padding(12.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    3 -> {
                        // MEMÓRIA TAB (Memory Optimization)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header explaining the dynamic memory status
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, activeAccentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = "Memory Optimization",
                                        tint = activeAccentColor,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Gerenciamento do Sistema",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "O Astra NovaCore monitora e gerencia dinamicamente o uso de memória virtual, heap da JVM e caches locais para fornecer uma experiência de reprodução livre de travamentos.",
                                        color = LightGray,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            // Memory Administrator Card
                            MemoryAdministratorCard(viewModel = viewModel, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // Spinner while importing
            if (isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(280.dp)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = activeAccentColor)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Sincronizando...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Indexando conexões de mídias e decodificando estruturas de canais.",
                                color = MediumGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Import Dialog with pristine formatting (Local Files)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Carregar Vídeo Off-line",
                    color = activeAccentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text(
                            text = "Copie seus vídeos locais e legendas SRT para o sistema seguro do player, garantindo acesso instantâneo sem internet.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            label = { Text("Nome do Vídeo", color = Color.Gray, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = activeAccentColor,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = inputFolder,
                            onValueChange = { inputFolder = it },
                            label = { Text("Pasta / Categoria", color = Color.Gray, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = activeAccentColor,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Text(text = "Defina Origem do Vídeo", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { videoPickerLauncher.launch("video/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (selectedVideoUri != null) activeAccentColor else DarkSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Pick")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (selectedVideoUri != null) "Galeria Escolhida" else "Abrir Arquivo", color = if (selectedVideoUri != null) Black else Color.White)
                            }
                        }
                        
                        if (selectedVideoUri != null) {
                            Text(
                                text = "✓ URI: ${selectedVideoUri?.lastPathSegment}",
                                color = activeAccentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { 
                                inputUrl = it
                                if (it.isNotBlank()) selectedVideoUri = Uri.parse(it) 
                            },
                            label = { Text("Link MP4 / M3U8 (Opcional)", color = Color.Gray, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = activeAccentColor,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Divider(color = BorderGray, modifier = Modifier.padding(vertical = 4.dp))
                        Text(text = "Adicionar Dual Legendas SRT (Opcional)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { srtPicker1Launcher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (selectedSrtUri1 != null) activeAccentColor else DarkSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Let1")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (selectedSrtUri1 != null) "Legenda 1 ✓" else "SRT 1", color = if (selectedSrtUri1 != null) Black else Color.White, fontSize = 12.sp)
                            }

                            OutlinedTextField(
                                value = srtLanguage1,
                                onValueChange = { srtLanguage1 = it },
                                label = { Text("Idioma 1", color = Color.Gray, fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = activeAccentColor,
                                    unfocusedBorderColor = BorderGray
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { srtPicker2Launcher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (selectedSrtUri2 != null) activeAccentColor else DarkSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Let2")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (selectedSrtUri2 != null) "Legenda 2 ✓" else "SRT 2", color = if (selectedSrtUri2 != null) Black else Color.White, fontSize = 12.sp)
                            }

                            OutlinedTextField(
                                value = srtLanguage2,
                                onValueChange = { srtLanguage2 = it },
                                label = { Text("Idioma 2", color = Color.Gray, fontSize = 10.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = activeAccentColor,
                                    unfocusedBorderColor = BorderGray
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val videoUri = selectedVideoUri
                        if (videoUri != null && inputTitle.isNotBlank()) {
                            viewModel.importVideoFile(
                                title = inputTitle,
                                videoUri = videoUri,
                                folderName = inputFolder.trim().ifBlank { "Geral" },
                                srtUri1 = selectedSrtUri1,
                                srtLang1 = srtLanguage1,
                                srtUri2 = selectedSrtUri2,
                                srtLang2 = srtLanguage2
                            )
                            showAddDialog = false
                            inputTitle = ""
                            inputUrl = ""
                            inputFolder = "Meus Vídeos"
                            selectedVideoUri = null
                            selectedSrtUri1 = null
                            selectedSrtUri2 = null
                        } else {
                            android.widget.Toast.makeText(context, "Selecione um vídeo e atribua um nome.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)
                ) {
                    Text("Salvar Off-line", color = Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = DarkBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Modal Import Dialog for RAW / HLS URL Streams
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = {
                Text(
                    text = "Stream URL Raw / HLS",
                    color = activeAccentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Insira links diretos de reprodução de fluxo de vídeo (HLS .m3u8, Github Raw, MP4, etc). Eles serão reproduzidos via rede instantaneamente.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    OutlinedTextField(
                        value = streamTitle,
                        onValueChange = { streamTitle = it },
                        label = { Text("Nome da Stream", color = Color.Gray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeAccentColor,
                            unfocusedBorderColor = BorderGray
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("URL Stream (m3u8, raw, mp4)", color = Color.Gray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeAccentColor,
                            unfocusedBorderColor = BorderGray
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = streamFolder,
                        onValueChange = { streamFolder = it },
                        label = { Text("Pasta / Categoria", color = Color.Gray, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeAccentColor,
                            unfocusedBorderColor = BorderGray
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (streamTitle.isNotBlank() && streamUrl.isNotBlank()) {
                            viewModel.registerStreamVideo(
                                title = streamTitle,
                                url = streamUrl.trim(),
                                folderName = streamFolder.trim().ifBlank { "Streams" }
                            )
                            showUrlDialog = false
                            streamTitle = ""
                            streamUrl = ""
                            streamFolder = "Streams"
                        } else {
                            android.widget.Toast.makeText(context, "Preencha o título e a URL da stream", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)
                ) {
                    Text("Salvar Stream", color = Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = DarkBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = activeAccentColor
                    )
                    Text(
                        text = "Estilo & Temas LED",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Escolha o estilo de efeito LED para o seu Astra NovaCore Player:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    val themes = listOf(
                        Triple("gold", "Luxo Aura Gold", "Aura dourada clássica e sofisticada"),
                        Triple("led_warm", "LED Neon Quente", "Cores quentes pulsantes de pôr do sol"),
                        Triple("led_cold", "LED Neon Frio", "Cores frias de neon futurista e cibernético"),
                        Triple("led_multicolor", "LED Arco-Íris Multicolor", "Espectro RGB dinâmico giratório completo")
                    )

                    val currentThemeState by viewModel.currentTheme.collectAsState()

                    themes.forEach { (themeKey, themeTitle, themeDesc) ->
                        val isSelected = currentThemeState == themeKey
                        val borderGlowColor = when (themeKey) {
                            "gold" -> GoldMetallic
                            "led_warm" -> Color(0xFFFF1493)
                            "led_cold" -> Color(0xFF00FFFF)
                            "led_multicolor" -> Color(0xFFFF007F)
                            else -> GoldMetallic
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DarkSurface else Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .ledGlow(
                                    color = borderGlowColor,
                                    borderRadius = 12.dp,
                                    glowRadius = 10.dp,
                                    enabled = isSelected && themeKey.startsWith("led_")
                                )
                                .clickable {
                                    viewModel.setCurrentTheme(themeKey)
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) borderGlowColor else BorderGray,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Dynamic circle indicator matching theme
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (themeKey == "led_multicolor") {
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Magenta)
                                                )
                                            } else {
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    colors = listOf(borderGlowColor, borderGlowColor.copy(alpha = 0.5f))
                                                )
                                            }
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = themeTitle,
                                        color = if (isSelected) borderGlowColor else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = themeDesc,
                                        color = MediumGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showThemeDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor)
                ) {
                    Text("Concluído", color = Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun MemoryAdministratorCard(
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor = rememberDynamicAccentColor(viewModel)
    
    // Memory dynamic stats
    var usedMb by remember { mutableStateOf(0L) }
    var totalMb by remember { mutableStateOf(0L) }
    var freeMb by remember { mutableStateOf(0L) }
    var cacheSizeKb by remember { mutableStateOf(0L) }
    var isOptimized by remember { mutableStateOf(false) }

    // Internal storage remaining stats
    var internalFreeGb by remember { mutableStateOf(0f) }
    var internalTotalGb by remember { mutableStateOf(0f) }

    fun refreshStats() {
        try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.maxMemory()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            
            usedMb = usedMemory / (1024 * 1024)
            totalMb = totalMemory / (1024 * 1024)
            freeMb = totalMb - usedMb
            
            // Analyze remaining internal memory of the device
            val statFs = android.os.StatFs(context.filesDir.absolutePath)
            internalFreeGb = statFs.availableBytes / (1024f * 1024f * 1024f)
            internalTotalGb = statFs.totalBytes / (1024f * 1024f * 1024f)

            var sumBytes = 0L
            listOf(context.cacheDir, File(context.filesDir, "subtitles"), File(context.filesDir, "thumbnails")).forEach { dir ->
                if (dir.exists()) {
                    dir.walkTopDown().forEach { file ->
                        if (file.isFile) sumBytes += file.length()
                    }
                }
            }
            cacheSizeKb = sumBytes / 1024
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Refresh dynamically
    LaunchedEffect(Unit) {
        while (true) {
            refreshStats()
            kotlinx.coroutines.delay(3000)
        }
    }

    val currentThemeState by viewModel.currentTheme.collectAsState()
    val isLedMode = currentThemeState.startsWith("led_")

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .ledGlow(
                color = accentColor,
                borderRadius = 12.dp,
                glowRadius = 14.dp,
                enabled = isLedMode
            )
            .border(0.5.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Gerenciador de Memória",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Ajustador de Memória Dinâmico",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (isOptimized) {
                    Box(
                        modifier = Modifier
                            .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OTIMIZADO",
                            color = accentColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))

            val internalProgress = remember(internalFreeGb, internalTotalGb) {
                if (internalTotalGb > 0f) ((internalTotalGb - internalFreeGb) / internalTotalGb).coerceIn(0f, 1f) else 0f
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Armazenamento Interno Restante",
                    color = LightGray,
                    fontSize = 11.sp
                )
                Text(
                    text = String.format("%.2f GB Livres de %.1f GB", internalFreeGb, internalTotalGb),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            LinearProgressIndicator(
                progress = { internalProgress },
                color = accentColor,
                trackColor = BorderGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))
            
            val progress = remember(usedMb, totalMb) {
                if (totalMb > 0) (usedMb.toFloat() / totalMb.toFloat()).coerceIn(0f, 1f) else 0f
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Heap da JVM Utilizado",
                    color = LightGray,
                    fontSize = 11.sp
                )
                Text(
                    text = "$usedMb MB / $totalMb MB",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                color = accentColor.copy(alpha = 0.7f),
                trackColor = BorderGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Caches Temporários",
                        color = LightGray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (cacheSizeKb > 1024) String.format("%.2f MB", cacheSizeKb / 1024f) else "$cacheSizeKb KB",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = {
                        try {
                            context.cacheDir.deleteRecursively()
                            System.gc()
                            isOptimized = true
                            refreshStats()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Black
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Limpar",
                        tint = Black,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Otimizar",
                        color = Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
