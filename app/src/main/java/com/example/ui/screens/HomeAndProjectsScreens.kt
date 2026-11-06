package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.StudioProject
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainNavSection
import com.example.ui.viewmodel.StorageInfoState

@Composable
fun HomeScreen(
    isArabic: Boolean,
    projects: List<StudioProject>,
    interruptedProject: StudioProject?,
    recentExports: List<RecentExportItem>,
    storageInfo: StorageInfoState,
    onStartNewProject: () -> Unit,
    onImportVideoClick: () -> Unit,
    onOpenAiToolsClick: () -> Unit,
    onOpenProject: (StudioProject) -> Unit,
    onDismissInterruptedProject: () -> Unit,
    onOpenAllProjects: () -> Unit,
    onSelectQuickTool: (EditorToolTab) -> Unit,
    onApplyFeaturedTemplate: (CaptionTemplate) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Hero Header Inspired by Tasneem & Attached Mockup
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                StudioTealSlate,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AslStudioLogoHeader()
                        Surface(
                            color = PrimaryOrange.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "PRO AI v1.0",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryOrange
                            )
                        }
                    }

                    // Welcome message
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isArabic) "مرحباً بك \uD83D\uDC4B" else "Welcome back \uD83D\uDC4B",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isArabic) {
                                "اصنع فيديوهات احترافية بكابشن ذكي وتأثيرات مميزة"
                            } else {
                                "Create professional videos with smart AI captions and effects"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Interrupted Session Recovery Banner
                    if (interruptedProject != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = PrimaryOrange.copy(alpha = 0.18f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "استعادة جلسة تحرير سابقة" else "Recover Interrupted Session",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = PrimaryOrange
                                    )
                                    Text(
                                        text = interruptedProject.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = onDismissInterruptedProject) {
                                        Text(if (isArabic) "تجاهل" else "Dismiss")
                                    }
                                    Button(
                                        onClick = { onOpenProject(interruptedProject) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                                    ) {
                                        Text(if (isArabic) "استئناف" else "Resume")
                                    }
                                }
                            }
                        }
                    }

                    // Large New Project Button (Primary Orange #E8872F)
                    Button(
                        onClick = onStartNewProject,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("home_new_project_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "مشروع جديد" else "New Project",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    // Two Shortcut Cards: Import Video & AI Smart Tools
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShortcutActionCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_import_video_button"),
                            icon = Icons.Default.VideoLibrary,
                            title = if (isArabic) "استيراد فيديو" else "Import Video",
                            subtitle = if (isArabic) "MP4 / MOV / WebM" else "MP4 / MOV / WebM",
                            onClick = onImportVideoClick
                        )
                        ShortcutActionCard(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_ai_tools_button"),
                            icon = Icons.Default.AutoAwesome,
                            title = if (isArabic) "أدوات الذكاء الاصطناعي" else "AI Smart Tools",
                            subtitle = if (isArabic) "تفريغ ومزامنة" else "Transcribe & Sync",
                            onClick = onOpenAiToolsClick
                        )
                    }
                }
            }
        }

        // 2. Quick Editing Tools Row
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "أدوات التحرير السريع" else "Quick Editing Tools",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val quickTools = listOf(
                    Triple(EditorToolTab.CAPTIONS, Icons.Default.ClosedCaption, if (isArabic) "الكابشن الذكي" else "AI Captions"),
                    Triple(EditorToolTab.ANIMATION, Icons.Default.Animation, if (isArabic) "حركات الكلمات" else "Animations"),
                    Triple(EditorToolTab.EFFECTS, Icons.Default.Tune, if (isArabic) "تأثيرات الفيديو" else "Video Effects"),
                    Triple(EditorToolTab.AUDIO, Icons.Default.GraphicEq, if (isArabic) "تحرير الصوت" else "Audio Studio"),
                    Triple(EditorToolTab.EXPORT, Icons.Default.HighQuality, if (isArabic) "تصدير 1080p" else "1080p Export")
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(quickTools) { (tab, icon, label) ->
                        Surface(
                            onClick = { onSelectQuickTool(tab) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(icon, contentDescription = label, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
                                Text(label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // 3. Recent Projects Card Container (matching the rounded ivory/slate card in screenshot)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "المشاريع الأخيرة" else "Recent Projects",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = onOpenAllProjects) {
                            Text(
                                text = if (isArabic) "عرض الكل" else "View All",
                                color = PrimaryOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (projects.isEmpty()) {
                        Text(
                            text = if (isArabic) "لا توجد مشاريع بعد. ابدأ مشروعاً جديداً الآن!" else "No projects yet. Start a new project now!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        projects.take(3).forEach { project ->
                            RecentProjectRowItem(
                                project = project,
                                isArabic = isArabic,
                                onClick = { onOpenProject(project) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Featured Caption Templates
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isArabic) "قوالب الكابشن المميزة" else "Featured Caption Templates",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(CaptionTemplateCatalog.builtInTemplates.take(6)) { tpl ->
                        Card(
                            onClick = { onApplyFeaturedTemplate(tpl) },
                            modifier = Modifier.width(165.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                PodcastStudioThumbnail(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(92.dp),
                                    badgeText = if (isArabic) tpl.nameAr else tpl.nameEn
                                )
                                Text(
                                    text = if (isArabic) tpl.nameAr else tpl.nameEn,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isArabic) tpl.captionMode.labelAr else tpl.captionMode.labelEn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PrimaryOrange
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Recent Exports & Storage Info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "معلومات التخزين وآخر الفيديوهات المصدرة" else "Storage & Recent Exports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryOrange)
                    }
                    Text(
                        text = if (isArabic) {
                            "المساحة المتاحة: ${formatBytesSize(storageInfo.freeDeviceBytes)} • استهلاك التطبيق: ${formatBytesSize(storageInfo.usedAppBytes)}"
                        } else {
                            "Free Space: ${formatBytesSize(storageInfo.freeDeviceBytes)} • App Usage: ${formatBytesSize(storageInfo.usedAppBytes)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (recentExports.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        recentExports.take(2).forEach { exp ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${exp.projectName} (${exp.resolutionLabel} • ${exp.frameRate}fps)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PrimaryOrange
                                )
                                Text(
                                    text = formatBytesSize(exp.fileSizeBytes),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    Text(
                        text = "ASL Captions Studio • v1.0.0 (API 26+)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(94.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = PrimaryOrange, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentProjectRowItem(
    project: StudioProject,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PodcastStudioThumbnail(
                modifier = Modifier
                    .width(86.dp)
                    .height(56.dp),
                badgeText = project.aspectRatioPreset.id
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatTimestampMs(project.videoMetadata.durationMs)} • ${project.segments.size} ${if (isArabic) "جمل كابشن" else "captions"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.PlayCircleFilled,
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun ProjectsScreen(
    isArabic: Boolean,
    projects: List<StudioProject>,
    searchQuery: String,
    sortAscending: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSortOrder: () -> Unit,
    onCreateNewProject: () -> Unit,
    onOpenProject: (StudioProject) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    var projectToRename by remember { mutableStateOf<StudioProject?>(null) }
    var renameText by remember { mutableStateOf("") }

    val filtered = remember(projects, searchQuery, sortAscending) {
        val base = if (searchQuery.isBlank()) projects else {
            projects.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
        if (sortAscending) base.sortedBy { it.lastModifiedMillis }
        else base.sortedByDescending { it.lastModifiedMillis }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("projects_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "جميع المشاريع (${projects.size})" else "All Projects (${projects.size})",
                style = MaterialTheme.typography.headlineMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onToggleSortOrder) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = if (isArabic) "ترتيب حسب التاريخ" else "Sort by date",
                        tint = PrimaryOrange
                    )
                }
                Button(
                    onClick = onCreateNewProject,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "جديد" else "New")
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("projects_search_input"),
            placeholder = {
                Text(if (isArabic) "ابحث في المشاريع..." else "Search projects...")
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { proj ->
                Card(
                    onClick = { onOpenProject(proj) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PodcastStudioThumbnail(
                            modifier = Modifier
                                .width(92.dp)
                                .height(64.dp),
                            badgeText = proj.aspectRatioPreset.id
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = proj.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${formatTimestampMs(proj.videoMetadata.durationMs)} • ${proj.videoMetadata.width}x${proj.videoMetadata.height} • ${proj.segments.size} جمل",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isArabic) "نمط: ${proj.captionMode.labelAr}" else "Mode: ${proj.captionMode.labelEn}",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryOrange
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                projectToRename = proj
                                renameText = proj.title
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDuplicateProject(proj.id) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDeleteProject(proj.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StudioError)
                            }
                        }
                    }
                }
            }
        }
    }

    if (projectToRename != null) {
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text(if (isArabic) "إعادة تسمية المشروع" else "Rename Project") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        projectToRename?.let { onRenameProject(it.id, renameText) }
                        projectToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text(if (isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun CreateScreen(
    isArabic: Boolean,
    selectedDefaultRatio: AspectRatioPreset,
    onSelectAspectRatio: (AspectRatioPreset) -> Unit,
    onImportExistingVideo: () -> Unit,
    onStartBlankProject: (String) -> Unit
) {
    var projectTitle by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("create_screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = if (isArabic) "إنشاء مشروع مونتاج جديد" else "Create New Video Project",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isArabic) {
                    "استورد فيديو من جهازك (MP4, MOV, WebM) أو ابدأ مساحة عمل جاهزة مع كابشن ذكي."
                } else {
                    "Import a video from device storage (MP4, MOV, WebM) or start a new editing project."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action 1: Import Existing Video from Device Storage
        item {
            Card(
                onClick = onImportExistingVideo,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_import_video_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.VideoFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "استيراد فيديو من الجهاز" else "Import Existing Video",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = if (isArabic) {
                                "يدعم MP4 و MOV و WebM مع استخراج تلقائي للدقة ومعدل الإطارات"
                            } else {
                                "Supports MP4, MOV & WebM with automatic metadata extraction"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Aspect Ratio Presets (Section 15)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isArabic) "أبعاد الفيديو (Aspect Ratio)" else "Video Aspect Ratio Preset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    AspectRatioPreset.entries.forEach { preset ->
                        val selected = selectedDefaultRatio == preset
                        Surface(
                            onClick = { onSelectAspectRatio(preset) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) PrimaryOrange.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (selected) 1.5.dp else 0.dp,
                                    color = if (selected) PrimaryOrange else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) preset.labelAr else preset.labelEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (selected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryOrange)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action 2: Start New Editing Project
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isArabic) "بدء مشروع تحرير جديد" else "Start New Editing Project",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (isArabic) "اسم المشروع (اختياري)" else "Project Name (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = { onStartBlankProject(projectTitle) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("create_start_project_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSilver, contentColor = SoftIvory)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = PrimaryOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isArabic) "فتح استوديو التحرير الآن" else "Open Editing Studio Now")
                    }
                }
            }
        }
    }
}
