package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DisneySplashScreen(
    onSplashFinished: () -> Unit
) {
    // Phase controllers for the cinematic timing
    var startTextFadeIn by remember { mutableStateOf(false) }
    var startStarArc by remember { mutableStateOf(false) }
    var startSubTitleFadeIn by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pixie_dust")
    
    // Constant twinkling stars in background
    val twinklingPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    // Cinematic continuous spin for the sparkling four-point star lens flare
    val flareRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flare_rotation"
    )

    // Breathing shimmer pulse for the lens flare size
    val flareScale by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flare_scale"
    )

    // Highly premium smooth shimmering metallic glint offset sweeping across logo text
    val shineTransition = rememberInfiniteTransition(label = "title_shimmer")
    val shineOffset by shineTransition.animateFloat(
        initialValue = -800f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    // Animatable for the magical arc progress
    val arcProgress = remember { Animatable(0f) }

    // Derive dynamic glow intensity and blur based on Star Strike (arcProgress transition)
    val arcComp = arcProgress.value
    val logoGlowIntensity by animateFloatAsState(
        targetValue = if (arcComp >= 0.92f) 0.85f else 0.40f,
        animationSpec = tween(600, easing = EaseOutBack),
        label = "glow_intensity"
    )
    val logoGlowBlur by animateFloatAsState(
        targetValue = if (arcComp >= 0.92f) 35f else 14f,
        animationSpec = tween(600, easing = EaseOutBack),
        label = "glow_blur"
    )

    // Text scale and fade animation
    val logoAlpha by animateFloatAsState(
        targetValue = if (startTextFadeIn) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "logo_alpha"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (startTextFadeIn) 1.05f else 0.85f,
        animationSpec = tween(durationMillis = 2000, easing = EaseOutCubic),
        label = "logo_scale"
    )

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (startSubTitleFadeIn) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutQuad),
        label = "subtitle_alpha"
    )

    LaunchedEffect(Unit) {
        // Step 1: Fade-in background starry sky instantly, then fade-in Logo after 200ms
        delay(200)
        startTextFadeIn = true

        // Step 2: After Logo starts fading in, trigger the magical Disney shooting star arc
        delay(600)
        startStarArc = true
        arcProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1800, easing = EaseInOutQuad)
        )

        // Step 3: Fade-in subtitle just before the arc finishes
        delay(800)
        startSubTitleFadeIn = true

        // Step 4: Keep the full cinematic layout visible for 600ms, then trigger finish transition
        delay(1200)
        onSplashFinished()
    }

    // Static layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060608), // Extremely rich onyx black/charcoal
                        Color(0xFF131417), // Deep slate carbon gray
                        Color(0xFF202227), // Soft metallic silver-gray twilight edge
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Draw Disney Castle & Twinkled Star Constraints in background via Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Sparkle Particle stars in the sky
            val starPositions = listOf(
                Offset(width * 0.15f, height * 0.22f),
                Offset(width * 0.85f, height * 0.15f),
                Offset(width * 0.3f, height * 0.12f),
                Offset(width * 0.72f, height * 0.28f),
                Offset(width * 0.25f, height * 0.45f),
                Offset(width * 0.82f, height * 0.48f),
                Offset(width * 0.5f, height * 0.18f),
                Offset(width * 0.10f, height * 0.65f),
                Offset(width * 0.90f, height * 0.62f)
            )

            starPositions.forEachIndexed { idx, offset ->
                val currentTwinkle = if (idx % 2 == 0) twinklingPulse else (1.2f - twinklingPulse)
                
                if (idx % 3 == 0) {
                    // Draw a gorgeous cinematic 4-point Diamond Star
                    val starSize = (7.dp.toPx() * currentTwinkle).coerceAtLeast(1f)
                    val colorOuter = Color(0xFFFFD700).copy(alpha = 0.18f * currentTwinkle)
                    val colorInner = Color.White.copy(alpha = 0.9f * currentTwinkle)
                    
                    // Outer structural flares
                    drawLine(
                        color = colorOuter,
                        start = Offset(offset.x, offset.y - starSize * 2.5f),
                        end = Offset(offset.x, offset.y + starSize * 2.5f),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    drawLine(
                        color = colorOuter,
                        start = Offset(offset.x - starSize * 2.5f, offset.y),
                        end = Offset(offset.x + starSize * 2.5f, offset.y),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    
                    // High radiance white inner lines
                    drawLine(
                        color = colorInner,
                        start = Offset(offset.x, offset.y - starSize),
                        end = Offset(offset.x, offset.y + starSize),
                        strokeWidth = 0.8.dp.toPx()
                    )
                    drawLine(
                        color = colorInner,
                        start = Offset(offset.x - starSize, offset.y),
                        end = Offset(offset.x + starSize, offset.y),
                        strokeWidth = 0.8.dp.toPx()
                    )
                } else {
                    // Classic soft stellar body with Cyber Astral Blue aura
                    drawCircle(
                        color = Color(0xFF8CE3FF).copy(alpha = 0.15f * currentTwinkle),
                        radius = 6.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.85f * currentTwinkle),
                        radius = 1.5.dp.toPx(),
                        center = offset
                    )
                }
            }

            // Expanding cosmic Supernova radial gaseous backing nebula behind logo
            val radialGlowScale = if (startTextFadeIn) logoScale else 0f
            val nebulaGlowAlpha = 0.14f * logoAlpha * (0.8f + 0.2f * sin(flareRotation * (PI / 180.0)).toFloat())
            val maxNebulaRadius = width * 0.48f * radialGlowScale
            if (maxNebulaRadius > 0.1f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = nebulaGlowAlpha),
                            Color(0xFF8CE3FF).copy(alpha = nebulaGlowAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(width / 2f, height / 2f - 20.dp.toPx()),
                        radius = maxNebulaRadius
                    ),
                    radius = maxNebulaRadius,
                    center = Offset(width / 2f, height / 2f - 20.dp.toPx())
                )
            }

            // Draw the Disney Magic Shooting Sparkle Arc over the center
            if (startStarArc) {
                val arcRadius = width * 0.38f
                val centerX = width / 2f
                val centerY = height / 2f - 60.dp.toPx()

                // Generate point coordinates on the semicircular arc (from 180 degrees to 360/0 degrees)
                val currentProgress = arcProgress.value
                val strokeWidth = 3.dp.toPx()

                // Draw background arc trace subtly
                drawArc(
                    color = Color(0xFFFFD700).copy(alpha = 0.08f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = strokeWidth)
                )

                // Draw the dynamic animated glowing trace of pixie dust
                if (currentProgress > 0f) {
                    val sweep = 180f * currentProgress
                    drawArc(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0f),
                                Color(0xFFFFEFA6).copy(alpha = 0.4f * currentProgress),
                                Color.White.copy(alpha = 0.9f)
                              )
                        ),
                        startAngle = 180f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                        size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                        style = Stroke(width = strokeWidth)
                    )

                    // Find current tip of the shooting star
                    val currentAngleRad = (180f + sweep) * (PI / 180.0)
                    val starTipX = centerX + arcRadius * cos(currentAngleRad).toFloat()
                    val starTipY = centerY + arcRadius * sin(currentAngleRad).toFloat()
                    val starTip = Offset(starTipX, starTipY)

                    // Draw a spectacular cinematic 8-point rotating star lens flare at the tip
                    withTransform({
                        rotate(degrees = flareRotation, pivot = starTip)
                    }) {
                        // High-contrast primary light axes
                        drawLine(
                            color = Color.White.copy(alpha = 0.95f),
                            start = Offset(starTip.x, starTip.y - 15.dp.toPx() * flareScale),
                            end = Offset(starTip.x, starTip.y + 15.dp.toPx() * flareScale),
                            strokeWidth = 1.3.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.95f),
                            start = Offset(starTip.x - 15.dp.toPx() * flareScale, starTip.y),
                            end = Offset(starTip.x + 15.dp.toPx() * flareScale, starTip.y),
                            strokeWidth = 1.3.dp.toPx()
                        )
                        // Warm golden secondary diagonal beams
                        drawLine(
                            color = Color(0xFFFFD700).copy(alpha = 0.75f),
                            start = Offset(starTip.x - 9.dp.toPx() * flareScale, starTip.y - 9.dp.toPx() * flareScale),
                            end = Offset(starTip.x + 9.dp.toPx() * flareScale, starTip.y + 9.dp.toPx() * flareScale),
                            strokeWidth = 0.9.dp.toPx()
                        )
                        drawLine(
                            color = Color(0xFFFFD700).copy(alpha = 0.75f),
                            start = Offset(starTip.x - 9.dp.toPx() * flareScale, starTip.y + 9.dp.toPx() * flareScale),
                            end = Offset(starTip.x + 9.dp.toPx() * flareScale, starTip.y - 9.dp.toPx() * flareScale),
                            strokeWidth = 0.9.dp.toPx()
                        )
                    }

                    // Soft ambient golden aura backing the flare
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = 0.15f),
                        radius = 11.dp.toPx() * flareScale,
                        center = starTip
                    )
                    // High radiance ultra-compact core
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = starTip
                    )

                    // Draw multi-colored high-fidelity fluid pixie dust sparks following the star trail with wave oscillation
                    for (i in 1..16) {
                        val trailingProgress = (currentProgress - (i * 0.022f)).coerceIn(0f, 1f)
                        if (trailingProgress > 0f) {
                            val trailAngleRad = (180f + (180f * trailingProgress)) * (PI / 180.0)
                            
                            // Multi-harmonic sine waves to simulate a whimsical space wind sway
                            val waveTime = flareRotation * (PI / 180.0)
                            val waveOffset = (sin(i * 1.3 + waveTime * 2.2) * 12.dp.toPx() + 
                                             cos(i * 0.7 - waveTime) * 5.dp.toPx()).toFloat()
                            
                            val dustX = centerX + (arcRadius + waveOffset) * cos(trailAngleRad).toFloat()
                            val dustY = centerY + (arcRadius + waveOffset) * sin(trailAngleRad).toFloat()

                            // Alternate between Gold, Astral Blue, and Pure White
                            val dustColor = when (i % 3) {
                                0 -> Color(0xFFFFD700) // Rich Gold
                                1 -> Color(0xFF7FE5FF) // Cyber Astral Light Blue
                                else -> Color(0xFFFFFFFF) // Brilliant White
                            }

                            // Calculate luxurious distance-based opacity decay and twinkling
                            val distanceFade = (1.0f - (i / 16f)).coerceIn(0f, 1f)
                            val individualTwinkle = 0.6f + 0.4f * sin(waveTime * 1.5 + i).toFloat()
                            val particleAlpha = 0.9f * distanceFade * individualTwinkle

                            drawCircle(
                                color = dustColor.copy(alpha = particleAlpha),
                                radius = (3.dp.toPx() * distanceFade).coerceAtLeast(0.6f.dp.toPx()),
                                center = Offset(dustX, dustY)
                            )
                        }
                    }

                    // Expand impact shockwave ring on arc progress completion (right-most impact point)
                    if (currentProgress >= 0.92f) {
                        val impactProgress = (currentProgress - 0.92f) / 0.08f
                        val rippleRadius = 48.dp.toPx() * impactProgress
                        
                        // Golden ripple ring
                        drawCircle(
                            color = Color(0xFFFFD700).copy(alpha = 0.35f * (1f - impactProgress)),
                            radius = rippleRadius,
                            center = Offset(centerX + arcRadius, centerY),
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        // Outer ethereal white shockwave
                        drawCircle(
                            color = Color.White.copy(alpha = 0.20f * (1f - impactProgress)),
                            radius = rippleRadius * 1.3f,
                            center = Offset(centerX + arcRadius, centerY),
                            style = Stroke(width = 1.0.dp.toPx())
                        )
                    }
                }
            }
        }

        // Center Logo & Title Layout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-20).dp)
        ) {
            // Elegant glowing metallic gold typography branding with sweeping shiny metallic glint/shimmer
            Text(
                text = "Astra NovaCore",
                style = TextStyle(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    letterSpacing = 4.sp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD700), // Gold
                            Color(0xFFFFF1C2), // Warm Golden Glint Edge
                            Color.White,       // Ultra Bright Shiny Shimmer Core
                            Color(0xFFFFF1C2), // Warm Golden Glint Edge
                            Color(0xFFFFD700)  // Gold
                        ),
                        start = Offset(shineOffset, 0f),
                        end = Offset(shineOffset + 240f, 240f)
                    ),
                    shadow = Shadow(
                        color = Color(0xFFFFD700).copy(alpha = logoGlowIntensity),
                        offset = Offset(0f, 0f),
                        blurRadius = logoGlowBlur
                    )
                ),
                modifier = Modifier
                    .graphicsLayer(
                        scaleX = logoScale,
                        scaleY = logoScale,
                        alpha = logoAlpha
                    )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "CINEMATIC PLAYER",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 6.sp,
                    color = Color(0xFFFFD700).copy(alpha = 0.85f)
                ),
                modifier = Modifier.graphicsLayer(alpha = subtitleAlpha)
            )
        }

        // Animated pixie dust clusters gracefully orbiting or rising from base
        val particleTransition = rememberInfiniteTransition(label = "particles")
        val pOffset1 by particleTransition.animateFloat(
            initialValue = -15f,
            targetValue = 15f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "p1"
        )
        val pOffset2 by particleTransition.animateFloat(
            initialValue = 20f,
            targetValue = -20f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "p2"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Sparkle 1 (Left-Center)
            drawCircle(
                color = Color(0xFFFFDF7A).copy(alpha = 0.3f),
                radius = 4.dp.toPx(),
                center = Offset(width * 0.35f + pOffset1, height * 0.58f + pOffset2)
            )

            // Sparkle 2 (Right-Center)
            drawCircle(
                color = Color(0xFFFFFAF0).copy(alpha = 0.45f),
                radius = 3.dp.toPx(),
                center = Offset(width * 0.65f + pOffset2, height * 0.55f + pOffset1)
            )

            // Sparkle 3 (Bottom-Center)
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = 0.4f),
                radius = 3.5.dp.toPx(),
                center = Offset(width * 0.48f + pOffset1 * 0.5f, height * 0.62f + pOffset1)
            )
        }
    }
}
