package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.StudioAppSettings
import com.example.domain.model.*
import com.example.ui.components.PodcastStudioThumbnail
import com.example.ui.components.formatBytesSize
import com.example.ui.theme.*
import com.example.ui.viewmodel.StorageInfoState

@Composable
fun TemplatesScreen(
    isArabic: Boolean,
    selectedCategory: TemplateCategory,
    customTemplates: List<CaptionTemplate>,
    onSelectCategory: (TemplateCategory) -> Unit,
    onApplyTemplateToAll: (CaptionTemplate) -> Unit,
    onApplyTemplateToSelected: (CaptionTemplate) -> Unit,
    onSaveCustomTemplate: (String, String) -> Unit,
    onDeleteCustomTemplate: (String) -> Unit
) {
    var inspectedTemplate by remember { mutableStateOf<CaptionTemplate?>(null) }
    var showCreateCustomDialog by remember { mutableStateOf(false) }
    var customNameAr by remember { mutableStateOf("") }
    var customNameEn by remember { mutableStateOf("") }

    val allTemplates = remember(customTemplates) {
        CaptionTemplateCatalog.builtInTemplates + customTemplates
    }

    val filteredTemplates = remember(allTemplates, selectedCategory) {
        if (selectedCategory == TemplateCategory.ALL) allTemplates
        else allTemplates.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("templates_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "القوالب (${allTemplates.size})" else "Templates (${allTemplates.size})",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { showCreateCustomDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = PrimaryOrange)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isArabic) "حفظ نمطي الحالي" else "Save Current Style")
            }
        }

        // Category Filter Row (matching screenshot: الكل | بودكاست | سوشيال ميديا | سينمائي | ديناميكي | عربي)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TemplateCategory.entries) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(cat) },
                    label = {
                        Text(
                            text = if (isArabic) cat.labelAr else cat.labelEn,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // 2-Column Visual Grid of Templates
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTemplates, key = { it.id }) { tpl ->
                Card(
                    onClick = { inspectedTemplate = tpl },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = PrimaryOrange.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            PodcastStudioThumbnail(
                                modifier = Modifier.fillMaxSize(),
                                accentColor = Color(tpl.style.activeWordColorArgb)
                            )
                            // Live Styled Caption Preview Pill inside Card
                            val fontFam = resolveCaptionFontFamily(tpl.style.fontFamilyName)
                            Surface(
                                color = if (tpl.style.backgroundStyle == CaptionBackgroundStyle.NONE) {
                                    DeepCharcoal.copy(alpha = 0.72f)
                                } else {
                                    Color(tpl.style.backgroundColorArgb).copy(alpha = tpl.style.backgroundOpacity)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (isArabic) tpl.nameAr else tpl.nameEn,
                                    fontFamily = fontFam,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(tpl.style.activeWordColorArgb),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) tpl.nameAr else tpl.nameEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tpl.style.fontFamilyName} • ${if (isArabic) tpl.style.animation.preset.nameAr else tpl.style.animation.preset.nameEn}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (tpl.isCustom) {
                                IconButton(
                                    onClick = { onDeleteCustomTemplate(tpl.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StudioError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Template Action Sheet / Dialog
    if (inspectedTemplate != null) {
        val tpl = inspectedTemplate!!
        AlertDialog(
            onDismissRequest = { inspectedTemplate = null },
            title = {
                Text(if (isArabic) tpl.nameAr else tpl.nameEn, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) {
                            "الخط: ${tpl.style.fontFamilyName} (${tpl.style.fontSizeSp.toInt()}sp)\n" +
                                "نمط العرض: ${tpl.captionMode.labelAr}\n" +
                                "الحركة: ${tpl.style.animation.preset.nameAr}"
                        } else {
                            "Font: ${tpl.style.fontFamilyName} (${tpl.style.fontSizeSp.toInt()}sp)\n" +
                                "Mode: ${tpl.captionMode.labelEn}\n" +
                                "Animation: ${tpl.style.animation.preset.nameEn}"
                        }
                    )
                    Button(
                        onClick = {
                            onApplyTemplateToAll(tpl)
                            inspectedTemplate = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Text(if (isArabic) "تطبيق على جميع جمل الفيديو" else "Apply to Entire Video")
                    }
                    OutlinedButton(
                        onClick = {
                            onApplyTemplateToSelected(tpl)
                            inspectedTemplate = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArabic) "تطبيق على الكابشن المحدد فقط" else "Apply to Selected Captions")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { inspectedTemplate = null }) {
                    Text(if (isArabic) "إغلاق" else "Close")
                }
            }
        )
    }

    if (showCreateCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCustomDialog = false },
            title = { Text(if (isArabic) "حفظ قالب كابشن مخصص" else "Save Custom Caption Template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customNameAr,
                        onValueChange = { customNameAr = it },
                        label = { Text("اسم القالب بالعربية") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customNameEn,
                        onValueChange = { customNameEn = it },
                        label = { Text("Template Name (English)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomTemplate(
                            customNameAr.ifBlank { "قالب مخصص" },
                            customNameEn.ifBlank { "Custom Style" }
                        )
                        customNameAr = ""
                        customNameEn = ""
                        showCreateCustomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text(if (isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCustomDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsScreen(
    settings: StudioAppSettings,
    storageInfo: StorageInfoState,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onResolutionChange: (ExportResolution) -> Unit,
    onFrameRateChange: (Int) -> Unit,
    onAspectRatioChange: (AspectRatioPreset) -> Unit,
    onSpeechProviderChange: (SpeechProviderType) -> Unit,
    onWhisperUrlChange: (String) -> Unit,
    onAiConsentChange: (Boolean) -> Unit,
    onClearCache: () -> Unit
) {
    val isArabic = settings.language == AppLanguage.ARABIC
    var whisperUrlDraft by remember(settings.whisperEndpointUrl) { mutableStateOf(settings.whisperEndpointUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Text(
                text = if (isArabic) "الإعدادات والتفضيلات" else "Settings & Preferences",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 1. Language & Theme Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isArabic) "اللغة والمظهر (مستوحى من تسنيم)" else "Language & Visual Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val selected = settings.language == lang
                            Button(
                                onClick = { onLanguageChange(lang) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("lang_${lang.code}_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) PrimaryOrange else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (isArabic) lang.displayNameAr else lang.displayNameEn)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "المظهر الداكن الاحترافي (Deep Charcoal)" else "Premium Dark Theme",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isArabic) "عند الإيقاف يتم التبديل إلى المظهر العاجي الفاتح (Soft Ivory)" else "Switch between Deep Charcoal Dark and Soft Ivory Light",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isDarkTheme,
                            onCheckedChange = onThemeChange,
                            modifier = Modifier.testTag("theme_toggle_switch"),
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryOrange)
                        )
                    }
                }
            }
        }

        // 2. Default Export Quality & Frame Rate
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isArabic) "إعدادات التصدير الافتراضية (MP4)" else "Default Export Preferences (MP4)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )

                    Text(
                        text = if (isArabic) "دقة الفيديو الافتراضية" else "Default Video Resolution",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExportResolution.entries.forEach { res ->
                            val selected = settings.defaultResolution == res
                            FilterChip(
                                selected = selected,
                                onClick = { onResolutionChange(res) },
                                label = { Text(res.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Text(
                        text = if (isArabic) "معدل الإطارات الافتراضي (FPS)" else "Default Frame Rate (FPS)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(30, 60).forEach { fps ->
                            val selected = settings.defaultFrameRate == fps
                            FilterChip(
                                selected = selected,
                                onClick = { onFrameRateChange(fps) },
                                label = { Text("$fps FPS") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. AI Transcription Provider Configuration & Privacy (Sections 6, 18, 20)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isArabic) "إعدادات مزود الذكاء الاصطناعي والخصوصية" else "AI Transcription Provider & Privacy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )

                    SpeechProviderType.entries.forEach { provider ->
                        val selected = settings.speechProvider == provider
                        Surface(
                            onClick = { onSpeechProviderChange(provider) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) PrimaryOrange.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isArabic) provider.labelAr else provider.labelEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSpeechProviderChange(provider) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryOrange)
                                )
                            }
                        }
                    }

                    if (settings.speechProvider == SpeechProviderType.WHISPER_ENDPOINT) {
                        OutlinedTextField(
                            value = whisperUrlDraft,
                            onValueChange = {
                                whisperUrlDraft = it
                                onWhisperUrlChange(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (isArabic) "رابط خادم Whisper (HTTPS)" else "Whisper Server Endpoint URL") },
                            placeholder = { Text("https://your-server.example.com/v1/audio/transcriptions") },
                            singleLine = true
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "الموافقة على إرسال مقاطع الصوت للتفريغ السحابي" else "Consent for Cloud Audio Transcription",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) {
                                    "تتم معالجة الفيديو والتأثيرات والرندر محلياً بالكامل على جهازك. يتم إرسال المسار الصوتي المضغوط فقط عند طلب توليد الكابشن التلقائي عبر Firebase AI Logic المحمي بـ App Check بدون أي مفاتيح خاصة داخل التطبيق."
                                } else {
                                    "Video editing, effects, and MP4 rendering run 100% locally on your device. Compressed audio chunks are sent securely via App Check-attested Firebase AI Logic only when you trigger AI transcription."
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.hasAcceptedAiCloudConsent,
                            onCheckedChange = onAiConsentChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryOrange)
                        )
                    }
                }
            }
        }

        // 4. Storage & Cache Management
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isArabic) "إدارة التخزين والذاكرة المؤقتة" else "Storage & Cache Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Text(
                        text = if (isArabic) {
                            "ملفات المشاريع والفيديو: ${formatBytesSize(storageInfo.usedAppBytes)}\n" +
                                "الذاكرة المؤقتة (Cache): ${formatBytesSize(storageInfo.cacheBytes)}\n" +
                                "المساحة الحرة في الجهاز: ${formatBytesSize(storageInfo.freeDeviceBytes)}"
                        } else {
                            "Projects & Media: ${formatBytesSize(storageInfo.usedAppBytes)}\n" +
                                "App Cache: ${formatBytesSize(storageInfo.cacheBytes)}\n" +
                                "Device Free Space: ${formatBytesSize(storageInfo.freeDeviceBytes)}"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PrimaryOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isArabic) "مسح الذاكرة المؤقتة (Clear Cache)" else "Clear Temporary Cache")
                    }
                }
            }
        }

        // 5. About Application
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isArabic) "حول تطبيق ASL Captions Studio" else "About ASL Captions Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Text(
                        text = if (isArabic) {
                            "استوديو مونتاج فيديو احترافي مدعوم بالذكاء الاصطناعي لتوليد الكابشن التلقائي باللغتين العربية والإنجليزية، ومزامنة الكلمات المنطوقة، وتطبيق 25 حركة نصية و 20 قالب كابشن وتأثيرات لونية احترافية مع تصدير MP4 حقيقي بدقة 1080p."
                        } else {
                            "Professional AI video editing platform for automatic Arabic & English word-level captions, 25 text animations, 20 templates, visual effects, multi-track timeline, and real 1080p MP4 export."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Package: com.asldesign.captionsstudio • Version 1.0.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryOrange
                    )
                }
            }
        }
    }
}
