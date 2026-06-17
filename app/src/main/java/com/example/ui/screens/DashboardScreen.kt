package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedVideo
import com.example.ui.VideoViewModel
import com.example.ui.components.VideoListItem
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: VideoViewModel,
    onPlayVideo: (SavedVideo) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel State variables
    val videos by viewModel.filteredVideosFlow.collectAsState()
    val groupedVideos by viewModel.groupedVideosFlow.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    // UI state
    var showAddDialog by remember { mutableStateOf(false) }
    var activeSearchMode by remember { mutableStateOf(false) }

    // Dialog state
    var inputTitle by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf("") }
    var inputFolder by remember { mutableStateOf("Meus Vídeos") }

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedSrtUri1 by remember { mutableStateOf<Uri?>(null) }
    var selectedSrtUri2 by remember { mutableStateOf<Uri?>(null) }
    var srtLanguage1 by remember { mutableStateOf("Inglês") }
    var srtLanguage2 by remember { mutableStateOf("Português") }

    // Launchers for Picking Files
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            // auto set a title if empty from uri path
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
                                focusedIndicatorColor = GoldMetallic,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = GoldMetallic)
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
                        Text(
                            text = "Astra NovaCore",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
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

                            // Info details
                            IconButton(onClick = {
                                // Simple elegant dialog about decoding modes
                                val text = "Astra NovaCore\nPremium Minimalist Black & Gold Aesthetic.\nSupports dual SRT languages simultaneously & complete SW/HW+ decoder selection."
                                android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Mais Informações",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Elegant Tab Row under bottom of the screen (Matches user screenshot: "Vídeos" and "Pastas")
            Column(
                modifier = Modifier
                    .background(Black)
                    .navigationBarsPadding()
            ) {
                Divider(color = BorderGray, thickness = 0.5.dp)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabLabels = listOf("Vídeos", "Pastas")
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
                            Text(
                                text = label,
                                color = if (isSelected) GoldMetallic else MediumGray,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Visual gold pill indicator
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(if (isSelected) GoldMetallic else Color.Transparent)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GoldMetallic,
                contentColor = Black,
                icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar Vídeo de Mídia") },
                text = { Text("Carregar Vídeo", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 12.dp)
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
                            // Empty state beautiful placeholder
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
                                    tint = GoldMetallic.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Nenhum Conteúdo Carregado",
                                    color = Color.White,
                                    fontSize = 18.sp,
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
                            // List layout switch
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
                                            isGridView = true
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
                                            isGridView = false
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
                                    tint = GoldMetallic.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Nenhuma Pasta Criada",
                                    color = Color.White,
                                    fontSize = 18.sp,
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
                                                .border(0.5.dp, GoldMetallic.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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
                                                            tint = GoldMetallic
                                                        )
                                                        Column {
                                                            Text(
                                                                text = folderName,
                                                                color = Color.White,
                                                                fontSize = 15.sp,
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
                                                                isGridView = false
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
            }

            // Spinner while importing
            if (isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
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
                            CircularProgressIndicator(color = GoldMetallic)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Carregando Mídia...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Copiando arquivo para armazenamento local seguro e indexando legendas de alta definição.",
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

    // Modal Import Dialog with pristine formatting
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Carregar Vídeo Off-line",
                    color = GoldMetallic,
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
                        // Title inputs
                        OutlinedTextField(
                            value = inputTitle,
                            onValueChange = { inputTitle = it },
                            label = { Text("Nome do Vídeo", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GoldMetallic,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        // Folder inputs
                        OutlinedTextField(
                            value = inputFolder,
                            onValueChange = { inputFolder = it },
                            label = { Text("Pasta / Categoria", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GoldMetallic,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        // File picker selector or web URL input
                        Text(text = "Defina Origem do Vídeo", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { videoPickerLauncher.launch("video/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (selectedVideoUri != null) GoldMetallic else DarkSurface),
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
                                color = GoldMetallic,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    item {
                        // URL input instead of file picker
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { 
                                inputUrl = it
                                if (it.isNotBlank()) selectedVideoUri = Uri.parse(it) 
                            },
                            label = { Text("Link MP4 / M3U8 (Opcional)", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = GoldMetallic,
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
                        // Subtitle 1 custom
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { srtPicker1Launcher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (selectedSrtUri1 != null) GoldMetallic else DarkSurface),
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
                                    focusedBorderColor = GoldMetallic,
                                    unfocusedBorderColor = BorderGray
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        // Subtitle 2 custom
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { srtPicker2Launcher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = if (selectedSrtUri2 != null) BrightGold else DarkSurface),
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
                                    focusedBorderColor = GoldMetallic,
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
                            // Reset state
                            inputTitle = ""
                            inputUrl = ""
                            inputFolder = "Meus Vídeos"
                            selectedVideoUri = null
                            selectedSrtUri1 = null
                            selectedSrtUri2 = null
                        } else {
                            android.widget.Toast.makeText(context, "Por favor, selecione um vídeo e atribua um nome.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldMetallic)
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
}
