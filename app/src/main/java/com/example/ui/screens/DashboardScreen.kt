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
    val titleLogoType by viewModel.titleLogoType.collectAsState()
    val customLogoPath by viewModel.customLogoPath.collectAsState()

    // UI state
    var showAddDialog by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var showThemesDialog by remember { mutableStateOf(false) }
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
                            LogoImage(
                                logoType = titleLogoType,
                                customLogoPath = customLogoPath,
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
                                            showThemesDialog = true
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
                                    Divider(color = BorderGray, modifier = Modifier.padding(vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = { Text("Definições", color = Color.White, fontSize = 13.sp) },
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = activeAccentColor, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            viewModel.setShowSettingsScreen(true)
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
                    val tabIcons = listOf(
                        Icons.Default.Movie,
                        Icons.Default.Folder,
                        Icons.Default.CloudQueue,
                        Icons.Default.Memory
                    )
                    
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
                containerColor = if (isLedMode) DarkSurface else activeAccentColor,
                contentColor = if (isLedMode) activeAccentColor else Black,
                icon = { 
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Carregar Vídeo Off-line",
                        tint = if (isLedMode) activeAccentColor else Black
                    ) 
                },
                text = { 
                    Text(
                        "Carregar Vídeo", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp,
                        color = if (isLedMode) activeAccentColor else Black
                    ) 
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .then(
                        if (isLedMode) {
                            Modifier.border(1.5.dp, activeAccentColor, RoundedCornerShape(16.dp))
                        } else Modifier
                    )
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
                        var showCreateFolderDialog by remember { mutableStateOf(false) }
                        var newFolderNameInput by remember { mutableStateOf("") }

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top Row with "Pastas" title and "+ Criar Pasta" button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Minhas Categorias",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { showCreateFolderDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isLedMode) Color.Transparent else activeAccentColor,
                                        contentColor = if (isLedMode) activeAccentColor else Black
                                    ),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .then(
                                            if (isLedMode) {
                                                Modifier.border(1.dp, activeAccentColor, RoundedCornerShape(18.dp))
                                            } else Modifier
                                        ),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isLedMode) activeAccentColor else Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Criar Pasta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (groupedVideos.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f)
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
                                        text = "Nenhuma Pasta Registrada",
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Toque em 'Criar Pasta' no topo para começar a categorizar suas mídias de forma organizada.",
                                        color = MediumGray,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 76.dp, start = 16.dp, end = 16.dp),
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
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Folder,
                                                                contentDescription = "Pasta",
                                                                tint = activeAccentColor
                                                            )
                                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    Text(
                                                                        text = folderName,
                                                                        color = Color.White,
                                                                        fontSize = 16.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        maxLines = 1
                                                                    )
                                                                    
                                                                    val customFoldersList by viewModel.customFolders.collectAsState()
                                                                    if (folderVideos.isEmpty() && customFoldersList.contains(folderName)) {
                                                                        IconButton(
                                                                            onClick = { viewModel.deleteFolder(folderName) },
                                                                            modifier = Modifier.size(24.dp)
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Default.Delete,
                                                                                contentDescription = "Excluir pasta",
                                                                                tint = Color.Red.copy(alpha = 0.7f),
                                                                                modifier = Modifier.size(16.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
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
                                                            if (folderVideos.isEmpty()) {
                                                                Text(
                                                                    text = "Nenhum vídeo nesta pasta. Escolha esta pasta ao carregar novos vídeos.",
                                                                    color = MediumGray,
                                                                    fontSize = 12.sp,
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(horizontal = 8.dp, vertical = 12.dp),
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            } else {
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
                        }

                        // Dialog for Creating Custom Folder
                        if (showCreateFolderDialog) {
                            AlertDialog(
                                onDismissRequest = { showCreateFolderDialog = false },
                                title = {
                                    Text("Criar Nova Pasta", color = activeAccentColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Crie uma categoria personalizada para classificar e organizar seus vídeos.",
                                            color = LightGray,
                                            fontSize = 12.sp
                                        )
                                        OutlinedTextField(
                                            value = newFolderNameInput,
                                            onValueChange = { newFolderNameInput = it },
                                            placeholder = { Text("Nome da Pasta", color = Color.Gray) },
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
                                            if (newFolderNameInput.isNotBlank()) {
                                                viewModel.createFolder(newFolderNameInput)
                                                newFolderNameInput = ""
                                                showCreateFolderDialog = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isLedMode) Color.Transparent else activeAccentColor,
                                            contentColor = if (isLedMode) activeAccentColor else Black
                                        ),
                                        modifier = Modifier.then(
                                            if (isLedMode) {
                                                Modifier.border(1.dp, activeAccentColor, RoundedCornerShape(12.dp))
                                            } else Modifier
                                        )
                                    ) {
                                        Text("Criar", fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCreateFolderDialog = false }) {
                                        Text("Cancelar", color = Color.White)
                                    }
                                },
                                containerColor = DarkBackground,
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                    2 -> {
                        // AUTH CLOUD TAB (GitHub Public Repository Video & Playlist Scanner - Upgraded)
                        var repoUrl by remember { mutableStateOf("") }
                        val githubTokenState by viewModel.githubToken.collectAsState()
                        val syncLogState by viewModel.syncLog.collectAsState()
                        val scannedCountState by viewModel.scannedVideosCount.collectAsState()
                        val bookmarkHistory by viewModel.syncedReposList.collectAsState()
                        
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
                            // Header banner card with nice gradient and cloud emblem
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                activeAccentColor.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .border(1.dp, activeAccentColor.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                                    .padding(20.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Glowing emblem box
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(activeAccentColor.copy(alpha = 0.12f))
                                            .border(1.dp, activeAccentColor.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudQueue,
                                            contentDescription = "Nuvem",
                                            tint = activeAccentColor,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(14.dp))
                                    
                                    Text(
                                        text = "AUTH CLOUD MEDIA SYNC",
                                        color = activeAccentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = "Indexador Cósmico de Repositórios",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Text(
                                        text = "Sincronize mídias de repositórios públicos do GitHub ou analise playlists M3U hospedadas de canais remotos diretamente no seu player.",
                                        color = LightGray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(18.dp))
                            
                            // Input fields block
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Repo Address Input
                                    Column {
                                        Text(
                                            text = "Endereço do Repositório GitHub",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        TextField(
                                            value = repoUrl,
                                            onValueChange = { repoUrl = it },
                                            placeholder = { Text("usuario/projeto ou link completo", fontSize = 13.sp, color = MediumGray) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Code,
                                                    contentDescription = null,
                                                    tint = activeAccentColor
                                                )
                                            },
                                            colors = TextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                                focusedIndicatorColor = activeAccentColor,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            singleLine = true
                                        )
                                    }
                                    
                                    // Optional GitHub Token Input
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Token de Acesso Pessoal (Opcional)",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (githubTokenState.isBlank()) "Sem Token" else "Configurado",
                                                color = if (githubTokenState.isBlank()) MediumGray else activeAccentColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        TextField(
                                            value = githubTokenState,
                                            onValueChange = { viewModel.setGithubToken(it) },
                                            placeholder = { Text("ghp_... (evita limites de IP na API)", fontSize = 13.sp, color = MediumGray) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Key,
                                                    contentDescription = null,
                                                    tint = if (githubTokenState.isBlank()) MediumGray else activeAccentColor
                                                )
                                            },
                                            colors = TextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                                                focusedIndicatorColor = activeAccentColor,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            singleLine = true
                                        )
                                    }
                                    
                                    // Action Sync Button
                                    Button(
                                        onClick = {
                                            if (repoUrl.isBlank()) {
                                                statusMessage = "Digite o repositório ou selecione um abaixo para progredir."
                                                isError = true
                                                return@Button
                                            }
                                            statusMessage = null
                                            viewModel.syncGithubRepo(
                                                repoUrl = repoUrl,
                                                onSuccess = { count ->
                                                    statusMessage = "Sincronização completa! $count mídias indexadas e integradas ao Astra."
                                                    isError = false
                                                },
                                                onError = { err ->
                                                    statusMessage = "Sincronização falhou: $err"
                                                    isError = true
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = activeAccentColor,
                                            contentColor = Black
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            tint = Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Iniciar Sincronização Cósmica",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                            
                            // Realtime syncing terminal logs
                            if (isImporting) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, activeAccentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(
                                                    color = activeAccentColor,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "CONEXÃO ATIVA",
                                                    color = activeAccentColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            Text(
                                                text = "Mídias: $scannedCountState",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = syncLogState,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            lineHeight = 15.sp,
                                            maxLines = 3,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            
                            // Status feedback box
                            if (statusMessage != null && !isImporting) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isError) Color(0x20FF1744) else Color(0x2000E676)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (isError) Color.Red.copy(alpha = 0.4f) else Color.Green.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isError) Color.Red else Color.Green,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = statusMessage!!,
                                            color = if (isError) Color(0xFFFFDAD6) else Color(0xFFE3FFDB),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            
                            // Quick Connection Bookmarks history Section
                            if (bookmarkHistory.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "CONEXÕES RECENTES (SALVAS)",
                                    color = activeAccentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.align(Alignment.Start),
                                    letterSpacing = 1.3.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                bookmarkHistory.forEach { bookmarkedRepo ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { repoUrl = bookmarkedRepo }
                                            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = activeAccentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = bookmarkedRepo,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Toque para preencher",
                                                        color = MediumGray,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeSyncedRepoFromHistory(bookmarkedRepo) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Deletar",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // End of Bookmark section
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                    3 -> {
                        // MEMÓRIA TAB (Memory Optimization - Shifted to index 3)
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
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        val customFoldersList by viewModel.customFolders.collectAsState()
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = inputFolder,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Selecione a Pasta", color = Color.Gray, fontSize = 12.sp) },
                                trailingIcon = {
                                    IconButton(onClick = { dropdownExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Expandir Pastas",
                                            tint = activeAccentColor
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = activeAccentColor,
                                    unfocusedBorderColor = BorderGray,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { dropdownExpanded = true }
                            )
                            
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(DarkSurface)
                                    .border(1.dp, BorderGray)
                            ) {
                                customFoldersList.forEach { folderName ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = folderName,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            ) 
                                        },
                                        onClick = {
                                            inputFolder = folderName
                                            dropdownExpanded = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(text = "Defina Origem do Vídeo", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isSelected = selectedVideoUri != null
                            Button(
                                onClick = { videoPickerLauncher.launch("video/*") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLedMode) {
                                        DarkSurface
                                    } else {
                                        if (isSelected) activeAccentColor else DarkSurface
                                    },
                                    contentColor = if (isLedMode) {
                                        if (isSelected) activeAccentColor else Color.White
                                    } else {
                                        if (isSelected) Black else Color.White
                                    }
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (isLedMode) {
                                            Modifier.border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) activeAccentColor else BorderGray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        } else Modifier
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile, 
                                    contentDescription = "Pick",
                                    tint = if (isLedMode && isSelected) activeAccentColor else if (!isLedMode && isSelected) Black else Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSelected) "Galeria Escolhida" else "Abrir Arquivo", 
                                    color = if (isLedMode && isSelected) activeAccentColor else if (!isLedMode && isSelected) Black else Color.White
                                )
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
                            val isSelectedSrt1 = selectedSrtUri1 != null
                            Button(
                                onClick = { srtPicker1Launcher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLedMode) {
                                        DarkSurface
                                    } else {
                                        if (isSelectedSrt1) activeAccentColor else DarkSurface
                                    },
                                    contentColor = if (isLedMode) {
                                        if (isSelectedSrt1) activeAccentColor else Color.White
                                    } else {
                                        if (isSelectedSrt1) Black else Color.White
                                    }
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .then(
                                        if (isLedMode) {
                                            Modifier.border(
                                                width = if (isSelectedSrt1) 1.5.dp else 1.dp,
                                                color = if (isSelectedSrt1) activeAccentColor else BorderGray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        } else Modifier
                                    ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles, 
                                    contentDescription = "Let1",
                                    tint = if (isLedMode && isSelectedSrt1) activeAccentColor else if (!isLedMode && isSelectedSrt1) Black else Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSelectedSrt1) "Legenda 1 ✓" else "SRT 1", 
                                    color = if (isLedMode && isSelectedSrt1) activeAccentColor else if (!isLedMode && isSelectedSrt1) Black else Color.White, 
                                    fontSize = 12.sp
                                )
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
                            val isSelectedSrt2 = selectedSrtUri2 != null
                            Button(
                                onClick = { srtPicker2Launcher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLedMode) {
                                        DarkSurface
                                    } else {
                                        if (isSelectedSrt2) activeAccentColor else DarkSurface
                                    },
                                    contentColor = if (isLedMode) {
                                        if (isSelectedSrt2) activeAccentColor else Color.White
                                    } else {
                                        if (isSelectedSrt2) Black else Color.White
                                    }
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .then(
                                        if (isLedMode) {
                                            Modifier.border(
                                                width = if (isSelectedSrt2) 1.5.dp else 1.dp,
                                                color = if (isSelectedSrt2) activeAccentColor else BorderGray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        } else Modifier
                                    ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles, 
                                    contentDescription = "Let2",
                                    tint = if (isLedMode && isSelectedSrt2) activeAccentColor else if (!isLedMode && isSelectedSrt2) Black else Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSelectedSrt2) "Legenda 2 ✓" else "SRT 2", 
                                    color = if (isLedMode && isSelectedSrt2) activeAccentColor else if (!isLedMode && isSelectedSrt2) Black else Color.White, 
                                    fontSize = 12.sp
                                )
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLedMode) Color.Transparent else activeAccentColor,
                        contentColor = if (isLedMode) activeAccentColor else Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.then(
                        if (isLedMode) {
                            Modifier
                                .border(1.5.dp, activeAccentColor, RoundedCornerShape(12.dp))
                                .ledGlow(
                                    color = activeAccentColor,
                                    borderRadius = 12.dp,
                                    glowRadius = 10.dp,
                                    enabled = true
                                )
                        } else Modifier
                    )
                ) {
                    Text(
                        text = "Salvar Off-line", 
                        color = if (isLedMode) activeAccentColor else Black, 
                        fontWeight = FontWeight.Bold
                    )
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

                    var streamFolderDropdownExpanded by remember { mutableStateOf(false) }
                    val customFoldersList by viewModel.customFolders.collectAsState()
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = streamFolder,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Selecione a Pasta", color = Color.Gray, fontSize = 12.sp) },
                            trailingIcon = {
                                IconButton(onClick = { streamFolderDropdownExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Expandir Pastas",
                                        tint = activeAccentColor
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = activeAccentColor,
                                unfocusedBorderColor = BorderGray,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { streamFolderDropdownExpanded = true }
                        )
                        
                        DropdownMenu(
                            expanded = streamFolderDropdownExpanded,
                            onDismissRequest = { streamFolderDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface)
                                .border(1.dp, BorderGray)
                        ) {
                            customFoldersList.forEach { folderName ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = folderName,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        ) 
                                    },
                                    onClick = {
                                        streamFolder = folderName
                                        streamFolderDropdownExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLedMode) Color.Transparent else activeAccentColor,
                        contentColor = if (isLedMode) activeAccentColor else Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.then(
                        if (isLedMode) {
                            Modifier
                                .border(1.5.dp, activeAccentColor, RoundedCornerShape(12.dp))
                                .ledGlow(
                                    color = activeAccentColor,
                                    borderRadius = 12.dp,
                                    glowRadius = 10.dp,
                                    enabled = true
                                )
                        } else Modifier
                    )
                ) {
                    Text(
                        text = "Salvar Stream", 
                        color = if (isLedMode) activeAccentColor else Black, 
                        fontWeight = FontWeight.Bold
                    )
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

    if (showThemesDialog) {
        val currentThemeState by viewModel.currentTheme.collectAsState()
        val glowRadiusValue by viewModel.ledGlowRadius.collectAsState()
        val isLedPulseEnabled by viewModel.ledPulseEnabled.collectAsState()

        AlertDialog(
            onDismissRequest = { showThemesDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = activeAccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Estilo & Temas Aura",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    // Immersive high-fidelity Preview Card demonstrating the active LED Neon Theme
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .rememberLedGlow(
                                color = activeAccentColor,
                                borderRadius = 16.dp,
                                baseGlowRadius = glowRadiusValue.dp,
                                enabled = isLedMode,
                                pulseEnabled = isLedPulseEnabled
                            )
                            .border(1.5.dp, activeAccentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Simulated Glowing LED bulb element
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(activeAccentColor.copy(alpha = 0.15f))
                                    .rememberLedGlow(
                                        color = activeAccentColor,
                                        borderRadius = 22.dp,
                                        baseGlowRadius = (glowRadiusValue + 4).dp,
                                        enabled = isLedMode,
                                        pulseEnabled = isLedPulseEnabled
                                    )
                                    .border(1.5.dp, activeAccentColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = activeAccentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "AURA GLOW DIGITAL",
                                color = activeAccentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = when (currentThemeState) {
                                    "gold" -> "Luxo Aura Gold"
                                    "led_warm" -> "LED Sunset Neon"
                                    "led_cold" -> "LED Cyber Frio"
                                    "led_multicolor" -> "Multi-Luzes RGB"
                                    else -> "Original"
                                },
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Heading section
                    Text(
                        text = "SELECIONE SEU ESTILO",
                        color = activeAccentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.Start),
                        letterSpacing = 1.3.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Grid or list of options
                    val themesList = listOf(
                        Triple("gold", "Luxo Aura Gold", "Dourado reluzente metálico clássico real"),
                        Triple("led_warm", "LED Neon Quente", "Fusão de rosa e laranja neon base sunset"),
                        Triple("led_cold", "LED Neon Frio", "Eletrônica azul-ciano cibernética futurista"),
                        Triple("led_multicolor", "LED Arco-Íris Multicolor", "Espectro RGB ondulatório dinâmico")
                    )

                    themesList.forEach { (themeKey, themeTitle, themeDesc) ->
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
                                containerColor = if (isSelected) Color.Black.copy(alpha = 0.5f) else DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.setCurrentTheme(themeKey) }
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) borderGlowColor else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Dynamic beautiful color preview circle
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
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
                                        .border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = themeDesc,
                                        color = LightGray,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setCurrentTheme(themeKey) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = borderGlowColor,
                                        unselectedColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }

                    // Configurações do LED Neon
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "EFEITOS E CUSTOMIZAÇÃO",
                        color = activeAccentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.Start),
                        letterSpacing = 1.3.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Pulse Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Pulse Cósmico",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Efeito de respiração das auras",
                                        color = LightGray,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = isLedPulseEnabled,
                                    onCheckedChange = { viewModel.setLedPulseEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = activeAccentColor,
                                        checkedTrackColor = activeAccentColor.copy(alpha = 0.35f),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = BorderGray
                                    )
                                )
                            }

                            Divider(color = BorderGray, modifier = Modifier.padding(vertical = 10.dp))

                            // Brightness/Glow radius slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Aura: ${glowRadiusValue}dp",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when {
                                            glowRadiusValue <= 10 -> "Suave"
                                            glowRadiusValue <= 18 -> "Vibrante"
                                            else -> "Astra Turbo"
                                        },
                                        color = activeAccentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Slider(
                                    value = glowRadiusValue.toFloat(),
                                    onValueChange = { viewModel.setLedGlowRadius(it.toInt()) },
                                    valueRange = 5f..30f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = activeAccentColor,
                                        activeTrackColor = activeAccentColor,
                                        inactiveTrackColor = BorderGray
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showThemesDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLedMode) Color.Transparent else activeAccentColor,
                        contentColor = if (isLedMode) activeAccentColor else Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.then(
                        if (isLedMode) {
                            Modifier
                                .border(1.5.dp, activeAccentColor, RoundedCornerShape(10.dp))
                                .ledGlow(
                                    color = activeAccentColor,
                                    borderRadius = 10.dp,
                                    glowRadius = 8.dp,
                                    enabled = true
                                )
                        } else Modifier
                    )
                ) {
                    Text(
                        text = "Concluído", 
                        color = if (isLedMode) activeAccentColor else Black, 
                        fontWeight = FontWeight.Bold
                    )
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
                        containerColor = if (isLedMode) Color.Transparent else accentColor,
                        contentColor = if (isLedMode) accentColor else Black
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .then(
                            if (isLedMode) {
                                Modifier.border(1.2.dp, accentColor, RoundedCornerShape(6.dp))
                            } else Modifier
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = "Limpar",
                        tint = if (isLedMode) accentColor else Black,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Otimizar",
                        color = if (isLedMode) accentColor else Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
