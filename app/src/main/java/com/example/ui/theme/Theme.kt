package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import com.example.ui.VideoViewModel

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val CustomDarkColorScheme = darkColorScheme(
    primary = GoldMetallic,
    onPrimary = Black,
    secondary = White,
    onSecondary = Black,
    tertiary = BrightGold,
    background = Black,
    onBackground = White,
    surface = DarkBackground,
    onSurface = OffWhite,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = LightGray,
    outline = BorderGray
)

private val CustomLightColorScheme = lightColorScheme(
    primary = GoldMetallic,
    onPrimary = Black,
    secondary = Black,
    onSecondary = White,
    tertiary = DeepGold,
    background = OffWhite,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = LightGray,
    onSurfaceVariant = DarkBackground,
    outline = LightGray
)

@androidx.compose.runtime.Composable
fun rememberDynamicAccentColor(viewModel: VideoViewModel): androidx.compose.ui.graphics.Color {
    val themeState by viewModel.currentTheme.collectAsState()
    return when (themeState) {
        "led_warm" -> {
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "WarmLed")
            val animatedColor by infiniteTransition.animateColor(
                initialValue = androidx.compose.ui.graphics.Color(0xFFFF1493), // Beautiful bright deep neon pink/rose
                targetValue = androidx.compose.ui.graphics.Color(0xFFFF7F00), // Vibrant sunset neon orange
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "WarmLedColor"
            )
            animatedColor
        }
        "led_cold" -> {
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "ColdLed")
            val animatedColor by infiniteTransition.animateColor(
                initialValue = androidx.compose.ui.graphics.Color(0xFF0044FF), // Deep electric blue
                targetValue = androidx.compose.ui.graphics.Color(0xFF00FFFF), // Radiant cyber cyan/turquoise
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "ColdLedColor"
            )
            animatedColor
        }
        "led_multicolor" -> {
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "MulticolorLed")
            val hue by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(6000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                ),
                label = "HueFloat"
            )
            androidx.compose.ui.graphics.Color.hsv(hue, 0.9f, 0.95f)
        }
        else -> GoldMetallic // Classic luxury Aura Gold
    }
}

@androidx.compose.runtime.Composable
fun rememberDynamicGradient(viewModel: VideoViewModel): androidx.compose.ui.graphics.Brush {
    val themeState by viewModel.currentTheme.collectAsState()
    val accent = rememberDynamicAccentColor(viewModel)
    return when (themeState) {
        "led_warm" -> {
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "WarmGrad")
            val offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(4000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                ),
                label = "WarmGradFloat"
            )
            androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(androidx.compose.ui.graphics.Color(0xFFFF1493), androidx.compose.ui.graphics.Color(0xFFFF5252), androidx.compose.ui.graphics.Color(0xFFFF7F00)),
                start = androidx.compose.ui.geometry.Offset(offset, 0f),
                end = androidx.compose.ui.geometry.Offset(offset + 400f, 400f)
            )
        }
        "led_cold" -> {
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "ColdGrad")
            val offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(4000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                ),
                label = "ColdGradFloat"
            )
            androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(androidx.compose.ui.graphics.Color(0xFF0044FF), androidx.compose.ui.graphics.Color(0xFF00FFFF), androidx.compose.ui.graphics.Color(0xFF7B00FF)),
                start = androidx.compose.ui.geometry.Offset(offset, 0f),
                end = androidx.compose.ui.geometry.Offset(offset + 400f, 400f)
            )
        }
        "led_multicolor" -> {
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "MulticolorGrad")
            val offset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1000f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(5000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                ),
                label = "MulticolorGradFloat"
            )
            androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.Color(0xFFFF007F), // Neon Pink
                    androidx.compose.ui.graphics.Color(0xFF7F00FF), // Violet
                    androidx.compose.ui.graphics.Color(0xFF00F0FF), // Cyan
                    androidx.compose.ui.graphics.Color(0xFF00FF7F), // Mint Green
                    androidx.compose.ui.graphics.Color(0xFFFFD700)  // Gold Yellow
                ),
                start = androidx.compose.ui.geometry.Offset(offset, 0f),
                end = androidx.compose.ui.geometry.Offset(offset + 500f, 500f)
            )
        }
        else -> {
            androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(GoldMetallic, androidx.compose.ui.graphics.Color(0xFFF3E5AB), GoldMetallic)
            )
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark layout for the cinematic visual style
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our exact custom palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CustomDarkColorScheme else CustomLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

fun Modifier.ledGlow(
    color: androidx.compose.ui.graphics.Color,
    borderRadius: Dp = 12.dp,
    glowRadius: Dp = 10.dp,
    enabled: Boolean = true
): Modifier = if (!enabled) this else this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            this.color = android.graphics.Color.TRANSPARENT
            this.setShadowLayer(
                glowRadius.toPx(),
                0f,
                0f,
                color.toArgb()
            )
        }
        canvas.nativeCanvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            borderRadius.toPx(),
            borderRadius.toPx(),
            paint
        )
    }
}

@Composable
fun Modifier.rememberLedGlow(
    color: androidx.compose.ui.graphics.Color,
    borderRadius: Dp = 12.dp,
    baseGlowRadius: Dp = 10.dp,
    enabled: Boolean = true,
    pulseEnabled: Boolean = true
): Modifier {
    if (!enabled) return this
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "GlowPulse")
    val multiplier by if (pulseEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.3f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "GlowMultiplier"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1.0f) }
    }
    
    val currentRadiusSpec = baseGlowRadius * multiplier
    
    return this.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().asFrameworkPaint().apply {
                this.color = android.graphics.Color.TRANSPARENT
                this.setShadowLayer(
                    currentRadiusSpec.toPx(),
                    0f,
                    0f,
                    color.toArgb()
                )
            }
            canvas.nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                borderRadius.toPx(),
                borderRadius.toPx(),
                paint
            )
        }
    }
}

