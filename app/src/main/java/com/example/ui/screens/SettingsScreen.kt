package com.example.ui.screens

import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.ui.VideoViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VideoViewModel,
    onClose: () -> Unit
) {
    val activeAccentColor = rememberDynamicAccentColor(viewModel)
    val videoAutoRepeat by viewModel.videoAutoRepeat.collectAsState()
    val currentThemeState by viewModel.currentTheme.collectAsState()
    val isLedMode = remember(currentThemeState) { currentThemeState.startsWith("led_") }

    val context = LocalContext.current
    val customLogoPath by viewModel.customLogoPath.collectAsState()

    var pbyExpanded by rememberSaveable { mutableStateOf(false) }
    var logoExpanded by rememberSaveable { mutableStateOf(false) }
    var cpuExpanded by rememberSaveable { mutableStateOf(false) }
    var dualLibExpanded by rememberSaveable { mutableStateOf(false) }
    val dualLibrary by viewModel.dualLibrary.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val directory = context.filesDir
                    val destinationFile = java.io.File(directory, "custom_user_logo.bin")
                    destinationFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                    viewModel.setCustomLogoPath(destinationFile.absolutePath)
                    viewModel.setTitleLogoType("custom")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = activeAccentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Definições",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar para tela inicial",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Black)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Information Alert Banner / Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = activeAccentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = activeAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Configurações Globais",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Gerencie as opções do player Astra NovaCore de forma centralizada. Estas opções serão aplicadas a todas as mídias executadas.",
                        color = LightGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Playback Options Section (Gaveta)
            SettingsSectionDrawer(
                title = "Opções de Reprodução",
                subtitle = "Comportamento e looping automático",
                icon = Icons.Default.PlayArrow,
                expanded = pbyExpanded,
                onExpandToggle = { pbyExpanded = !pbyExpanded },
                activeAccentColor = activeAccentColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Repetição Automática",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vídeos reiniciam automaticamente em looping",
                            color = LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Switch(
                        checked = videoAutoRepeat,
                        onCheckedChange = { viewModel.setVideoAutoRepeat(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = activeAccentColor,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                            uncheckedBorderColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }

            // Logo Customization Section (Gaveta)
            val logoOptions = remember(customLogoPath) {
                listOf(
                    SettingsLogoOption("classic", "Astra Clássico", "Logotipo minimalista Astra NovaCore", com.example.R.drawable.anc_play_icon_1781702460435, null),
                    SettingsLogoOption("cyber_cat", "Gato Cyberpunk 🐾", "Mascote felino com fones de ouvido e neon dourado", com.example.R.drawable.img_cyber_cat_1781912104185, null),
                    SettingsLogoOption("cute_alien", "E.T. Cinéfilo 🍿", "Alien sorridente comendo pipoca no espaço sideral", com.example.R.drawable.img_cute_alien_1781912115476, null),
                    SettingsLogoOption("fun_gif", "Vibe Gamer GIF ⚡", "Gato fofo animado tocando sintetizadores em loop", null, "https://media.giphy.com/media/GeimqsH0TLDt4tScGw/giphy.gif"),
                    SettingsLogoOption(
                        "custom",
                        "Logo Importado 🌌",
                        if (customLogoPath != null) "Sua própria imagem ou GIF personalizado" else "Toque para importar uma imagem ou GIF do celular",
                        null,
                        customLogoPath
                    )
                )
            }

            val currentLogoType by viewModel.titleLogoType.collectAsState()

            SettingsSectionDrawer(
                title = "Logotipo & Visual",
                subtitle = "Escolha ou importe um ícone para o player",
                icon = Icons.Default.Palette,
                expanded = logoExpanded,
                onExpandToggle = { logoExpanded = !logoExpanded },
                activeAccentColor = activeAccentColor
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    logoOptions.forEach { option ->
                        val isSelected = currentLogoType == option.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) activeAccentColor.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (option.id == "custom" && customLogoPath == null) {
                                        try {
                                            launcher.launch("image/*")
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    } else {
                                        viewModel.setTitleLogoType(option.id)
                                    }
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) activeAccentColor else activeAccentColor.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Circular preview
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                        .border(1.dp, activeAccentColor.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LogoImage(
                                        logoType = option.id,
                                        customLogoPath = customLogoPath,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = option.description,
                                        color = LightGray,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )

                                    if (option.id == "custom" && customLogoPath != null) {
                                        Button(
                                            onClick = {
                                                try {
                                                    launcher.launch("image/*")
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = activeAccentColor.copy(alpha = 0.12f),
                                                contentColor = activeAccentColor
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 1.dp),
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .height(28.dp)
                                        ) {
                                            Text("Alterar imagem/GIF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = activeAccentColor)
                                        }
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (option.id == "custom" && customLogoPath == null) {
                                            try {
                                                launcher.launch("image/*")
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        } else {
                                            viewModel.setTitleLogoType(option.id)
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = activeAccentColor,
                                        unselectedColor = Color.White.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // CPU Cores tuning section (Gaveta)
            val cpuCores by viewModel.cpuCores.collectAsState()
            val physicalCores = remember { Runtime.getRuntime().availableProcessors() }

            SettingsSectionDrawer(
                title = "Processamento & Performance",
                subtitle = "Alocação de potência de hardware",
                icon = Icons.Default.Build,
                expanded = cpuExpanded,
                onExpandToggle = { cpuExpanded = !cpuExpanded },
                activeAccentColor = activeAccentColor
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Large cool processor chip visual
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .border(0.5.dp, activeAccentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Processador Virtual Astra",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Cores físicos detectados: $physicalCores",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(activeAccentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .border(1.dp, activeAccentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${cpuCores} CORE(S)",
                                    color = activeAccentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Core Slider
                    Slider(
                        value = cpuCores.toFloat(),
                        onValueChange = { viewModel.setCpuCores(it.toInt().coerceIn(1, physicalCores)) },
                        valueRange = 1f..maxOf(1f, physicalCores.toFloat()),
                        enabled = physicalCores > 1,
                        steps = if (physicalCores - 1 > 0) physicalCores - 1 else 0,
                        colors = SliderDefaults.colors(
                            thumbColor = activeAccentColor,
                            activeTrackColor = activeAccentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status text based on choice
                    val performanceStatus = when {
                        cpuCores == 1 -> "Modo Ultra Economia (1 core)"
                        cpuCores == physicalCores -> "Modo de Alta Performance (Nativa)"
                        else -> "Modo Balanceado ($cpuCores cores)"
                    }

                    Text(
                        text = "Status: $performanceStatus",
                        color = activeAccentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            // Dual Library Selection (Gaveta)
            SettingsSectionDrawer(
                title = "Biblioteca Dupla (Dual Library)",
                subtitle = "Escolha entre MobileVLCKit e FFmpeg",
                icon = Icons.Default.Build,
                expanded = dualLibExpanded,
                onExpandToggle = { dualLibExpanded = !dualLibExpanded },
                activeAccentColor = activeAccentColor
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Selecione a biblioteca de engine de vídeo padrão:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Card for MobileVLCKit (vlc)
                    val isVlcSelected = dualLibrary == "vlc"
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isVlcSelected) activeAccentColor.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setDualLibrary("vlc") }
                            .border(
                                width = if (isVlcSelected) 1.5.dp else 1.dp,
                                color = if (isVlcSelected) activeAccentColor else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isVlcSelected) activeAccentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isVlcSelected) activeAccentColor else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MobileVLCKit Backend",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Uma Bliblioteca de Alta Performance.",
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            RadioButton(
                                selected = isVlcSelected,
                                onClick = { viewModel.setDualLibrary("vlc") },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = activeAccentColor,
                                    unselectedColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    // Card for FFmpeg (ffmpeg)
                    val isFfmpegSelected = dualLibrary == "ffmpeg"
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFfmpegSelected) activeAccentColor.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setDualLibrary("ffmpeg") }
                            .border(
                                width = if (isFfmpegSelected) 1.5.dp else 1.dp,
                                color = if (isFfmpegSelected) activeAccentColor else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isFfmpegSelected) activeAccentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = if (isFfmpegSelected) activeAccentColor else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FFmpeg Engine Backend",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Essa Bliblioteca Focada em compatibilidade melhor.",
                                    color = LightGray,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            RadioButton(
                                selected = isFfmpegSelected,
                                onClick = { viewModel.setDualLibrary("ffmpeg") },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = activeAccentColor,
                                    unselectedColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            }

            // Version Indicator / Branding footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Astra NovaCore Player",
                        color = MediumGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v1.1.2 • Premium Edition",
                        color = MediumGray.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// Logo option data class for configurations
data class SettingsLogoOption(
    val id: String,
    val name: String,
    val description: String,
    val resourceId: Int?,
    val remoteUrl: String?
)

// Reusable High-Performance Logo Image Composable supporting static & animated GIF Coil inputs
@Composable
fun LogoImage(
    logoType: String,
    modifier: Modifier = Modifier,
    customLogoPath: String? = null
) {
    val context = LocalContext.current

    if (logoType == "custom" && customLogoPath != null) {
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }
        Image(
            painter = rememberAsyncImagePainter(
                model = java.io.File(customLogoPath),
                imageLoader = imageLoader
            ),
            contentDescription = "Logo Personalizado",
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        when (logoType) {
            "cyber_cat" -> {
                Image(
                    painter = painterResource(id = com.example.R.drawable.img_cyber_cat_1781912104185),
                    contentDescription = "Mascote Gato Cyberpunk",
                    modifier = modifier
                )
            }
            "cute_alien" -> {
                Image(
                    painter = painterResource(id = com.example.R.drawable.img_cute_alien_1781912115476),
                    contentDescription = "Mascote E.T. Cinéfilo",
                    modifier = modifier
                )
            }
            "fun_gif" -> {
                val imageLoader = remember {
                    ImageLoader.Builder(context)
                        .components {
                            if (Build.VERSION.SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .build()
                }

                Image(
                    painter = rememberAsyncImagePainter(
                        model = "https://media.giphy.com/media/GeimqsH0TLDt4tScGw/giphy.gif",
                        imageLoader = imageLoader
                    ),
                    contentDescription = "Vibe Gamer GIF",
                    modifier = modifier
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = com.example.R.drawable.anc_play_icon_1781702460435),
                    contentDescription = "Logo Astra Clássico",
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
fun SettingsSectionDrawer(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    activeAccentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (expanded) activeAccentColor.copy(alpha = 0.25f) else activeAccentColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(activeAccentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = activeAccentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = LightGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Colapsar" else "Expandir",
                    tint = LightGray,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = rotationState)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(2.dp))
                    content()
                }
            }
        }
    }
}

