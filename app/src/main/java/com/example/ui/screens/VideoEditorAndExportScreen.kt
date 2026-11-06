package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ai.TranscriptionProgress
import com.example.data.repository.StudioProject
import com.example.domain.model.*
import com.example.media.ExportProgressState
import com.example.media.SharedFrameRenderer
import com.example.ui.components.LiveCaptionOverlay
import com.example.ui.components.PodcastStudioThumbnail
import com.example.ui.components.formatBytesSize
import com.example.ui.components.formatTimestampMs
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun VideoEditorScreen(
    isArabic: Boolean,
    project: StudioProject,
    selectedTool: EditorToolTab,
    playbackPositionMs: Long,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    timelineZoom: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    transcriptionLanguage: TranscriptionLanguage,
    transcriptionState: TranscriptionProgress,
    isAiToolLoading: Boolean,
    aiErrorBanner: String?,
    aiSuggestion: AiSmartSuggestion?,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenExportCenter: () -> Unit,
    onSelectTool: (EditorToolTab) -> Unit,
    onPlaybackPositionChange: (Long) -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    onToggleFullscreen: () -> Unit,
    onTimelineZoomChange: (Float) -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onTrimVideoClip: (String, Long, Long) -> Unit,
    onDeleteTimelineClip: (String) -> Unit,
    onAspectRatioChange: (AspectRatioPreset) -> Unit,
    onReplaceSourceVideo: () -> Unit,
    onCaptionModeChange: (CaptionDisplayMode) -> Unit,
    onTranscriptionLanguageChange: (TranscriptionLanguage) -> Unit,
    onRunAutoTranscription: () -> Unit,
    onAddCaption: (String) -> Unit,
    onUpdateSegmentText: (String, String, Long?, Long?) -> Unit,
    onUpdateWord: (String, String, String, Long, Long, Boolean) -> Unit,
    onSplitSegment: (String) -> Unit,
    onMergeWithNext: (String) -> Unit,
    onDuplicateSegment: (String) -> Unit,
    onDeleteSegment: (String) -> Unit,
    onToggleSelectSegment: (String) -> Unit,
    onUpdateStyle: ((CaptionStyle) -> CaptionStyle) -> Unit,
    onUpdateAnimation: ((AnimationParameters) -> AnimationParameters) -> Unit,
    onUpdateEffects: ((VideoEffectsConfig) -> VideoEffectsConfig) -> Unit,
    onResetEffects: () -> Unit,
    onUpdateAudio: ((AudioTrackConfig) -> AudioTrackConfig) -> Unit,
    onImportMusic: () -> Unit,
    onImportSoundEffect: () -> Unit,
    onRemoveAudioClip: (String) -> Unit,
    onRunSmartAiTool: (Int) -> Unit,
    onApplyAiSuggestion: (AiSmartSuggestion) -> Unit,
    onDismissAiSuggestion: () -> Unit
) {
    val context = LocalContext.current

    // Real Media3 ExoPlayer lifecycle management (Section 5A & 19)
    val exoPlayer = remember(project.videoMetadata.uriString) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            runCatching {
                setMediaItem(MediaItem.fromUri(Uri.parse(project.videoMetadata.uriString)))
                prepare()
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            runCatching { exoPlayer.release() }
        }
    }

    LaunchedEffect(isPlaying) {
        runCatching {
            if (isPlaying) exoPlayer.play() else exoPlayer.pause()
        }
    }

    LaunchedEffect(project.audioConfig.originalVolume, project.audioConfig.isOriginalMuted) {
        runCatching {
            exoPlayer.volume = if (project.audioConfig.isOriginalMuted) 0f else project.audioConfig.originalVolume.coerceIn(0f, 1f)
        }
    }

    // Smooth 30fps live playhead synchronization
    LaunchedEffect(isPlaying, project.videoMetadata.durationMs) {
        while (isPlaying) {
            delay(33L)
            val exoPos = runCatching { exoPlayer.currentPosition }.getOrDefault(0L)
            val nextPos = if (exoPos > 0L) exoPos else (playbackPositionMs + 33L) % project.videoMetadata.durationMs.coerceAtLeast(1000L)
            onPlaybackPositionChange(nextPos)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("video_editor_screen")
    ) {
        // Top Studio Header Bar (matching screenshot: تصدير | Undo/Redo | تحرير الفيديو | Back)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = if (isArabic) "تحرير الفيديو" else "Video Editor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.testTag("editor_undo_button")) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.testTag("editor_redo_button")) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }
                    Button(
                        onClick = onOpenExportCenter,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("editor_export_button")
                    ) {
                        Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "تصدير" else "Export", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Main Workspace: Video Preview + Multi-Track Timeline + Active Tool Panel
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // A. VIDEO PREVIEW AREA WITH LIVE EFFECTS & SYNCHRONIZED CAPTIONS
            VideoPreviewArea(
                isArabic = isArabic,
                exoPlayer = exoPlayer,
                project = project,
                playbackPositionMs = playbackPositionMs,
                isPlaying = isPlaying,
                isFullscreen = isFullscreen,
                onPlayPauseToggle = { onPlayingChange(!isPlaying) },
                onSeekTo = { pos ->
                    onPlaybackPositionChange(pos)
                    runCatching { exoPlayer.seekTo(pos) }
                },
                onAspectRatioChange = onAspectRatioChange,
                onReplaceVideo = onReplaceSourceVideo,
                onToggleFullscreen = onToggleFullscreen
            )

            if (!isFullscreen) {
                // B. FUNCTIONAL 5-TRACK MULTI-TRACK TIMELINE
                MultiTrackTimelineSection(
                    isArabic = isArabic,
                    project = project,
                    playbackPositionMs = playbackPositionMs,
                    timelineZoom = timelineZoom,
                    onSeekTo = { pos ->
                        onPlaybackPositionChange(pos)
                        runCatching { exoPlayer.seekTo(pos) }
                    },
                    onZoomChange = onTimelineZoomChange,
                    onSplitAtPlayhead = onSplitAtPlayhead,
                    onTrimFirstClip = {
                        project.timelineSegments.firstOrNull()?.let { clip ->
                            onTrimVideoClip(clip.id, 0L, -500L)
                        }
                    },
                    onDeleteSelectedClip = {
                        project.timelineSegments.lastOrNull()?.let { clip ->
                            onDeleteTimelineClip(clip.id)
                        }
                    }
                )

                // C. ACTIVE EDITING TOOL PANEL
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (selectedTool) {
                        EditorToolTab.CAPTIONS -> CaptionsEditorPanel(
                            isArabic = isArabic,
                            project = project,
                            transcriptionLanguage = transcriptionLanguage,
                            transcriptionState = transcriptionState,
                            onSelectMode = onCaptionModeChange,
                            onSelectTranscriptionLanguage = onTranscriptionLanguageChange,
                            onRunAutoTranscription = onRunAutoTranscription,
                            onAddCaption = onAddCaption,
                            onUpdateSegmentText = onUpdateSegmentText,
                            onUpdateWord = onUpdateWord,
                            onSplitSegment = onSplitSegment,
                            onMergeWithNext = onMergeWithNext,
                            onDuplicateSegment = onDuplicateSegment,
                            onDeleteSegment = onDeleteSegment,
                            onToggleSelectSegment = onToggleSelectSegment,
                            onSeekTo = { pos ->
                                onPlaybackPositionChange(pos)
                                runCatching { exoPlayer.seekTo(pos) }
                            }
                        )
                        EditorToolTab.TEXT,
                        EditorToolTab.FONTS,
                        EditorToolTab.BACKGROUND -> TextDesignStudioPanel(
                            isArabic = isArabic,
                            style = project.captionStyle,
                            focusSection = selectedTool,
                            onUpdateStyle = onUpdateStyle
                        )
                        EditorToolTab.ANIMATION -> AnimationStudioPanel(
                            isArabic = isArabic,
                            animation = project.captionStyle.animation,
                            onUpdateAnimation = onUpdateAnimation
                        )
                        EditorToolTab.EFFECTS,
                        EditorToolTab.FILTERS -> VideoEffectsStudioPanel(
                            isArabic = isArabic,
                            effects = project.videoEffects,
                            onUpdateEffects = onUpdateEffects,
                            onResetEffects = onResetEffects
                        )
                        EditorToolTab.AUDIO -> AudioStudioPanel(
                            isArabic = isArabic,
                            audioConfig = project.audioConfig,
                            onUpdateAudio = onUpdateAudio,
                            onImportMusic = onImportMusic,
                            onImportSoundEffect = onImportSoundEffect,
                            onRemoveAudioClip = onRemoveAudioClip
                        )
                        EditorToolTab.CROP -> CropAndFormatPanel(
                            isArabic = isArabic,
                            currentRatio = project.aspectRatioPreset,
                            effects = project.videoEffects,
                            onSelectRatio = onAspectRatioChange,
                            onUpdateEffects = onUpdateEffects
                        )
                        EditorToolTab.AI_TOOLS -> AiSmartToolsPanel(
                            isArabic = isArabic,
                            isLoading = isAiToolLoading,
                            errorBanner = aiErrorBanner,
                            activeSuggestion = aiSuggestion,
                            onRunTool = onRunSmartAiTool,
                            onApplySuggestion = onApplyAiSuggestion,
                            onDismissSuggestion = onDismissAiSuggestion
                        )
                        EditorToolTab.EXPORT -> {}
                    }
                }
            }
        }

        // D. BOTTOM EDITING TOOLBAR (matching screenshot: الكابشن | الخطوط | الحركة | الصوت | الخلفية | تأثيرات | قص | الذكاء الاصطناعي)
        EditorBottomToolbar(
            isArabic = isArabic,
            selectedTool = selectedTool,
            onSelectTool = onSelectTool
        )
    }
}

