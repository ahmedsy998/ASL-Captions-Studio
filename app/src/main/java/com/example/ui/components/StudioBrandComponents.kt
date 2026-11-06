package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.media.CaptionAnimationEngine
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainNavSection
import kotlin.math.min

@Composable
fun AslStudioLogoHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 42.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Custom Clapperboard + Play + Captions Vector Icon
        Canvas(modifier = Modifier.size(iconSize)) {
            val w = size.width
            val h = size.height

            // Clapperboard top bar (Orange)
            drawRoundRect(
                color = PrimaryOrange,
                topLeft = Offset(w * 0.08f, h * 0.10f),
                size = Size(w * 0.84f, h * 0.22f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
            )

            // Main slate body
            drawRoundRect(
                color = DarkSilver,
                topLeft = Offset(w * 0.08f, h * 0.30f),
                size = Size(w * 0.84f, h * 0.62f),
                cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
            )

            // Play triangle inside slate
            val playPath = Path().apply {
                moveTo(w * 0.40f, h * 0.44f)
                lineTo(w * 0.40f, h * 0.70f)
                lineTo(w * 0.66f, h * 0.57f)
                close()
            }
            drawPath(playPath, color = PrimaryOrange)

            // Caption indicator bars at bottom
            drawRoundRect(
                color = SoftIvory,
                topLeft = Offset(w * 0.22f, h * 0.77f),
                size = Size(w * 0.30f, h * 0.06f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = PrimaryOrange,
                topLeft = Offset(w * 0.56f, h * 0.77f),
                size = Size(w * 0.22f, h * 0.06f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        Column {
            Text(
                text = "ASL",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Captions Studio",
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryOrange
            )
        }
    }
}

@Composable
fun StudioBottomNavigationBar(
    currentSection: MainNavSection,
    isArabic: Boolean,
    onSelectSection: (MainNavSection) -> Unit
) {
    val navItems = listOf(
        Triple(MainNavSection.HOME, Icons.Filled.Home, Icons.Outlined.Home),
        Triple(MainNavSection.PROJECTS, Icons.Filled.Folder, Icons.Outlined.Folder),
        Triple(MainNavSection.CREATE, Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
        Triple(MainNavSection.TEMPLATES, Icons.Filled.DashboardCustomize, Icons.Outlined.DashboardCustomize),
        Triple(MainNavSection.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_navigation_bar")
    ) {
        navItems.forEach { (section, filledIcon, outlinedIcon) ->
            val selected = currentSection == section
            val label = if (isArabic) section.labelAr else section.labelEn
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectSection(section) },
                modifier = Modifier.testTag("nav_${section.name.lowercase()}"),
                icon = {
                    Icon(
                        imageVector = if (selected) filledIcon else outlinedIcon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryOrange,
                    selectedTextColor = PrimaryOrange,
                    indicatorColor = PrimaryOrange.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Live Caption Overlay Composable (Sections 7, 8, 9, 13)
 * Renders synchronized word-by-word or full-sentence captions in real time
 * using the exact same CaptionAnimationEngine state as the MP4 export renderer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveCaptionOverlay(
    positionMs: Long,
    captionMode: CaptionDisplayMode,
    segments: List<CaptionSegment>,
    defaultStyle: CaptionStyle,
    modifier: Modifier = Modifier
) {
    val activeSegment = segments.firstOrNull { positionMs in it.startTimeMs..it.endTimeMs }
        ?: segments.firstOrNull()
        ?: return

    val style = activeSegment.styleOverride ?: defaultStyle
    val animState = CaptionAnimationEngine.computeFrameState(
        params = style.animation,
        currentPositionMs = positionMs.coerceIn(activeSegment.startTimeMs, activeSegment.endTimeMs),
        segmentStartMs = activeSegment.startTimeMs,
        segmentEndMs = activeSegment.endTimeMs
    )

    val words = activeSegment.words.ifEmpty {
        activeSegment.text.split(Regex("\\s+")).filter { it.isNotBlank() }.mapIndexed { idx, w ->
            CaptionWord(
                text = w,
                startTimeMs = activeSegment.startTimeMs + idx * 350L,
                endTimeMs = activeSegment.startTimeMs + (idx + 1) * 350L
            )
        }
    }

    val activeWordIdx = words.indexOfFirst { positionMs in it.startTimeMs..it.endTimeMs }
        .let { idx -> if (idx >= 0) idx else 0 }

    val visibleWords = when (captionMode) {
        CaptionDisplayMode.WORD_BY_WORD -> {
            val groupIdx = activeWordIdx / 3
            val start = groupIdx * 3
            val end = min(words.size, start + 3)
            words.subList(start, end)
        }
        CaptionDisplayMode.FULL_SENTENCE -> {
            val revealCount = (words.size * animState.characterRevealFraction).toInt().coerceAtLeast(1)
            words.take(revealCount)
        }
    }

    val boxAlignment = when (style.positionPreset) {
        CaptionPositionPreset.TOP -> Alignment.TopCenter
        CaptionPositionPreset.CENTER -> Alignment.Center
        CaptionPositionPreset.BOTTOM -> Alignment.BottomCenter
        CaptionPositionPreset.CUSTOM -> Alignment.BottomCenter
    }

    val fontFamily = resolveCaptionFontFamily(style.fontFamilyName)
    val scaledFontSize = (style.fontSizeSp * 0.48f).coerceIn(13f, 28f).sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = boxAlignment
    ) {
        val bgColor = when (style.backgroundStyle) {
            CaptionBackgroundStyle.NONE -> Color.Transparent
            CaptionBackgroundStyle.SOLID -> Color(style.backgroundColorArgb).copy(alpha = style.backgroundOpacity.coerceIn(0.8f, 1f))
            CaptionBackgroundStyle.TRANSPARENT -> Color(style.backgroundColorArgb).copy(alpha = (style.backgroundOpacity * 0.5f).coerceIn(0.15f, 0.6f))
            CaptionBackgroundStyle.ROUNDED_RECT -> Color(style.backgroundColorArgb).copy(alpha = style.backgroundOpacity)
        }
        val cornerShape = if (style.backgroundStyle == CaptionBackgroundStyle.ROUNDED_RECT) {
            RoundedCornerShape(style.backgroundCornerRadius.dp)
        } else {
            RoundedCornerShape(4.dp)
        }

        FlowRow(
            modifier = Modifier
                .graphicsLayer {
                    alpha = animState.alpha.coerceIn(0.15f, 1f)
                    scaleX = animState.scaleX
                    scaleY = animState.scaleY
                    translationX = animState.translationXPx * 0.4f
                    translationY = animState.translationYPx * 0.4f
                    rotationZ = animState.rotationDegrees
                }
                .background(color = bgColor, shape = cornerShape)
                .padding(
                    horizontal = (style.backgroundPaddingH * 0.6f).dp,
                    vertical = (style.backgroundPaddingV * 0.6f).dp
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = style.maxWordsPerLine.coerceIn(1, 8)
        ) {
            visibleWords.forEach { word ->
                val isCurrentWord = word.id == words.getOrNull(activeWordIdx)?.id
                val isHighlighted = isCurrentWord || word.isHighlighted

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            if (isCurrentWord) {
                                scaleX = animState.activeWordScale
                                scaleY = animState.activeWordScale
                            }
                        }
                        .background(
                            color = if (isCurrentWord) Color(style.activeWordColorArgb)
                            else if (word.isHighlighted) Color(style.activeWordColorArgb).copy(alpha = 0.32f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = if (isHighlighted) 6.dp else 2.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = word.text,
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = scaledFontSize,
                            fontWeight = if (style.fontWeight >= 700 || isHighlighted) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrentWord) Color.White else Color(style.textColorArgb),
                            letterSpacing = (style.letterSpacingEm + animState.extraLetterSpacingEm).sp,
                            shadow = Shadow(
                                color = Color(style.shadowColorArgb),
                                offset = Offset(style.shadowOffsetX, style.shadowOffsetY),
                                blurRadius = (style.shadowBlurRadius + animState.glowBlurRadius).coerceAtLeast(1f)
                            ),
                            textAlign = when (style.textAlignment) {
                                CaptionTextAlignment.START -> TextAlign.Start
                                CaptionTextAlignment.CENTER -> TextAlign.Center
                                CaptionTextAlignment.END -> TextAlign.End
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PodcastStudioThumbnail(
    modifier: Modifier = Modifier,
    accentColor: Color = PrimaryOrange,
    badgeText: String? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(DeepCharcoal, StudioTealSlate, DarkSilver)
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Warm studio bokeh & speaker silhouette illustration
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = accentColor.copy(alpha = 0.22f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.5f, size.height * 0.42f)
            )
            // Head & headphones arc
            drawCircle(
                color = SoftIvory.copy(alpha = 0.85f),
                radius = size.minDimension * 0.16f,
                center = Offset(size.width * 0.5f, size.height * 0.36f)
            )
            // Shoulders
            drawRoundRect(
                color = DarkSilverSurface,
                topLeft = Offset(size.width * 0.26f, size.height * 0.55f),
                size = Size(size.width * 0.48f, size.height * 0.36f),
                cornerRadius = CornerRadius(24f, 24f)
            )
            // Studio microphone
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(size.width * 0.62f, size.height * 0.46f),
                size = Size(size.width * 0.10f, size.height * 0.20f),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }
        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .background(DeepCharcoal.copy(alpha = 0.88f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                    color = PrimaryOrange,
                    maxLines = 1
                )
            }
        }
    }
}

fun formatTimestampMs(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

fun formatBytesSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format("%.1f GB", mb / 1024.0)
    } else {
        String.format("%.1f MB", mb)
    }
}
