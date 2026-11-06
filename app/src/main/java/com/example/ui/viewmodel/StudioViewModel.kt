package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.os.StatFs
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.*
import com.example.data.local.StudioDatabase
import com.example.data.repository.SettingsDataStore
import com.example.data.repository.StudioAppSettings
import com.example.data.repository.StudioProject
import com.example.data.repository.StudioRepository
import com.example.domain.model.*
import com.example.media.ExportProgressState
import com.example.media.RealMp4ExportEngine
import com.example.media.VideoMetadataExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private const val TAG = "StudioViewModel"

enum class MainNavSection(val labelAr: String, val labelEn: String) {
    HOME("الرئيسية", "Home"),
    PROJECTS("المشاريع", "Projects"),
    CREATE("إنشاء", "Create"),
    TEMPLATES("القوالب", "Templates"),
    SETTINGS("الإعدادات", "Settings"),
    EDITOR("تحرير الفيديو", "Video Editor"),
    EXPORT_CENTER("تصدير الفيديو", "Export Video")
}

data class EditorSnapshot(
    val captionMode: CaptionDisplayMode,
    val segments: List<CaptionSegment>,
    val captionStyle: CaptionStyle,
    val videoEffects: VideoEffectsConfig,
    val audioConfig: AudioTrackConfig,
    val timelineSegments: List<TimelineVideoSegment>,
    val aspectRatioPreset: AspectRatioPreset
)