@Composable
private fun VideoPreviewArea(
    isArabic: Boolean,
    exoPlayer: ExoPlayer,
    project: StudioProject,
    playbackPositionMs: Long,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onAspectRatioChange: (AspectRatioPreset) -> Unit,
    onReplaceVideo: () -> Unit,
    onToggleFullscreen: () -> Unit
) {
    val effects = project.videoEffects
    var showRatioMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFullscreen) 420.dp else 225.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DeepCharcoalDarker)
                .border(1.dp, DarkSilver, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val aspectFloat = project.aspectRatioPreset.toFloatRatio()

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(aspectFloat.coerceIn(0.56f, 1.8f), matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(12.dp))
                    .graphicsLayer {
                        val dur = project.videoMetadata.durationMs.coerceAtLeast(1000L).toFloat()
                        val progress = (playbackPositionMs.toFloat() / dur).coerceIn(0f, 1f)
                        val zoomEffect = 1f + (effects.zoomInAmount / 100f * 0.3f * progress) -
                            (effects.zoomOutAmount / 100f * 0.2f * progress)
                        val s = (effects.scale * zoomEffect).coerceIn(0.5f, 2.5f)
                        scaleX = if (effects.flipHorizontal) -s else s
                        scaleY = if (effects.flipVertical) -s else s
                        rotationZ = effects.rotationDegrees
                    }
            ) {
                // Visual studio backdrop + ExoPlayer view
                PodcastStudioThumbnail(
                    modifier = Modifier.fillMaxSize(),
                    accentColor = PrimaryOrange
                )
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            player = exoPlayer
                        }
                    },
                    update = { view ->
                        if (view.player != exoPlayer) view.player = exoPlayer
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Color Grading / Brightness / Contrast Visual Tint Overlay
                if (effects.colorGradingPreset != ColorGradingPreset.NONE || effects.brightness != 0f || effects.temperature != 0f) {
                    val overlayColor = when (effects.colorGradingPreset) {
                        ColorGradingPreset.WARM -> PrimaryOrange.copy(alpha = 0.14f)
                        ColorGradingPreset.COOL -> Color(0xFF4FC3F7).copy(alpha = 0.14f)
                        ColorGradingPreset.CINEMATIC -> Color(0xFF00897B).copy(alpha = 0.12f)
                        ColorGradingPreset.BLACK_AND_WHITE -> Color.Black.copy(alpha = 0.35f)
                        else -> if (effects.temperature > 0) PrimaryOrange.copy(alpha = (effects.temperature / 400f).coerceIn(0f, 0.25f))
                        else Color(0xFF4FC3F7).copy(alpha = (-effects.temperature / 400f).coerceIn(0f, 0.25f))
                    }
                    Box(modifier = Modifier.fillMaxSize().background(overlayColor))
                }

                // Live Synchronized Animated Captions Overlay
                LiveCaptionOverlay(
                    positionMs = playbackPositionMs,
                    captionMode = project.captionMode,
                    segments = project.segments,
                    defaultStyle = project.captionStyle
                )
            }
        }

        // Playback Controls Bar (matching screenshot: 02:18 / 07:24 | |< > >| | [16:9] | Fullscreen)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatTimestampMs(playbackPositionMs)} / ${formatTimestampMs(project.videoMetadata.durationMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onSeekTo((playbackPositionMs - 1000L).coerceAtLeast(0L)) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Rewind 1s")
                }
                IconButton(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("editor_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = PrimaryOrange,
                        modifier = Modifier.size(34.dp)
                    )
                }
                IconButton(
                    onClick = { onSeekTo((playbackPositionMs + 1000L).coerceAtMost(project.videoMetadata.durationMs)) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Forward 1s")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box {
                    Surface(
                        onClick = { showRatioMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.border(1.dp, SilverGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = project.aspectRatioPreset.id,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    DropdownMenu(expanded = showRatioMenu, onDismissRequest = { showRatioMenu = false }) {
                        AspectRatioPreset.entries.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(if (isArabic) preset.labelAr else preset.labelEn) },
                                onClick = {
                                    onAspectRatioChange(preset)
                                    showRatioMenu = false
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = onReplaceVideo, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Replace Source Video", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggleFullscreen, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiTrackTimelineSection(
    isArabic: Boolean,
    project: StudioProject,
    playbackPositionMs: Long,
    timelineZoom: Float,
    onSeekTo: (Long) -> Unit,
    onZoomChange: (Float) -> Unit,
    onSplitAtPlayhead: () -> Unit,
    onTrimFirstClip: () -> Unit,
    onDeleteSelectedClip: () -> Unit
) {
    val durationMs = project.videoMetadata.durationMs.coerceAtLeast(3000L)
    val progress = (playbackPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepCharcoalDarker)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Timeline Action & Zoom Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onSplitAtPlayhead,
                        color = DarkSilver,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isArabic) "قص عند المؤشر" else "Split", style = MaterialTheme.typography.labelMedium, color = SoftIvory)
                        }
                    }
                    Surface(
                        onClick = onTrimFirstClip,
                        color = DarkSilver,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تقليص" else "Trim",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftIvory,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        onClick = onDeleteSelectedClip,
                        color = DarkSilver,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Clip",
                            tint = StudioError,
                            modifier = Modifier.padding(4.dp).size(15.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onZoomChange(timelineZoom - 0.25f) }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = SilverGray, modifier = Modifier.size(16.dp))
                    }
                    Text("${(timelineZoom * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = SilverGray)
                    IconButton(onClick = { onZoomChange(timelineZoom + 0.25f) }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = SilverGray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Interactive Playhead Scrubber
            Slider(
                value = progress,
                onValueChange = { frac -> onSeekTo((frac * durationMs).toLong()) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = PrimaryOrange,
                    inactiveTrackColor = DarkSilver
                ),
                modifier = Modifier.height(22.dp)
            )

            // 5 Multi-Track Lanes (1: Video, 2: Captions, 3: Original Audio Waveform, 4: Effects, 5: Music)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                val trackWidth = (330.dp * timelineZoom).coerceAtLeast(300.dp)
                Column(
                    modifier = Modifier.width(trackWidth),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Track 1: Video Clips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSilverSurface),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            tint = SilverGray,
                            modifier = Modifier.padding(horizontal = 6.dp).size(16.dp)
                        )
                        project.timelineSegments.forEach { clip ->
                            Box(
                                modifier = Modifier
                                    .weight(clip.durationMs.toFloat().coerceAtLeast(1f))
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StudioTealSlate)
                                    .border(1.dp, PrimaryOrange.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(clip.label, fontSize = 10.sp, color = SoftIvory, maxLines = 1)
                            }
                        }
                    }

                    // Track 2: Captions Track (Orange blocks synced to timestamps — matching screenshot)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSilverSurface)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.padding(horizontal = 2.dp).size(16.dp)
                        )
                        project.segments.forEach { seg ->
                            val isActive = playbackPositionMs in seg.startTimeMs..seg.endTimeMs
                            Surface(
                                onClick = { onSeekTo(seg.startTimeMs + 50L) },
                                color = if (isActive) PrimaryOrange else PrimaryOrangeDark.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight((seg.endTimeMs - seg.startTimeMs).toFloat().coerceAtLeast(200f))
                                    .height(26.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = seg.text,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Track 3: Original Audio Waveform Track (Teal waveform matching screenshot)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF173330)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF4DB6AC),
                            modifier = Modifier.padding(horizontal = 6.dp).size(15.dp)
                        )
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            val barCount = 48
                            val step = size.width / barCount
                            for (i in 0 until barCount) {
                                val hRatio = 0.25f + 0.7f * kotlin.math.abs(kotlin.math.sin(i * 0.65f))
                                val barH = size.height * hRatio
                                drawRoundRect(
                                    color = if (i.toFloat() / barCount <= progress) Color(0xFF4DB6AC) else Color(0xFF26615A),
                                    topLeft = Offset(i * step, (size.height - barH) / 2f),
                                    size = Size((step * 0.55f).coerceAtLeast(2f), barH),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                        }
                    }

                    // Track 4 & 5: Effects + Additional Audio Indicator Row
                    if (project.videoEffects != VideoEffectsConfig() || project.audioConfig.additionalClips.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (project.videoEffects.colorGradingPreset != ColorGradingPreset.NONE) {
                                Surface(color = DarkSilver, shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "FX: ${project.videoEffects.colorGradingPreset.labelAr}",
                                        fontSize = 10.sp,
                                        color = PrimaryOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            project.audioConfig.additionalClips.forEach { ac ->
                                Surface(color = Color(0xFF2E4C48), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "\uD83C\uDFB5 ${ac.name}",
                                        fontSize = 10.sp,
                                        color = SoftIvory,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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

@Composable
private fun CropAndFormatPanel(
    isArabic: Boolean,
    currentRatio: AspectRatioPreset,
    effects: VideoEffectsConfig,
    onSelectRatio: (AspectRatioPreset) -> Unit,
    onUpdateEffects: ((VideoEffectsConfig) -> VideoEffectsConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isArabic) "أبعاد الفيديو والقص التلقائي (Formats & Crop)" else "Aspect Ratio Presets & Crop",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AspectRatioPreset.entries) { preset ->
                val selected = currentRatio == preset
                FilterChip(
                    selected = selected,
                    onClick = { onSelectRatio(preset) },
                    label = { Text(if (isArabic) preset.labelAr else preset.labelEn) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        Text(
            text = if (isArabic) "تكبير الكادر (Scale): ${String.format("%.2fx", effects.scale)}" else "Frame Scale: ${String.format("%.2fx", effects.scale)}",
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = effects.scale,
            onValueChange = { s -> onUpdateEffects { it.copy(scale = s) } },
            valueRange = 0.5f..2.2f,
            colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
        )
    }
}

@Composable
private fun EditorBottomToolbar(
    isArabic: Boolean,
    selectedTool: EditorToolTab,
    onSelectTool: (EditorToolTab) -> Unit
) {
    val tools = listOf(
        EditorToolTab.CAPTIONS to Icons.Default.ClosedCaption,
        EditorToolTab.FONTS to Icons.Default.FontDownload,
        EditorToolTab.ANIMATION to Icons.Default.PlayArrow,
        EditorToolTab.AUDIO to Icons.Default.MusicNote,
        EditorToolTab.BACKGROUND to Icons.Default.GridOn,
        EditorToolTab.EFFECTS to Icons.Default.AutoAwesome,
        EditorToolTab.CROP to Icons.Default.ContentCut,
        EditorToolTab.AI_TOOLS to Icons.Default.Psychology
    )

    Surface(
        color = DeepCharcoalDarker,
        tonalElevation = 8.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tools) { (tab, icon) ->
                val selected = selectedTool == tab
                Surface(
                    onClick = { onSelectTool(tab) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) PrimaryOrange else Color.Transparent,
                    modifier = Modifier.testTag("toolbar_${tab.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab.labelEn,
                            tint = if (selected) Color.White else SilverGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isArabic) tab.labelAr else tab.labelEn,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else SilverGray,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Professional Export Center Screen (Section 14 — matching 'تصدير الفيديو' screenshot)
 */
@Composable
fun ExportCenterScreen(
    isArabic: Boolean,
    project: StudioProject,
    exportState: ExportProgressState,
    onBackToEditor: () -> Unit,
    onUpdateExportConfig: ((ExportConfig) -> ExportConfig) -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onResetExportState: () -> Unit
) {
    val cfg = project.exportConfig
    var fileNameDraft by remember(cfg.outputFileName) { mutableStateOf(cfg.outputFileName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("export_center_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBackToEditor) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isArabic) "تصدير الفيديو (MP4)" else "Export Video (MP4)",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Video Quality (1080p vs 720p)
                Text(
                    text = if (isArabic) "جودة الفيديو" else "Video Resolution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExportResolution.entries.reversed().forEach { res ->
                        val selected = cfg.resolution == res
                        Button(
                            onClick = { onUpdateExportConfig { it.copy(resolution = res) } },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) PrimaryOrange else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(if (res == ExportResolution.RES_1080P) "1080p" else "720p", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. Frame Rate (30 FPS vs 60 FPS)
                Text(
                    text = if (isArabic) "معدل الإطارات" else "Frame Rate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf(30, 60).forEach { fps ->
                        val selected = cfg.frameRate == fps
                        Button(
                            onClick = { onUpdateExportConfig { it.copy(frameRate = fps) } },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) PrimaryOrange else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("$fps FPS", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Bitrate & Audio Quality
                Text(
                    text = if (isArabic) "معدل البت (Bitrate): ${cfg.bitrateMbps} Mbps • صوت ${cfg.audioQualityKbps} kbps" else "Bitrate: ${cfg.bitrateMbps} Mbps • Audio ${cfg.audioQualityKbps} kbps",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = cfg.bitrateMbps.toFloat(),
                    onValueChange = { v -> onUpdateExportConfig { it.copy(bitrateMbps = v.toInt()) } },
                    valueRange = 6f..24f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
                )

                // 4. Output Filename & Destination
                Text(
                    text = if (isArabic) "اسم الملف" else "Output Filename",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = fileNameDraft,
                    onValueChange = {
                        fileNameDraft = it
                        onUpdateExportConfig { c -> c.copy(outputFileName = it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    suffix = { Text(".mp4") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = if (isArabic) "مجلد الحفظ: ${cfg.destinationFolder} (معرض الصور)" else "Destination: ${cfg.destinationFolder} (Android Gallery)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 5. Export Button & Real Rendering Progress
                when (exportState) {
                    is ExportProgressState.Idle -> {
                        Button(
                            onClick = onStartExport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("start_mp4_export_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                        ) {
                            Icon(Icons.Default.IosShare, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "تصدير الآن" else "Export Now",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    is ExportProgressState.Rendering -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LinearProgressIndicator(
                                progress = { exportState.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = PrimaryOrange
                            )
                            Text(
                                text = "${exportState.progressPercent}% — ${if (isArabic) exportState.statusAr else exportState.statusEn}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = onCancelExport,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isArabic) "إلغاء التصدير" else "Cancel Export", color = StudioError)
                            }
                        }
                    }

                    is ExportProgressState.Completed -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSuccess.copy(alpha = 0.16f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioSuccess)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "تم تصدير الفيديو بنجاح وحفظه في المعرض!" else "MP4 Export Completed & Saved to Gallery!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioSuccess
                                    )
                                }
                                Text(
                                    text = "${exportState.outputFilePath}\nSize: ${formatBytesSize(exportState.fileSizeBytes)}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Button(
                                    onClick = onResetExportState,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isArabic) "تصدير نسخة أخرى" else "Done")
                                }
                            }
                        }
                    }

                    is ExportProgressState.Failed -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioError.copy(alpha = 0.16f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isArabic) exportState.messageAr else exportState.messageEn,
                                    color = StudioError,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = onStartExport,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                                ) {
                                    Text(if (isArabic) "إعادة المحاولة" else "Retry Export")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
