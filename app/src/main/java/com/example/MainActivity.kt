package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.domain.model.AppLanguage
import com.example.domain.model.EditorToolTab
import com.example.ui.components.StudioBottomNavigationBar
import com.example.ui.screens.*
import com.example.ui.theme.ASLCaptionsStudioTheme
import com.example.ui.viewmodel.MainNavSection
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // STEP 2: Initialize UI
        setContent {
            AslCaptionsStudioApp()
        }
    }
}

@Composable
fun AslCaptionsStudioApp(
    studioViewModel: StudioViewModel = viewModel()
) {
    val settings by studioViewModel.appSettings.collectAsStateWithLifecycle()
    val currentSection by studioViewModel.currentSection.collectAsStateWithLifecycle()
    val allProjects by studioViewModel.allProjects.collectAsStateWithLifecycle()
    val activeProject by studioViewModel.activeProject.collectAsStateWithLifecycle()
    val interruptedProject by studioViewModel.interruptedProject.collectAsStateWithLifecycle()
    val customTemplates by studioViewModel.customTemplates.collectAsStateWithLifecycle()
    val recentExports by studioViewModel.recentExports.collectAsStateWithLifecycle()
    val selectedEditorTool by studioViewModel.selectedEditorTool.collectAsStateWithLifecycle()
    val playbackPositionMs by studioViewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val isPlaying by studioViewModel.isPlaying.collectAsStateWithLifecycle()
    val isFullscreen by studioViewModel.isFullscreenPreview.collectAsStateWithLifecycle()
    val timelineZoom by studioViewModel.timelineZoom.collectAsStateWithLifecycle()
    val canUndo by studioViewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by studioViewModel.canRedo.collectAsStateWithLifecycle()
    val transcriptionLanguage by studioViewModel.transcriptionLanguage.collectAsStateWithLifecycle()
    val transcriptionState by studioViewModel.transcriptionState.collectAsStateWithLifecycle()
    val isAiToolLoading by studioViewModel.isAiToolLoading.collectAsStateWithLifecycle()
    val aiErrorBanner by studioViewModel.aiErrorBanner.collectAsStateWithLifecycle()
    val aiSuggestion by studioViewModel.aiSuggestion.collectAsStateWithLifecycle()
    val exportState by studioViewModel.exportState.collectAsStateWithLifecycle()
    val projectSearchQuery by studioViewModel.projectSearchQuery.collectAsStateWithLifecycle()
    val sortProjectsAscending by studioViewModel.sortProjectsAscending.collectAsStateWithLifecycle()
    val selectedTemplateCategory by studioViewModel.selectedTemplateCategory.collectAsStateWithLifecycle()
    val storageInfo by studioViewModel.storageInfo.collectAsStateWithLifecycle()
    val toastMessage by studioViewModel.statusToastMessage.collectAsStateWithLifecycle()

    val isArabic = settings.language == AppLanguage.ARABIC
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        if (!toastMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(toastMessage!!)
            studioViewModel.dismissToast()
        }
    }

    // Android Storage Access Framework Launchers (Sections 4 & 12)
    val importNewVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            studioViewModel.importVideoFromUri(uri, replaceCurrentProjectVideo = false)
        }
    }

    val replaceVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            studioViewModel.importVideoFromUri(uri, replaceCurrentProjectVideo = true)
        }
    }

    val importMusicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            studioViewModel.importAudioClipFromUri(uri, isSoundEffect = false)
        }
    }

    val importSfxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            studioViewModel.importAudioClipFromUri(uri, isSoundEffect = true)
        }
    }

    // Handle Back navigation from secondary screens
    BackHandler(enabled = currentSection != MainNavSection.HOME) {
        when (currentSection) {
            MainNavSection.EXPORT_CENTER -> studioViewModel.navigateTo(MainNavSection.EDITOR)
            else -> studioViewModel.navigateTo(MainNavSection.HOME)
        }
    }

    ASLCaptionsStudioTheme(
        darkTheme = settings.isDarkTheme,
        isRtl = settings.language.isRtl
    ) {
        val showBottomBar = currentSection != MainNavSection.EDITOR &&
            currentSection != MainNavSection.EXPORT_CENTER

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    StudioBottomNavigationBar(
                        currentSection = currentSection,
                        isArabic = isArabic,
                        onSelectSection = { studioViewModel.navigateTo(it) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentSection) {
                    MainNavSection.HOME -> HomeScreen(
                        isArabic = isArabic,
                        projects = allProjects,
                        interruptedProject = interruptedProject,
                        recentExports = recentExports,
                        storageInfo = storageInfo,
                        onStartNewProject = { studioViewModel.startNewBlankProject() },
                        onImportVideoClick = { importNewVideoLauncher.launch("video/*") },
                        onOpenAiToolsClick = {
                            studioViewModel.navigateTo(MainNavSection.EDITOR, defaultTool = EditorToolTab.AI_TOOLS)
                        },
                        onOpenProject = { studioViewModel.openProject(it) },
                        onDismissInterruptedProject = { studioViewModel.dismissInterruptedSessionBanner() },
                        onOpenAllProjects = { studioViewModel.navigateTo(MainNavSection.PROJECTS) },
                        onSelectQuickTool = { tool ->
                            if (tool == EditorToolTab.EXPORT) {
                                studioViewModel.navigateTo(MainNavSection.EXPORT_CENTER)
                            } else {
                                studioViewModel.navigateTo(MainNavSection.EDITOR, defaultTool = tool)
                            }
                        },
                        onApplyFeaturedTemplate = { tpl ->
                            studioViewModel.applyCaptionTemplate(tpl)
                            studioViewModel.navigateTo(MainNavSection.EDITOR)
                        }
                    )

                    MainNavSection.PROJECTS -> ProjectsScreen(
                        isArabic = isArabic,
                        projects = allProjects,
                        searchQuery = projectSearchQuery,
                        sortAscending = sortProjectsAscending,
                        onSearchQueryChange = { studioViewModel.setProjectSearchQuery(it) },
                        onToggleSortOrder = { studioViewModel.toggleProjectSortOrder() },
                        onCreateNewProject = { studioViewModel.startNewBlankProject() },
                        onOpenProject = { studioViewModel.openProject(it) },
                        onRenameProject = { id, name -> studioViewModel.renameProject(id, name) },
                        onDuplicateProject = { id -> studioViewModel.duplicateProject(id) },
                        onDeleteProject = { id -> studioViewModel.deleteProject(id) }
                    )

                    MainNavSection.CREATE -> CreateScreen(
                        isArabic = isArabic,
                        selectedDefaultRatio = settings.defaultAspectRatio,
                        onSelectAspectRatio = { studioViewModel.setDefaultAspectRatio(it) },
                        onImportExistingVideo = { importNewVideoLauncher.launch("video/*") },
                        onStartBlankProject = { title -> studioViewModel.startNewBlankProject(title) }
                    )

                    MainNavSection.TEMPLATES -> TemplatesScreen(
                        isArabic = isArabic,
                        selectedCategory = selectedTemplateCategory,
                        customTemplates = customTemplates,
                        onSelectCategory = { studioViewModel.selectTemplateCategory(it) },
                        onApplyTemplateToAll = { tpl ->
                            studioViewModel.applyCaptionTemplate(tpl, applyToSelectedOnly = false)
                            studioViewModel.navigateTo(MainNavSection.EDITOR)
                        },
                        onApplyTemplateToSelected = { tpl ->
                            studioViewModel.applyCaptionTemplate(tpl, applyToSelectedOnly = true)
                            studioViewModel.navigateTo(MainNavSection.EDITOR)
                        },
                        onSaveCustomTemplate = { ar, en ->
                            studioViewModel.saveCurrentStyleAsCustomTemplate(ar, en)
                        },
                        onDeleteCustomTemplate = { id ->
                            studioViewModel.deleteCustomTemplate(id)
                        }
                    )

                    MainNavSection.SETTINGS -> SettingsScreen(
                        settings = settings,
                        storageInfo = storageInfo,
                        onLanguageChange = { studioViewModel.setAppLanguage(it) },
                        onThemeChange = { studioViewModel.setDarkTheme(it) },
                        onResolutionChange = { studioViewModel.setDefaultExportResolution(it) },
                        onFrameRateChange = { studioViewModel.setDefaultFrameRate(it) },
                        onAspectRatioChange = { studioViewModel.setDefaultAspectRatio(it) },
                        onSpeechProviderChange = { studioViewModel.setSpeechProvider(it) },
                        onWhisperUrlChange = { studioViewModel.setWhisperEndpointUrl(it) },
                        onAiConsentChange = { studioViewModel.setAiCloudConsent(it) },
                        onClearCache = { studioViewModel.clearAppCache() }
                    )

                    MainNavSection.EDITOR -> {
                        val proj = activeProject ?: allProjects.firstOrNull()
                        if (proj != null) {
                            VideoEditorScreen(
                                isArabic = isArabic,
                                project = proj,
                                selectedTool = selectedEditorTool,
                                playbackPositionMs = playbackPositionMs,
                                isPlaying = isPlaying,
                                isFullscreen = isFullscreen,
                                timelineZoom = timelineZoom,
                                canUndo = canUndo,
                                canRedo = canRedo,
                                transcriptionLanguage = transcriptionLanguage,
                                transcriptionState = transcriptionState,
                                isAiToolLoading = isAiToolLoading,
                                aiErrorBanner = aiErrorBanner,
                                aiSuggestion = aiSuggestion,
                                onBack = {
                                    studioViewModel.setPlaying(false)
                                    studioViewModel.navigateTo(MainNavSection.HOME)
                                },
                                onUndo = { studioViewModel.undo() },
                                onRedo = { studioViewModel.redo() },
                                onOpenExportCenter = {
                                    studioViewModel.setPlaying(false)
                                    studioViewModel.navigateTo(MainNavSection.EXPORT_CENTER)
                                },
                                onSelectTool = { studioViewModel.selectEditorTool(it) },
                                onPlaybackPositionChange = { studioViewModel.setPlaybackPosition(it) },
                                onPlayingChange = { studioViewModel.setPlaying(it) },
                                onToggleFullscreen = { studioViewModel.toggleFullscreenPreview() },
                                onTimelineZoomChange = { studioViewModel.setTimelineZoom(it) },
                                onSplitAtPlayhead = { studioViewModel.splitAtPlayhead() },
                                onTrimVideoClip = { id, s, e -> studioViewModel.trimVideoClip(id, s, e) },
                                onDeleteTimelineClip = { id -> studioViewModel.deleteTimelineClip(id) },
                                onAspectRatioChange = { studioViewModel.setAspectRatioPreset(it) },
                                onReplaceSourceVideo = { replaceVideoLauncher.launch("video/*") },
                                onCaptionModeChange = { studioViewModel.setCaptionDisplayMode(it) },
                                onTranscriptionLanguageChange = { studioViewModel.setTranscriptionLanguage(it) },
                                onRunAutoTranscription = { studioViewModel.runAutomaticTranscription() },
                                onAddCaption = { studioViewModel.addCaptionSegment(it) },
                                onUpdateSegmentText = { id, txt, s, e -> studioViewModel.updateCaptionSegmentText(id, txt, s, e) },
                                onUpdateWord = { sId, wId, txt, s, e, h -> studioViewModel.updateSingleWord(sId, wId, txt, s, e, h) },
                                onSplitSegment = { studioViewModel.splitCaptionSegment(it) },
                                onMergeWithNext = { studioViewModel.mergeWithNextCaptionSegment(it) },
                                onDuplicateSegment = { studioViewModel.duplicateCaptionSegment(it) },
                                onDeleteSegment = { studioViewModel.deleteCaptionSegment(it) },
                                onToggleSelectSegment = { studioViewModel.toggleSelectCaptionSegment(it) },
                                onUpdateStyle = { transform -> studioViewModel.updateCaptionStyle(transform) },
                                onUpdateAnimation = { animTransform ->
                                    studioViewModel.updateCaptionStyle({ it.copy(animation = animTransform(it.animation)) })
                                },
                                onUpdateEffects = { studioViewModel.updateVideoEffects(it) },
                                onResetEffects = { studioViewModel.resetVideoEffects() },
                                onUpdateAudio = { studioViewModel.updateAudioConfig(it) },
                                onImportMusic = { importMusicLauncher.launch("audio/*") },
                                onImportSoundEffect = { importSfxLauncher.launch("audio/*") },
                                onRemoveAudioClip = { studioViewModel.removeAdditionalAudioClip(it) },
                                onRunSmartAiTool = { studioViewModel.runSmartAiTool(it) },
                                onApplyAiSuggestion = { studioViewModel.applyAiSuggestion(it) },
                                onDismissAiSuggestion = { studioViewModel.dismissAiSuggestion() }
                            )
                        }
                    }

                    MainNavSection.EXPORT_CENTER -> {
                        val proj = activeProject ?: allProjects.firstOrNull()
                        if (proj != null) {
                            ExportCenterScreen(
                                isArabic = isArabic,
                                project = proj,
                                exportState = exportState,
                                onBackToEditor = { studioViewModel.navigateTo(MainNavSection.EDITOR) },
                                onUpdateExportConfig = { studioViewModel.updateExportConfig(it) },
                                onStartExport = { studioViewModel.startRealVideoExport() },
                                onCancelExport = { studioViewModel.cancelVideoExport() },
                                onResetExportState = { studioViewModel.resetExportState() }
                            )
                        }
                    }
                }
            }
        }
    }
}