data class StorageInfoState(
    val usedAppBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val freeDeviceBytes: Long = 0L,
    val totalDeviceBytes: Long = 1L
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext
    private val database = StudioDatabase.getInstance(context)
    private val repository = StudioRepository(database.studioDao())
    private val settingsDataStore = SettingsDataStore(context)
    private val aiSmartToolsEngine = AiSmartToolsEngine(context)

    val appSettings: StateFlow<StudioAppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioAppSettings())

    val allProjects: StateFlow<List<StudioProject>> = repository.allProjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customTemplates: StateFlow<List<CaptionTemplate>> = repository.customTemplatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentExports: StateFlow<List<RecentExportItem>> = repository.recentExportsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSection = MutableStateFlow(MainNavSection.HOME)
    val currentSection: StateFlow<MainNavSection> = _currentSection.asStateFlow()

    private val _activeProject = MutableStateFlow<StudioProject?>(null)
    val activeProject: StateFlow<StudioProject?> = _activeProject.asStateFlow()

    private val _interruptedProject = MutableStateFlow<StudioProject?>(null)
    val interruptedProject: StateFlow<StudioProject?> = _interruptedProject.asStateFlow()

    private val _selectedEditorTool = MutableStateFlow(EditorToolTab.CAPTIONS)
    val selectedEditorTool: StateFlow<EditorToolTab> = _selectedEditorTool.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(2180L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isFullscreenPreview = MutableStateFlow(false)
    val isFullscreenPreview: StateFlow<Boolean> = _isFullscreenPreview.asStateFlow()

    private val _timelineZoom = MutableStateFlow(1.0f) // 0.5f .. 3.0f
    val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

    private val _selectedTimelineClipId = MutableStateFlow<String?>(null)
    val selectedTimelineClipId: StateFlow<String?> = _selectedTimelineClipId.asStateFlow()

    private val _transcriptionLanguage = MutableStateFlow(TranscriptionLanguage.AUTO)
    val transcriptionLanguage: StateFlow<TranscriptionLanguage> = _transcriptionLanguage.asStateFlow()

    private val _transcriptionState = MutableStateFlow<TranscriptionProgress>(TranscriptionProgress.Idle)
    val transcriptionState: StateFlow<TranscriptionProgress> = _transcriptionState.asStateFlow()

    private val _aiSuggestion = MutableStateFlow<AiSmartSuggestion?>(null)
    val aiSuggestion: StateFlow<AiSmartSuggestion?> = _aiSuggestion.asStateFlow()

    private val _isAiToolLoading = MutableStateFlow(false)
    val isAiToolLoading: StateFlow<Boolean> = _isAiToolLoading.asStateFlow()

    private val _aiErrorBanner = MutableStateFlow<String?>(null)
    val aiErrorBanner: StateFlow<String?> = _aiErrorBanner.asStateFlow()

    private val _exportState = MutableStateFlow<ExportProgressState>(ExportProgressState.Idle)
    val exportState: StateFlow<ExportProgressState> = _exportState.asStateFlow()

    private val _projectSearchQuery = MutableStateFlow("")
    val projectSearchQuery: StateFlow<String> = _projectSearchQuery.asStateFlow()

    private val _sortProjectsAscending = MutableStateFlow(false)
    val sortProjectsAscending: StateFlow<Boolean> = _sortProjectsAscending.asStateFlow()

    private val _selectedTemplateCategory = MutableStateFlow(TemplateCategory.ALL)
    val selectedTemplateCategory: StateFlow<TemplateCategory> = _selectedTemplateCategory.asStateFlow()

    private val _storageInfo = MutableStateFlow(StorageInfoState())
    val storageInfo: StateFlow<StorageInfoState> = _storageInfo.asStateFlow()

    private val _statusToastMessage = MutableStateFlow<String?>(null)
    val statusToastMessage: StateFlow<String?> = _statusToastMessage.asStateFlow()

    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var exportJob: Job? = null
    private var transcriptionJob: Job? = null

    init {
        initializeSampleProjectsAndCheckRecovery()
        refreshStorageStats()
    }

    private fun initializeSampleProjectsAndCheckRecovery() {
        viewModelScope.launch {
            try {
                val sampleVideoFile = RealMp4ExportEngine.ensureSamplePodcastVideo(context)
                val sampleUri = Uri.fromFile(sampleVideoFile).toString()

                val count = repository.getProjectCount()
                if (count == 0) {
                    val podcastSegments = createDefaultArabicPodcastSegments()
                    val podcastProject = StudioProject(
                        id = "proj_podcast_episode",
                        title = "حلقة البودكاست — صناعة المحتوى",
                        videoMetadata = VideoMetadata(
                            uriString = sampleUri,
                            fileName = sampleVideoFile.name,
                            width = 1280,
                            height = 720,
                            durationMs = 8000L,
                            frameRate = 30f,
                            fileSizeBytes = sampleVideoFile.length()
                        ),
                        aspectRatioPreset = AspectRatioPreset.RATIO_16_9,
                        captionMode = CaptionDisplayMode.WORD_BY_WORD,
                        segments = podcastSegments,
                        captionStyle = CaptionTemplateCatalog.builtInTemplates.first().style,
                        videoEffects = VideoEffectsConfig(),
                        audioConfig = AudioTrackConfig(),
                        timelineSegments = listOf(
                            TimelineVideoSegment(
                                sourceStartMs = 0L,
                                sourceEndMs = 8000L,
                                timelineStartMs = 0L,
                                label = "حلقة البودكاست.mp4"
                            )
                        ),
                        exportConfig = ExportConfig(outputFileName = "حلقة_البودكاست_1080p")
                    )

                    val shortSegments = createDefaultBilingualShortSegments()
                    val shortProject = StudioProject(
                        id = "proj_short_reel",
                        title = "مقطع قصير — Reels & Shorts",
                        videoMetadata = VideoMetadata(
                            uriString = sampleUri,
                            fileName = sampleVideoFile.name,
                            width = 1080,
                            height = 1920,
                            durationMs = 8000L,
                            frameRate = 60f,
                            fileSizeBytes = sampleVideoFile.length()
                        ),
                        aspectRatioPreset = AspectRatioPreset.RATIO_9_16,
                        captionMode = CaptionDisplayMode.WORD_BY_WORD,
                        segments = shortSegments,
                        captionStyle = CaptionTemplateCatalog.builtInTemplates[4].style,
                        videoEffects = VideoEffectsConfig(
                            cropRatio = AspectRatioPreset.RATIO_9_16,
                            colorGradingPreset = ColorGradingPreset.CINEMATIC
                        ),
                        audioConfig = AudioTrackConfig(),
                        timelineSegments = listOf(
                            TimelineVideoSegment(
                                sourceStartMs = 0L,
                                sourceEndMs = 8000L,
                                timelineStartMs = 0L,
                                label = "مقطع_قصير.mp4"
                            )
                        ),
                        exportConfig = ExportConfig(outputFileName = "مقطع_ريلز_احترافي"),
                        lastModifiedMillis = System.currentTimeMillis() - 3600_000L
                    )

                    repository.saveProject(podcastProject)
                    repository.saveProject(shortProject)
                    _activeProject.value = podcastProject
                } else {
                    val interrupted = repository.getInterruptedSessionProject()
                    if (interrupted != null) {
                        _interruptedProject.value = interrupted
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing sample projects", e)
            }
        }
    }

    fun navigateTo(section: MainNavSection, defaultTool: EditorToolTab? = null) {
        if (defaultTool != null) {
            _selectedEditorTool.value = defaultTool
        }
        if (section == MainNavSection.EDITOR && _activeProject.value == null) {
            val firstProj = allProjects.value.firstOrNull()
            if (firstProj != null) {
                openProject(firstProj)
                return
            }
        }
        _currentSection.value = section
    }

    fun selectEditorTool(tool: EditorToolTab) {
        _selectedEditorTool.value = tool
        if (tool == EditorToolTab.EXPORT) {
            _currentSection.value = MainNavSection.EXPORT_CENTER
        }
    }

    fun dismissToast() {
        _statusToastMessage.value = null
    }

    fun showToast(msgAr: String, msgEn: String) {
        val isAr = appSettings.value.language == AppLanguage.ARABIC
        _statusToastMessage.value = if (isAr) msgAr else msgEn
    }

    // ==================== PROJECT & SESSION MANAGEMENT ====================

    fun startNewBlankProject(title: String = "") {
        viewModelScope.launch {
            val sampleFile = RealMp4ExportEngine.ensureSamplePodcastVideo(context)
            val settings = appSettings.value
            val defaultTpl = CaptionTemplateCatalog.builtInTemplates.firstOrNull {
                it.id == settings.defaultTemplateId
            } ?: CaptionTemplateCatalog.builtInTemplates.first()

            val newProj = StudioProject(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank {
                    if (settings.language == AppLanguage.ARABIC) "مشروع جديد ${allProjects.value.size + 1}"
                    else "New Project ${allProjects.value.size + 1}"
                },
                videoMetadata = VideoMetadata(
                    uriString = Uri.fromFile(sampleFile).toString(),
                    fileName = sampleFile.name,
                    width = 1920,
                    height = 1080,
                    durationMs = 8000L,
                    frameRate = settings.defaultFrameRate.toFloat(),
                    fileSizeBytes = sampleFile.length()
                ),
                aspectRatioPreset = settings.defaultAspectRatio,
                captionMode = defaultTpl.captionMode,
                segments = createDefaultArabicPodcastSegments(),
                captionStyle = defaultTpl.style,
                videoEffects = VideoEffectsConfig(cropRatio = settings.defaultAspectRatio),
                audioConfig = AudioTrackConfig(),
                timelineSegments = listOf(
                    TimelineVideoSegment(
                        sourceStartMs = 0L,
                        sourceEndMs = 8000L,
                        timelineStartMs = 0L,
                        label = "Main Clip"
                    )
                ),
                exportConfig = ExportConfig(
                    resolution = settings.defaultResolution,
                    frameRate = settings.defaultFrameRate,
                    outputFileName = "ASL_Studio_${System.currentTimeMillis() / 1000}"
                ),
                hasActiveSession = true
            )
            repository.saveProject(newProj, markSessionActive = true)
            undoStack.clear()
            redoStack.clear()
            updateUndoRedoFlags()
            _activeProject.value = newProj
            _playbackPositionMs.value = 500L
            _currentSection.value = MainNavSection.EDITOR
        }
    }

    fun openProject(project: StudioProject) {
        viewModelScope.launch {
            undoStack.clear()
            redoStack.clear()
            updateUndoRedoFlags()
            _activeProject.value = project
            _playbackPositionMs.value = project.segments.firstOrNull()?.startTimeMs ?: 0L
            repository.saveProject(project, markSessionActive = true)
            _interruptedProject.value = null
            _currentSection.value = MainNavSection.EDITOR
        }
    }

    fun dismissInterruptedSessionBanner() {
        viewModelScope.launch {
            _interruptedProject.value = null
            repository.clearActiveSessions()
        }
    }

    fun renameProject(projectId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.renameProject(projectId, newTitle)
            if (_activeProject.value?.id == projectId) {
                _activeProject.value = _activeProject.value?.copy(title = newTitle.trim())
            }
            showToast("تم تغيير اسم المشروع بنجاح", "Project renamed")
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            repository.duplicateProject(projectId)
            showToast("تم نسخ المشروع بنجاح", "Project duplicated")
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_activeProject.value?.id == projectId) {
                _activeProject.value = allProjects.value.firstOrNull { it.id != projectId }
            }
            showToast("تم حذف المشروع", "Project deleted")
        }
    }

    fun setProjectSearchQuery(query: String) {
        _projectSearchQuery.value = query
    }

    fun toggleProjectSortOrder() {
        _sortProjectsAscending.value = !_sortProjectsAscending.value
    }

    // ==================== COMPLETE VIDEO & AUDIO IMPORT (SAF) ====================

    fun importVideoFromUri(uri: Uri, replaceCurrentProjectVideo: Boolean = false) {
        viewModelScope.launch {
            try {
                // Take persistable read permission when available via SAF
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                val metadata = VideoMetadataExtractor.extractMetadata(context, uri)
                if (!metadata.isSupported) {
                    showToast(
                        metadata.errorMessage ?: "صيغة الفيديو غير مدعومة",
                        metadata.errorMessage ?: "Unsupported video format"
                    )
                    return@launch
                }

                if (replaceCurrentProjectVideo && _activeProject.value != null) {
                    pushUndoSnapshot()
                    val updated = _activeProject.value!!.copy(
                        videoMetadata = metadata,
                        timelineSegments = listOf(
                            TimelineVideoSegment(
                                sourceStartMs = 0L,
                                sourceEndMs = metadata.durationMs,
                                timelineStartMs = 0L,
                                label = metadata.fileName
                            )
                        )
                    )
                    commitProjectChange(updated)
                    showToast("تم استبدال الفيديو الأصلي مع الحفاظ على الكابشن والتأثيرات", "Source video replaced")
                } else {
                    val settings = appSettings.value
                    val defaultTpl = CaptionTemplateCatalog.builtInTemplates.firstOrNull {
                        it.id == settings.defaultTemplateId
                    } ?: CaptionTemplateCatalog.builtInTemplates.first()

                    val newProj = StudioProject(
                        id = UUID.randomUUID().toString(),
                        title = metadata.fileName.substringBeforeLast('.').ifBlank { "فيديو مستورد" },
                        videoMetadata = metadata,
                        aspectRatioPreset = settings.defaultAspectRatio,
                        captionMode = defaultTpl.captionMode,
                        segments = createDefaultArabicPodcastSegments(),
                        captionStyle = defaultTpl.style,
                        videoEffects = VideoEffectsConfig(cropRatio = settings.defaultAspectRatio),
                        audioConfig = AudioTrackConfig(),
                        timelineSegments = listOf(
                            TimelineVideoSegment(
                                sourceStartMs = 0L,
                                sourceEndMs = metadata.durationMs,
                                timelineStartMs = 0L,
                                label = metadata.fileName
                            )
                        ),
                        exportConfig = ExportConfig(
                            resolution = settings.defaultResolution,
                            frameRate = settings.defaultFrameRate,
                            outputFileName = metadata.fileName.substringBeforeLast('.').ifBlank { "ASL_Export" }
                        ),
                        hasActiveSession = true
                    )
                    repository.saveProject(newProj, markSessionActive = true)
                    undoStack.clear()
                    redoStack.clear()
                    updateUndoRedoFlags()
                    _activeProject.value = newProj
                    _playbackPositionMs.value = 0L
                    _currentSection.value = MainNavSection.EDITOR
                    showToast("تم استيراد الفيديو (${metadata.width}x${metadata.height})", "Video imported (${metadata.width}x${metadata.height})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Video import failed", e)
                showToast("تعذر استيراد الفيديو: ${e.localizedMessage}", "Failed to import video: ${e.localizedMessage}")
            }
        }
    }

    fun importAudioClipFromUri(uri: Uri, isSoundEffect: Boolean = false) {
        val current = _activeProject.value ?: return
        viewModelScope.launch {
            var name = if (isSoundEffect) "مؤثر صوتي.mp3" else "موسيقى خلفية.mp3"
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && idx >= 0) name = c.getString(idx) ?: name
                }
            }
            pushUndoSnapshot()
            val clip = AudioClip(
                name = name,
                uriString = uri.toString(),
                isSoundEffect = isSoundEffect,
                volume = if (isSoundEffect) 0.85f else 0.55f,
                trimStartMs = 0L,
                trimEndMs = current.videoMetadata.durationMs.coerceAtLeast(5000L),
                timelineStartMs = _playbackPositionMs.value
            )
            val updatedAudio = current.audioConfig.copy(
                additionalClips = current.audioConfig.additionalClips + clip
            )
            commitProjectChange(current.copy(audioConfig = updatedAudio))
            showToast("تمت إضافة الملف الصوتي: $name", "Added audio track: $name")
        }
    }

    // ==================== UNDO / REDO & AUTOSAVE ====================

    private fun pushUndoSnapshot() {
        val proj = _activeProject.value ?: return
        undoStack.addLast(
            EditorSnapshot(
                captionMode = proj.captionMode,
                segments = proj.segments,
                captionStyle = proj.captionStyle,
                videoEffects = proj.videoEffects,
                audioConfig = proj.audioConfig,
                timelineSegments = proj.timelineSegments,
                aspectRatioPreset = proj.aspectRatioPreset
            )
        )
        if (undoStack.size > 30) undoStack.removeFirst()
        redoStack.clear()
        updateUndoRedoFlags()
    }

    fun undo() {
        val proj = _activeProject.value ?: return
        if (undoStack.isEmpty()) return
        val prev = undoStack.removeLast()
        redoStack.addLast(
            EditorSnapshot(
                captionMode = proj.captionMode,
                segments = proj.segments,
                captionStyle = proj.captionStyle,
                videoEffects = proj.videoEffects,
                audioConfig = proj.audioConfig,
                timelineSegments = proj.timelineSegments,
                aspectRatioPreset = proj.aspectRatioPreset
            )
        )
        updateUndoRedoFlags()
        val restored = proj.copy(
            captionMode = prev.captionMode,
            segments = prev.segments,
            captionStyle = prev.captionStyle,
            videoEffects = prev.videoEffects,
            audioConfig = prev.audioConfig,
            timelineSegments = prev.timelineSegments,
            aspectRatioPreset = prev.aspectRatioPreset
        )
        commitProjectChange(restored)
    }

    fun redo() {
        val proj = _activeProject.value ?: return
        if (redoStack.isEmpty()) return
        val next = redoStack.removeLast()
        undoStack.addLast(
            EditorSnapshot(
                captionMode = proj.captionMode,
                segments = proj.segments,
                captionStyle = proj.captionStyle,
                videoEffects = proj.videoEffects,
                audioConfig = proj.audioConfig,
                timelineSegments = proj.timelineSegments,
                aspectRatioPreset = proj.aspectRatioPreset
            )
        )
        updateUndoRedoFlags()
        val restored = proj.copy(
            captionMode = next.captionMode,
            segments = next.segments,
            captionStyle = next.captionStyle,
            videoEffects = next.videoEffects,
            audioConfig = next.audioConfig,
            timelineSegments = next.timelineSegments,
            aspectRatioPreset = next.aspectRatioPreset
        )
        commitProjectChange(restored)
    }

    private fun updateUndoRedoFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun commitProjectChange(updated: StudioProject) {
        _activeProject.value = updated
        viewModelScope.launch {
            repository.saveProject(updated, markSessionActive = true)
        }
    }

    // ==================== PLAYBACK & TIMELINE OPERATIONS ====================

    fun setPlaybackPosition(positionMs: Long) {
        val maxDur = _activeProject.value?.videoMetadata?.durationMs ?: 10_000L
        _playbackPositionMs.value = positionMs.coerceIn(0L, maxDur)
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun toggleFullscreenPreview() {
        _isFullscreenPreview.value = !_isFullscreenPreview.value
    }

    fun setTimelineZoom(zoom: Float) {
        _timelineZoom.value = zoom.coerceIn(0.5f, 3.0f)
    }

    fun selectTimelineClip(clipId: String?) {
        _selectedTimelineClipId.value = clipId
    }

    fun setAspectRatioPreset(preset: AspectRatioPreset) {
        val proj = _activeProject.value ?: return
        pushUndoSnapshot()
        commitProjectChange(
            proj.copy(
                aspectRatioPreset = preset,
                videoEffects = proj.videoEffects.copy(cropRatio = preset)
            )
        )
    }

    /**
     * Split active video clip or caption segment at current playhead (Section 5B)
     */
    fun splitAtPlayhead() {
        val proj = _activeProject.value ?: return
        val pos = _playbackPositionMs.value
        pushUndoSnapshot()

        // 1. Check if playhead is inside a caption segment -> split sentence
        val segIndex = proj.segments.indexOfFirst { pos > it.startTimeMs + 250L && pos < it.endTimeMs - 250L }
        if (segIndex >= 0) {
            splitCaptionSegment(proj.segments[segIndex].id, pos)
            showToast("تم تقسيم الكابشن عند مؤشر التشغيل", "Split caption at playhead")
            return
        }

        // 2. Otherwise split video timeline clip at playhead
        val clipIdx = proj.timelineSegments.indexOfFirst {
            val end = it.timelineStartMs + it.durationMs
            pos > it.timelineStartMs + 300L && pos < end - 300L
        }
        if (clipIdx >= 0) {
            val target = proj.timelineSegments[clipIdx]
            val offsetInClip = pos - target.timelineStartMs
            val firstHalf = target.copy(
                id = UUID.randomUUID().toString(),
                sourceEndMs = target.sourceStartMs + offsetInClip,
                label = "${target.label} (1)"
            )
            val secondHalf = target.copy(
                id = UUID.randomUUID().toString(),
                sourceStartMs = target.sourceStartMs + offsetInClip,
                timelineStartMs = pos,
                label = "${target.label} (2)"
            )
            val newList = proj.timelineSegments.toMutableList().apply {
                removeAt(clipIdx)
                add(clipIdx, secondHalf)
                add(clipIdx, firstHalf)
            }
            commitProjectChange(proj.copy(timelineSegments = newList))
            showToast("تم قص مقطع الفيديو عند المؤشر", "Split video clip at playhead")
        } else {
            showToast("ضع المؤشر في منتصف مقطع أو جملة كابشن للقص", "Place playhead inside a clip or caption to split")
        }
    }

    fun trimVideoClip(clipId: String, newStartDeltaMs: Long, newEndDeltaMs: Long) {
        val proj = _activeProject.value ?: return
        pushUndoSnapshot()
        val updated = proj.timelineSegments.map { clip ->
            if (clip.id == clipId) {
                val s = (clip.sourceStartMs + newStartDeltaMs).coerceAtLeast(0L)
                val e = (clip.sourceEndMs + newEndDeltaMs).coerceAtLeast(s + 500L)
                clip.copy(sourceStartMs = s, sourceEndMs = e)
            } else clip
        }
        commitProjectChange(proj.copy(timelineSegments = updated))
    }

    fun deleteTimelineClip(clipId: String) {
        val proj = _activeProject.value ?: return
        if (proj.timelineSegments.size <= 1) {
            showToast("لا يمكن حذف المقطع الوحيد في الخط الزمني", "Cannot delete the only video clip on timeline")
            return
        }
        pushUndoSnapshot()
        val updated = proj.timelineSegments.filterNot { it.id == clipId }
        commitProjectChange(proj.copy(timelineSegments = updated))
        showToast("تم حذف المقطع من الخط الزمني", "Clip deleted from timeline")
    }

    // ==================== CAPTION EDITING & STYLING (SECTIONS 7, 8, 9, 10) ====================

    fun setCaptionDisplayMode(mode: CaptionDisplayMode) {
        val proj = _activeProject.value ?: return
        if (proj.captionMode == mode) return
        pushUndoSnapshot()
        commitProjectChange(proj.copy(captionMode = mode))
    }

    fun updateCaptionStyle(transform: (CaptionStyle) -> CaptionStyle, applyToSelectedOnly: Boolean = false) {
        val proj = _activeProject.value ?: return
        pushUndoSnapshot()
        if (applyToSelectedOnly && proj.segments.any { it.isSelected }) {
            val updatedSegments = proj.segments.map { seg ->
                if (seg.isSelected) {
                    val base = seg.styleOverride ?: proj.captionStyle
                    seg.copy(styleOverride = transform(base))
                } else seg
            }
            commitProjectChange(proj.copy(segments = updatedSegments))
        } else {
            val newGlobalStyle = transform(proj.captionStyle)
            // Clear overrides when applying to all captions so the change is immediately visible everywhere
            val clearedOverrides = proj.segments.map { it.copy(styleOverride = null) }
            commitProjectChange(proj.copy(captionStyle = newGlobalStyle, segments = clearedOverrides))
        }
    }

    fun updateCaptionSegmentText(segmentId: String, newText: String, newStartMs: Long? = null, newEndMs: Long? = null) {
        val proj = _activeProject.value ?: return
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        pushUndoSnapshot()
        val updated = proj.segments.map { seg ->
            if (seg.id == segmentId) {
                val sMs = newStartMs ?: seg.startTimeMs
                val eMs = (newEndMs ?: seg.endTimeMs).coerceAtLeast(sMs + 250L)
                val wordTokens = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
                val step = (eMs - sMs) / wordTokens.size.coerceAtLeast(1)
                val rebuiltWords = wordTokens.mapIndexed { idx, token ->
                    val existingWord = seg.words.getOrNull(idx)
                    CaptionWord(
                        id = existingWord?.id ?: UUID.randomUUID().toString(),
                        text = token,
                        startTimeMs = sMs + idx * step,
                        endTimeMs = if (idx == wordTokens.lastIndex) eMs else sMs + (idx + 1) * step,
                        isHighlighted = existingWord?.isHighlighted ?: false
                    )
                }
                seg.copy(text = trimmed, words = rebuiltWords, startTimeMs = sMs, endTimeMs = eMs)
            } else seg
        }
        commitProjectChange(proj.copy(segments = updated))
    }

    fun updateSingleWord(segmentId: String, wordId: String, newWordText: String, newStartMs: Long, newEndMs: Long, isHighlighted: Boolean) {
        val proj = _activeProject.value ?: return
        pushUndoSnapshot()
        val updated = proj.segments.map { seg ->
            if (seg.id == segmentId) {
                val updatedWords = seg.words.map { w ->
                    if (w.id == wordId) {
                        w.copy(
                            text = newWordText.trim().ifBlank { w.text },
                            startTimeMs = newStartMs.coerceAtLeast(0L),
                            endTimeMs = newEndMs.coerceAtLeast(newStartMs + 80L),
                            isHighlighted = isHighlighted
                        )
                    } else w
                }
                seg.copy(
                    text = updatedWords.joinToString(" ") { it.text },
                    words = updatedWords,
                    startTimeMs = updatedWords.firstOrNull()?.startTimeMs ?: seg.startTimeMs,
                    endTimeMs = updatedWords.lastOrNull()?.endTimeMs ?: seg.endTimeMs
                )
            } else seg
        }
        commitProjectChange(proj.copy(segments = updated))
    }

    fun addCaptionSegment(text: String) {
        val proj = _activeProject.value ?: return
        val clean = text.trim().ifBlank {
            if (appSettings.value.language == AppLanguage.ARABIC) "جملة كابشن جديدة" else "New caption sentence"
        }
        pushUndoSnapshot()
        val startMs = _playbackPositionMs.value
        val endMs = (startMs + 2200L).coerceAtMost(proj.videoMetadata.durationMs.coerceAtLeast(startMs + 1000L))
        val tokens = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        val step = (endMs - startMs) / tokens.size.coerceAtLeast(1)
        val words = tokens.mapIndexed { idx, t ->
            CaptionWord(
                text = t,
                startTimeMs = startMs + idx * step,
                endTimeMs = startMs + (idx + 1) * step
            )
        }
        val newSeg = CaptionSegment(
            text = clean,
            words = words,
            startTimeMs = startMs,
            endTimeMs = endMs
        )
        val sorted = (proj.segments + newSeg).sortedBy { it.startTimeMs }
        commitProjectChange(proj.copy(segments = sorted))
    }

    fun deleteCaptionSegment(segmentId: String) {
        val proj = _activeProject.value ?: return
        pushUndoSnapshot()
        commitProjectChange(proj.copy(segments = proj.segments.filterNot { it.id == segmentId }))
    }

    fun duplicateCaptionSegment(segmentId: String) {
        val proj = _activeProject.value ?: return
        val target = proj.segments.firstOrNull { it.id == segmentId } ?: return
        pushUndoSnapshot()
        val dur = (target.endTimeMs - target.startTimeMs).coerceAtLeast(500L)
        val newStart = target.endTimeMs + 100L
        val newEnd = newStart + dur
        val shiftedWords = target.words.map { w ->
            val offset = w.startTimeMs - target.startTimeMs
            val wDur = w.endTimeMs - w.startTimeMs
            w.copy(
                id = UUID.randomUUID().toString(),
                startTimeMs = newStart + offset,
                endTimeMs = newStart + offset + wDur
            )
        }
        val copy = target.copy(
            id = UUID.randomUUID().toString(),
            words = shiftedWords,
            startTimeMs = newStart,
            endTimeMs = newEnd
        )
        val sorted = (proj.segments + copy).sortedBy { it.startTimeMs }
        commitProjectChange(proj.copy(segments = sorted))
    }

    fun splitCaptionSegment(segmentId: String, splitTimeMs: Long? = null) {
        val proj = _activeProject.value ?: return
        val idx = proj.segments.indexOfFirst { it.id == segmentId }
        if (idx < 0) return
        val seg = proj.segments[idx]
        if (seg.words.size < 2) {
            showToast("تحتاج الجملة إلى كلمتين على الأقل لتقسيمها", "Sentence needs at least 2 words to split")
            return
        }
        pushUndoSnapshot()
        val splitIdx = if (splitTimeMs != null) {
            seg.words.indexOfFirst { it.startTimeMs >= splitTimeMs }.takeIf { it in 1 until seg.words.size }
                ?: (seg.words.size / 2)
        } else {
            seg.words.size / 2
        }
        val firstWords = seg.words.subList(0, splitIdx)
        val secondWords = seg.words.subList(splitIdx, seg.words.size)

        val seg1 = seg.copy(
            id = UUID.randomUUID().toString(),
            text = firstWords.joinToString(" ") { it.text },
            words = firstWords,
            startTimeMs = firstWords.first().startTimeMs,
            endTimeMs = firstWords.last().endTimeMs
        )
        val seg2 = seg.copy(
            id = UUID.randomUUID().toString(),
            text = secondWords.joinToString(" ") { it.text },
            words = secondWords,
            startTimeMs = secondWords.first().startTimeMs,
            endTimeMs = secondWords.last().endTimeMs
        )
        val updated = proj.segments.toMutableList().apply {
            removeAt(idx)
            add(idx, seg2)
            add(idx, seg1)
        }
        commitProjectChange(proj.copy(segments = updated))
    }

    fun mergeWithNextCaptionSegment(segmentId: String) {
        val proj = _activeProject.value ?: return
        val idx = proj.segments.indexOfFirst { it.id == segmentId }
        if (idx < 0 || idx >= proj.segments.lastIndex) {
            showToast("لا توجد جملة تالية لدمجها مع هذه الجملة", "No next sentence available to merge")
            return
        }
        pushUndoSnapshot()
        val first = proj.segments[idx]
        val second = proj.segments[idx + 1]
        val combinedWords = first.words + second.words
        val merged = first.copy(
            id = UUID.randomUUID().toString(),
            text = "${first.text} ${second.text}".trim(),
            words = combinedWords,
            startTimeMs = first.startTimeMs,
            endTimeMs = second.endTimeMs
        )
        val updated = proj.segments.toMutableList().apply {
            removeAt(idx + 1)
            removeAt(idx)
            add(idx, merged)
        }
        commitProjectChange(proj.copy(segments = updated))
    }

    fun toggleSelectCaptionSegment(segmentId: String) {
        val proj = _activeProject.value ?: return
        val updated = proj.segments.map {
            if (it.id == segmentId) it.copy(isSelected = !it.isSelected) else it
        }
        _activeProject.value = proj.copy(segments = updated)
    }

    // ==================== TEMPLATES MANAGEMENT (SECTION 10) ====================

    fun selectTemplateCategory(category: TemplateCategory) {
        _selectedTemplateCategory.value = category
    }

    fun applyCaptionTemplate(template: CaptionTemplate, applyToSelectedOnly: Boolean = false) {
        val proj = _activeProject.value
        if (proj == null) {
            startNewBlankProject()
        }
        val active = _activeProject.value ?: return
        pushUndoSnapshot()
        if (applyToSelectedOnly && active.segments.any { it.isSelected }) {
            val updatedSegs = active.segments.map { seg ->
                if (seg.isSelected) seg.copy(styleOverride = template.style) else seg
            }
            commitProjectChange(active.copy(segments = updatedSegs))
            showToast("تم تطبيق القالب (${template.nameAr}) على الجمل المحددة", "Applied template (${template.nameEn}) to selected captions")
        } else {
            val clearedSegs = active.segments.map { it.copy(styleOverride = null) }
            commitProjectChange(
                active.copy(
                    captionMode = template.captionMode,
                    captionStyle = template.style,
                    segments = clearedSegs
                )
            )
            showToast("تم تطبيق القالب: ${template.nameAr}", "Applied template: ${template.nameEn}")
        }
    }

    fun saveCurrentStyleAsCustomTemplate(nameAr: String, nameEn: String) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            repository.saveCustomTemplate(
                nameAr = nameAr,
                nameEn = nameEn,
                captionMode = proj.captionMode,
                style = proj.captionStyle
            )
            showToast("تم حفظ القالب المخصص في مكتبة القوالب", "Saved custom template to library")
        }
    }

    fun deleteCustomTemplate(templateId: String) {
        viewModelScope.launch {
            repository.deleteCustomTemplate(templateId)
            showToast("تم حذف القالب المخصص", "Deleted custom template")
        }
    }

    // ==================== VIDEO EFFECTS & AUDIO STUDIO (SECTIONS 11 & 12) ====================

    fun updateVideoEffects(transform: (VideoEffectsConfig) -> VideoEffectsConfig) {
        val proj = _activeProject.value ?: return
        pushUndoSnapshot()
        commitProjectChange(proj.copy(videoEffects = transform(proj.videoEffects)))
    }

    fun resetVideoEffects() {
        val proj = _activeProject.value ?: return
        pushUndoSnapshot()
        commitProjectChange(proj.copy(videoEffects = VideoEffectsConfig(cropRatio = proj.aspectRatioPreset)))
        showToast("تم إعادة ضبط جميع تأثيرات الفيديو", "Reset all video effects")
    }

    fun updateAudioConfig(transform: (AudioTrackConfig) -> AudioTrackConfig) {
        val proj = _activeProject.value ?: return
        pushUndoSnapshot()
        commitProjectChange(proj.copy(audioConfig = transform(proj.audioConfig)))
    }

    fun removeAdditionalAudioClip(clipId: String) {
        val proj = _activeProject.value ?: return
        pushUndoSnapshot()
        val updated = proj.audioConfig.copy(
            additionalClips = proj.audioConfig.additionalClips.filterNot { it.id == clipId }
        )
        commitProjectChange(proj.copy(audioConfig = updated))
    }

    // ==================== AI TRANSCRIPTION & SMART TOOLS (SECTIONS 6 & 17) ====================

    fun setTranscriptionLanguage(language: TranscriptionLanguage) {
        _transcriptionLanguage.value = language
    }

    fun runAutomaticTranscription() {
        val proj = _activeProject.value ?: return
        val settings = appSettings.value
        if (!settings.hasAcceptedAiCloudConsent && settings.speechProvider == SpeechProviderType.FIREBASE_GEMINI) {
            _transcriptionState.value = TranscriptionProgress.Error(
                messageAr = "يرجى الموافقة على معالجة الصوت سحابياً في الإعدادات لتفعيل التفريغ التلقائي بالذكاء الاصطناعي.",
                messageEn = "Please enable AI cloud processing consent in Settings to run automatic transcription."
            )
            return
        }

        val provider: SpeechRecognitionProvider = when (settings.speechProvider) {
            SpeechProviderType.FIREBASE_GEMINI -> FirebaseGeminiSpeechProvider()
            SpeechProviderType.WHISPER_ENDPOINT -> WhisperCompatibleSpeechProvider(settings.whisperEndpointUrl)
            SpeechProviderType.LOCAL_ANDROID_SPEECH -> LocalAndroidSpeechProvider()
        }

        transcriptionJob?.cancel()
        transcriptionJob = viewModelScope.launch {
            _aiErrorBanner.value = null
            _transcriptionState.value = TranscriptionProgress.Processing(
                stepIndex = 1,
                totalSteps = 4,
                statusAr = "جاري بدء توليد الكابشن التلقائي بالذكاء الاصطناعي...",
                statusEn = "Starting AI automatic caption generation..."
            )
            try {
                val result = provider.transcribeVideo(
                    context = context,
                    videoUri = Uri.parse(proj.videoMetadata.uriString),
                    durationMs = proj.videoMetadata.durationMs,
                    language = _transcriptionLanguage.value,
                    onProgress = { progress ->
                        _transcriptionState.value = progress
                    }
                )
                pushUndoSnapshot()
                commitProjectChange(proj.copy(segments = result.segments))
                _transcriptionState.value = TranscriptionProgress.Success(result)
                showToast(
                    "تم توليد ${result.segments.size} جملة كابشن متزامنة بنجاح (${result.detectedLanguageCode})",
                    "Generated ${result.segments.size} synchronized caption segments (${result.detectedLanguageCode})"
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Transcription timed out", e)
                _transcriptionState.value = TranscriptionProgress.Error(
                    messageAr = "انتهت مهلة الاتصال بمزود الذكاء الاصطناعي بعد 60 ثانية. يرجى المحاولة مرة أخرى.",
                    messageEn = "AI transcription timed out after 60 seconds. Please retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed", e)
                _transcriptionState.value = TranscriptionProgress.Error(
                    messageAr = e.localizedMessage ?: "فشل توليد الكابشن التلقائي.",
                    messageEn = e.localizedMessage ?: "Automatic transcription failed."
                )
            }
        }
    }

    fun resetTranscriptionState() {
        _transcriptionState.value = TranscriptionProgress.Idle
    }

    fun runSmartAiTool(toolIndex: Int) {
        val proj = _activeProject.value ?: return
        if (toolIndex == 1) {
            runAutomaticTranscription()
            return
        }
        viewModelScope.launch {
            _isAiToolLoading.value = true
            _aiErrorBanner.value = null
            try {
                val suggestion = when (toolIndex) {
                    2 -> aiSmartToolsEngine.detectLanguage(proj.segments)
                    3 -> aiSmartToolsEngine.improveAndPunctuateTranscript(proj.segments, punctuationOnly = false)
                    4 -> aiSmartToolsEngine.improveAndPunctuateTranscript(proj.segments, punctuationOnly = true)
                    5 -> aiSmartToolsEngine.smartSentenceSegmentation(proj.segments)
                    6 -> aiSmartToolsEngine.analyzeAndRemoveLongSilences(proj.segments)
                    7 -> aiSmartToolsEngine.detectSceneChanges(
                        context = context,
                        videoUri = Uri.parse(proj.videoMetadata.uriString),
                        durationMs = proj.videoMetadata.durationMs
                    )
                    8 -> aiSmartToolsEngine.suggestCaptionTemplate(proj.segments)
                    9 -> aiSmartToolsEngine.suggestHighlightWords(proj.segments)
                    10 -> aiSmartToolsEngine.generateVideoDescription(proj.segments)
                    else -> aiSmartToolsEngine.detectLanguage(proj.segments)
                }
                _aiSuggestion.value = suggestion
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI smart tool timed out", e)
                _aiErrorBanner.value = if (appSettings.value.language == AppLanguage.ARABIC) {
                    "انتهت مهلة أداة الذكاء الاصطناعي (60 ثانية). يرجى المحاولة مجدداً."
                } else {
                    "AI tool timed out after 60 seconds. Please try again."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI smart tool error", e)
                _aiErrorBanner.value = e.localizedMessage ?: "AI smart tool failed."
            } finally {
                _isAiToolLoading.value = false
            }
        }
    }

    fun applyAiSuggestion(suggestion: AiSmartSuggestion) {
        val proj = _activeProject.value ?: return
        pushUndoSnapshot()
        var updated = proj
        if (suggestion.updatedSegments != null) {
            updated = updated.copy(segments = suggestion.updatedSegments)
        }
        if (!suggestion.suggestedTemplateId.isNullOrBlank()) {
            val tpl = CaptionTemplateCatalog.builtInTemplates.firstOrNull { it.id == suggestion.suggestedTemplateId }
            if (tpl != null) {
                updated = updated.copy(captionStyle = tpl.style, captionMode = tpl.captionMode)
            }
        }
        commitProjectChange(updated)
        _aiSuggestion.value = null
        showToast("تم تطبيق تحسين الذكاء الاصطناعي بنجاح", "Applied AI improvement")
    }

    fun dismissAiSuggestion() {
        _aiSuggestion.value = null
    }

    // ==================== REAL 1080p MP4 EXPORT CENTER (SECTION 14) ====================

    fun updateExportConfig(transform: (ExportConfig) -> ExportConfig) {
        val proj = _activeProject.value ?: return
        commitProjectChange(proj.copy(exportConfig = transform(proj.exportConfig)))
    }

    fun startRealVideoExport() {
        val proj = _activeProject.value ?: return
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _exportState.value = ExportProgressState.Rendering(
                progressPercent = 1,
                currentFrame = 0,
                totalFrames = 100,
                statusAr = "جاري تهيئة محرك الرندر وحرق الكابشن...",
                statusEn = "Initializing MP4 hardware encoder & caption burner..."
            )
            val result = RealMp4ExportEngine.exportProjectToMp4(
                context = context,
                videoMetadata = proj.videoMetadata,
                aspectRatioPreset = proj.aspectRatioPreset,
                captionMode = proj.captionMode,
                segments = proj.segments,
                captionStyle = proj.captionStyle,
                videoEffects = proj.videoEffects,
                audioConfig = proj.audioConfig,
                exportConfig = proj.exportConfig,
                onProgress = { renderingState ->
                    _exportState.value = renderingState
                }
            )
            _exportState.value = result
            if (result is ExportProgressState.Completed) {
                repository.recordRecentExport(
                    RecentExportItem(
                        projectName = proj.title,
                        outputPath = result.outputFilePath,
                        resolutionLabel = proj.exportConfig.resolution.label,
                        frameRate = proj.exportConfig.frameRate,
                        fileSizeBytes = result.fileSizeBytes
                    )
                )
                refreshStorageStats()
            }
        }
    }

    fun cancelVideoExport() {
        exportJob?.cancel()
        exportJob = null
        _exportState.value = ExportProgressState.Idle
        showToast("تم إلغاء عملية التصدير", "Export cancelled")
    }

    fun resetExportState() {
        _exportState.value = ExportProgressState.Idle
    }

    // ==================== SETTINGS & STORAGE MANAGEMENT (SECTION 18) ====================

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsDataStore.setLanguage(language) }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch { settingsDataStore.setDarkTheme(isDark) }
    }

    fun setDefaultExportResolution(res: ExportResolution) {
        viewModelScope.launch { settingsDataStore.setDefaultResolution(res) }
    }

    fun setDefaultFrameRate(fps: Int) {
        viewModelScope.launch { settingsDataStore.setDefaultFrameRate(fps) }
    }

    fun setDefaultAspectRatio(ratio: AspectRatioPreset) {
        viewModelScope.launch { settingsDataStore.setDefaultAspectRatio(ratio) }
    }

    fun setSpeechProvider(provider: SpeechProviderType) {
        viewModelScope.launch { settingsDataStore.setSpeechProvider(provider) }
    }

    fun setWhisperEndpointUrl(url: String) {
        viewModelScope.launch { settingsDataStore.setWhisperEndpointUrl(url) }
    }

    fun setAiCloudConsent(accepted: Boolean) {
        viewModelScope.launch { settingsDataStore.setAiCloudConsent(accepted) }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            runCatching {
                val stat = StatFs(context.filesDir.absolutePath)
                val free = stat.availableBlocksLong * stat.blockSizeLong
                val total = stat.blockCountLong * stat.blockSizeLong
                val cacheSize = calculateFolderSize(context.cacheDir)
                val filesSize = calculateFolderSize(context.filesDir) +
                    (context.getExternalFilesDir(null)?.let { calculateFolderSize(it) } ?: 0L)
                _storageInfo.value = StorageInfoState(
                    usedAppBytes = filesSize,
                    cacheBytes = cacheSize,
                    freeDeviceBytes = free,
                    totalDeviceBytes = total.coerceAtLeast(1L)
                )
            }
        }
    }

    fun clearAppCache() {
        viewModelScope.launch {
            runCatching {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                refreshStorageStats()
                showToast("تم تنظيف الذاكرة المؤقتة بنجاح", "Cache cleared")
            }
        }
    }

    private fun calculateFolderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var result = 0L
        dir.walkTopDown().forEach { f ->
            if (f.isFile) result += f.length()
        }
        return result
    }

    companion object {
        fun createDefaultArabicPodcastSegments(): List<CaptionSegment> {
            return listOf(
                CaptionSegment(
                    id = "seg_pod_1",
                    text = "في هذه الحلقة سنتحدث عن أهمية صناعة المحتوى",
                    startTimeMs = 0L,
                    endTimeMs = 2800L,
                    words = listOf(
                        CaptionWord(text = "في", startTimeMs = 0L, endTimeMs = 320L),
                        CaptionWord(text = "هذه", startTimeMs = 320L, endTimeMs = 700L),
                        CaptionWord(text = "الحلقة", startTimeMs = 700L, endTimeMs = 1180L),
                        CaptionWord(text = "سنتحدث", startTimeMs = 1180L, endTimeMs = 1650L),
                        CaptionWord(text = "عن", startTimeMs = 1650L, endTimeMs = 1900L),
                        CaptionWord(text = "أهمية", startTimeMs = 1900L, endTimeMs = 2320L, isHighlighted = true),
                        CaptionWord(text = "صناعة", startTimeMs = 2320L, endTimeMs = 2580L, isHighlighted = true),
                        CaptionWord(text = "المحتوى", startTimeMs = 2580L, endTimeMs = 2800L, isHighlighted = true)
                    )
                ),
                CaptionSegment(
                    id = "seg_pod_2",
                    text = "وكيف يغير الذكاء الاصطناعي مستقبل المونتاج الرقمي",
                    startTimeMs = 2900L,
                    endTimeMs = 5600L,
                    words = listOf(
                        CaptionWord(text = "وكيف", startTimeMs = 2900L, endTimeMs = 3300L),
                        CaptionWord(text = "يغير", startTimeMs = 3300L, endTimeMs = 3700L),
                        CaptionWord(text = "الذكاء", startTimeMs = 3700L, endTimeMs = 4150L, isHighlighted = true),
                        CaptionWord(text = "الاصطناعي", startTimeMs = 4150L, endTimeMs = 4650L, isHighlighted = true),
                        CaptionWord(text = "مستقبل", startTimeMs = 4650L, endTimeMs = 5050L),
                        CaptionWord(text = "المونتاج", startTimeMs = 5050L, endTimeMs = 5350L),
                        CaptionWord(text = "الرقمي", startTimeMs = 5350L, endTimeMs = 5600L)
                    )
                ),
                CaptionSegment(
                    id = "seg_pod_3",
                    text = "بدقة عالية ومزامنة فورية لكل كلمة منطوقة",
                    startTimeMs = 5700L,
                    endTimeMs = 8000L,
                    words = listOf(
                        CaptionWord(text = "بدقة", startTimeMs = 5700L, endTimeMs = 6050L),
                        CaptionWord(text = "عالية", startTimeMs = 6050L, endTimeMs = 6400L),
                        CaptionWord(text = "ومزامنة", startTimeMs = 6400L, endTimeMs = 6900L, isHighlighted = true),
                        CaptionWord(text = "فورية", startTimeMs = 6900L, endTimeMs = 7280L),
                        CaptionWord(text = "لكل", startTimeMs = 7280L, endTimeMs = 7550L),
                        CaptionWord(text = "كلمة", startTimeMs = 7550L, endTimeMs = 7780L),
                        CaptionWord(text = "منطوقة", startTimeMs = 7780L, endTimeMs = 8000L)
                    )
                )
            )
        }

        fun createDefaultBilingualShortSegments(): List<CaptionSegment> {
            return listOf(
                CaptionSegment(
                    id = "seg_short_1",
                    text = "اصنع فيديوهات احترافية بكابشن ذكي وتأثيرات مميزة",
                    startTimeMs = 0L,
                    endTimeMs = 4000L,
                    words = listOf(
                        CaptionWord(text = "اصنع", startTimeMs = 0L, endTimeMs = 550L),
                        CaptionWord(text = "فيديوهات", startTimeMs = 550L, endTimeMs = 1250L),
                        CaptionWord(text = "احترافية", startTimeMs = 1250L, endTimeMs = 1950L, isHighlighted = true),
                        CaptionWord(text = "بكابشن", startTimeMs = 1950L, endTimeMs = 2600L, isHighlighted = true),
                        CaptionWord(text = "ذكي", startTimeMs = 2600L, endTimeMs = 3200L),
                        CaptionWord(text = "وتأثيرات", startTimeMs = 3200L, endTimeMs = 3650L),
                        CaptionWord(text = "مميزة", startTimeMs = 3650L, endTimeMs = 4000L)
                    )
                ),
                CaptionSegment(
                    id = "seg_short_2",
                    text = "Create viral social videos with synchronized AI captions",
                    startTimeMs = 4100L,
                    endTimeMs = 8000L,
                    words = listOf(
                        CaptionWord(text = "Create", startTimeMs = 4100L, endTimeMs = 4600L),
                        CaptionWord(text = "viral", startTimeMs = 4600L, endTimeMs = 5100L, isHighlighted = true),
                        CaptionWord(text = "social", startTimeMs = 5100L, endTimeMs = 5600L),
                        CaptionWord(text = "videos", startTimeMs = 5600L, endTimeMs = 6150L),
                        CaptionWord(text = "with", startTimeMs = 6150L, endTimeMs = 6550L),
                        CaptionWord(text = "synchronized", startTimeMs = 6550L, endTimeMs = 7200L, isHighlighted = true),
                        CaptionWord(text = "AI", startTimeMs = 7200L, endTimeMs = 7550L, isHighlighted = true),
                        CaptionWord(text = "captions", startTimeMs = 7550L, endTimeMs = 8000L)
                    )
                )
            )
        }
    }
}
