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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.TranscriptionProgress
import com.example.data.repository.StudioProject
import com.example.domain.model.*
import com.example.ui.components.formatTimestampMs
import com.example.ui.theme.*

@Composable
fun CaptionsEditorPanel(
    isArabic: Boolean,
    project: StudioProject,
    transcriptionLanguage: TranscriptionLanguage,
    transcriptionState: TranscriptionProgress,
    onSelectMode: (CaptionDisplayMode) -> Unit,
    onSelectTranscriptionLanguage: (TranscriptionLanguage) -> Unit,
    onRunAutoTranscription: () -> Unit,
    onAddCaption: (String) -> Unit,
    onUpdateSegmentText: (String, String, Long?, Long?) -> Unit,
    onUpdateWord: (String, String, String, Long, Long, Boolean) -> Unit,
    onSplitSegment: (String) -> Unit,
    onMergeWithNext: (String) -> Unit,
    onDuplicateSegment: (String) -> Unit,
    onDeleteSegment: (String) -> Unit,
    onToggleSelectSegment: (String) -> Unit,
    onSeekTo: (Long) -> Unit
) {
    var editingSegment by remember { mutableStateOf<CaptionSegment?>(null) }
    var editingWordPair by remember { mutableStateOf<Pair<CaptionSegment, CaptionWord>?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCaptionDraft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Caption Mode Selector (كلمة بكلمة vs جملة كاملة — matching screenshot)
        Text(
            text = if (isArabic) "نمط الكابشن" else "Caption Display Mode",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CaptionDisplayMode.entries.forEach { mode ->
                val selected = project.captionMode == mode
                Surface(
                    onClick = { onSelectMode(mode) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) PrimaryOrange.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isArabic) mode.labelAr else mode.labelEn,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2. AI Automatic Caption Generator Bar
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "توليد الكابشن التلقائي بالذكاء الاصطناعي" else "AI Automatic Caption Generator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArabic) "يدعم العربية (الفصحى، السورية/الشامية، المصرية، الخليجية) والإنجليزية" else "Supports Arabic dialects & English with word-level alignment",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onRunAutoTranscription,
                        enabled = transcriptionState !is TranscriptionProgress.Processing,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("generate_ai_captions_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isArabic) "توليد الآن" else "Transcribe")
                    }
                }

                // Dialect / Language Selector Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TranscriptionLanguage.entries) { lang ->
                        val selected = transcriptionLanguage == lang
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectTranscriptionLanguage(lang) },
                            label = { Text(if (isArabic) lang.labelAr else lang.labelEn, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                when (transcriptionState) {
                    is TranscriptionProgress.Processing -> {
                        LinearProgressIndicator(
                            progress = { transcriptionState.stepIndex.toFloat() / transcriptionState.totalSteps.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = PrimaryOrange
                        )
                        Text(
                            text = if (isArabic) transcriptionState.statusAr else transcriptionState.statusEn,
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryOrange
                        )
                    }
                    is TranscriptionProgress.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) transcriptionState.messageAr else transcriptionState.messageEn,
                                style = MaterialTheme.typography.labelMedium,
                                color = StudioError,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onRunAutoTranscription) {
                                Text(if (isArabic) "إعادة المحاولة" else "Retry", color = PrimaryOrange)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // 3. Editable Transcript & Word-Level Timestamps List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "الجمل والكلمات المتزامنة (${project.segments.size})" else "Synchronized Captions (${project.segments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryOrange)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "إضافة جملة" else "Add Caption", fontSize = 12.sp)
            }
        }

        project.segments.forEach { segment ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (segment.isSelected) PrimaryOrange.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeekTo(segment.startTimeMs + 100L) }
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = segment.isSelected,
                                onCheckedChange = { onToggleSelectSegment(segment.id) },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange)
                            )
                            Text(
                                text = "${formatTimestampMs(segment.startTimeMs)} - ${formatTimestampMs(segment.endTimeMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryOrange
                            )
                        }
                        Row {
                            IconButton(onClick = { editingSegment = segment }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Sentence", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onSplitSegment(segment.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.ContentCut, contentDescription = "Split Sentence", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onMergeWithNext(segment.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.MergeType, contentDescription = "Merge Next", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDuplicateSegment(segment.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDeleteSegment(segment.id) }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StudioError, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Text(
                        text = segment.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // Interactive Word Chips with Word-Level Timestamps
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(segment.words, key = { it.id }) { word ->
                            Surface(
                                onClick = { editingWordPair = segment to word },
                                shape = RoundedCornerShape(8.dp),
                                color = if (word.isHighlighted) PrimaryOrange else MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "${word.text} (${word.startTimeMs}ms)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                    color = if (word.isHighlighted) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Full Sentence Dialog
    if (editingSegment != null) {
        val seg = editingSegment!!
        var textDraft by remember(seg) { mutableStateOf(seg.text) }
        var startDraft by remember(seg) { mutableStateOf(seg.startTimeMs.toString()) }
        var endDraft by remember(seg) { mutableStateOf(seg.endTimeMs.toString()) }

        AlertDialog(
            onDismissRequest = { editingSegment = null },
            title = { Text(if (isArabic) "تعديل الجملة والتوقيت" else "Edit Sentence & Timing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = textDraft,
                        onValueChange = { textDraft = it },
                        label = { Text(if (isArabic) "نص الجملة" else "Sentence Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startDraft,
                            onValueChange = { startDraft = it },
                            label = { Text("Start (ms)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endDraft,
                            onValueChange = { endDraft = it },
                            label = { Text("End (ms)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSegmentText(
                            seg.id,
                            textDraft,
                            startDraft.toLongOrNull(),
                            endDraft.toLongOrNull()
                        )
                        editingSegment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text(if (isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSegment = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Edit Single Word & Word Timestamp Dialog
    if (editingWordPair != null) {
        val (seg, word) = editingWordPair!!
        var wordDraft by remember(word) { mutableStateOf(word.text) }
        var startMsDraft by remember(word) { mutableStateOf(word.startTimeMs.toString()) }
        var endMsDraft by remember(word) { mutableStateOf(word.endTimeMs.toString()) }
        var highlightedDraft by remember(word) { mutableStateOf(word.isHighlighted) }

        AlertDialog(
            onDismissRequest = { editingWordPair = null },
            title = { Text(if (isArabic) "تعديل الكلمة والتوقيت الزمني" else "Edit Word & Timestamp") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wordDraft,
                        onValueChange = { wordDraft = it },
                        label = { Text(if (isArabic) "الكلمة" else "Word") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startMsDraft,
                            onValueChange = { startMsDraft = it },
                            label = { Text("Start (ms)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endMsDraft,
                            onValueChange = { endMsDraft = it },
                            label = { Text("End (ms)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = highlightedDraft, onCheckedChange = { highlightedDraft = it })
                        Text(if (isArabic) "تمييز هذه الكلمة بلون نشط دائماً" else "Always highlight this keyword")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateWord(
                            seg.id,
                            word.id,
                            wordDraft,
                            startMsDraft.toLongOrNull() ?: word.startTimeMs,
                            endMsDraft.toLongOrNull() ?: word.endTimeMs,
                            highlightedDraft
                        )
                        editingWordPair = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text(if (isArabic) "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingWordPair = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (isArabic) "إضافة جملة كابشن جديدة" else "Add New Caption") },
            text = {
                OutlinedTextField(
                    value = newCaptionDraft,
                    onValueChange = { newCaptionDraft = it },
                    placeholder = { Text(if (isArabic) "اكتب الجملة هنا..." else "Enter caption sentence...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddCaption(newCaptionDraft)
                        newCaptionDraft = ""
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Text(if (isArabic) "إضافة" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

/**
 * Professional Caption Design Studio Panel (Section 8)
 */
@Composable
fun TextDesignStudioPanel(
    isArabic: Boolean,
    style: CaptionStyle,
    focusSection: EditorToolTab,
    onUpdateStyle: ((CaptionStyle) -> CaptionStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Font Family Selection (matching screenshot: الخط | Tajawal Aa)
        Text(
            text = if (isArabic) "الخط (Font Family)" else "Font Family",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AvailableStudioFonts) { fontName ->
                val selected = style.fontFamilyName.equals(fontName, ignoreCase = true)
                Surface(
                    onClick = { onUpdateStyle { it.copy(fontFamilyName = fontName) } },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.border(
                        width = if (selected) 2.dp else 0.dp,
                        color = if (selected) PrimaryOrangeLight else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Aa أب",
                            fontFamily = resolveCaptionFontFamily(fontName),
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else PrimaryOrange
                        )
                        Text(
                            text = fontName,
                            fontFamily = resolveCaptionFontFamily(fontName),
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2. Font Size & Weight
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (isArabic) "حجم الخط" else "Font Size", style = MaterialTheme.typography.labelLarge)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${style.fontSizeSp.toInt()}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Slider(
            value = style.fontSizeSp,
            onValueChange = { v -> onUpdateStyle { it.copy(fontSizeSp = v) } },
            valueRange = 20f..68f,
            colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
        )

        // 3. Text Color & Active Word Color Swatches (matching screenshot circular swatches)
        Text(
            text = if (isArabic) "لون النص" else "Text Color",
            style = MaterialTheme.typography.labelLarge
        )
        ColorSwatchRow(
            selectedColorArgb = style.textColorArgb,
            onSelectColor = { c -> onUpdateStyle { it.copy(textColorArgb = c) } }
        )

        Text(
            text = if (isArabic) "لون الكلمة النشطة" else "Active Word Highlight Color",
            style = MaterialTheme.typography.labelLarge
        )
        ColorSwatchRow(
            selectedColorArgb = style.activeWordColorArgb,
            onSelectColor = { c -> onUpdateStyle { it.copy(activeWordColorArgb = c) } }
        )

        // 4. Background Style & Opacity & Corner Radius
        Text(
            text = if (isArabic) "الخلفية" else "Caption Background",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CaptionBackgroundStyle.entries) { bgStyle ->
                val selected = style.backgroundStyle == bgStyle
                FilterChip(
                    selected = selected,
                    onClick = { onUpdateStyle { it.copy(backgroundStyle = bgStyle) } },
                    label = { Text(if (isArabic) bgStyle.labelAr else bgStyle.labelEn) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (style.backgroundStyle != CaptionBackgroundStyle.NONE) {
            Text(
                text = if (isArabic) "شفافية الخلفية (${(style.backgroundOpacity * 100).toInt()}%)" else "Background Opacity (${(style.backgroundOpacity * 100).toInt()}%)",
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = style.backgroundOpacity,
                onValueChange = { v -> onUpdateStyle { it.copy(backgroundOpacity = v) } },
                valueRange = 0.15f..1.0f,
                colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
            )
        }

        // 5. Outline (Stroke) & Shadow Controls
        Text(
            text = if (isArabic) "الحدود الخارجية والظل (Outline & Shadow)" else "Outline Stroke & Drop Shadow",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) "سمك الحدود: ${style.strokeThicknessPx.toInt()}px" else "Stroke: ${style.strokeThicknessPx.toInt()}px",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = style.strokeThicknessPx,
                    onValueChange = { v -> onUpdateStyle { it.copy(strokeThicknessPx = v) } },
                    valueRange = 0f..12f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) "تمويه الظل: ${style.shadowBlurRadius.toInt()}px" else "Shadow Blur: ${style.shadowBlurRadius.toInt()}px",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = style.shadowBlurRadius,
                    onValueChange = { v -> onUpdateStyle { it.copy(shadowBlurRadius = v) } },
                    valueRange = 0f..24f,
                    colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
                )
            }
        }

        // 6. Position Preset (Top, Center, Bottom, Custom X/Y)
        Text(
            text = if (isArabic) "موقع الكابشن" else "Caption Position",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CaptionPositionPreset.entries.forEach { pos ->
                val selected = style.positionPreset == pos
                FilterChip(
                    selected = selected,
                    onClick = { onUpdateStyle { it.copy(positionPreset = pos) } },
                    label = { Text(if (isArabic) pos.labelAr else pos.labelEn) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    selectedColorArgb: Long,
    onSelectColor: (Long) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(CaptionPresetColors) { colorArgb ->
            val isSelected = selectedColorArgb == colorArgb
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(colorArgb))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) PrimaryOrange else SilverGray.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onSelectColor(colorArgb) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (colorArgb == 0xFFFFFFFFL || colorArgb == 0xFFFFD600L) DeepCharcoal else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Professional 25 Caption Animation Presets Panel (Section 9)
 */
@Composable
fun AnimationStudioPanel(
    isArabic: Boolean,
    animation: AnimationParameters,
    onUpdateAnimation: ((AnimationParameters) -> AnimationParameters) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isArabic) "حركات الكابشن الاحترافية (25 حركة)" else "Caption Animation Engine (25 Presets)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Grid of 25 presets (3 per row matching the screenshot's 'حركات الكابشن' card)
        val rows = remember { CaptionAnimationPreset.entries.chunked(3) }
        rows.forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPresets.forEach { preset ->
                    val selected = animation.preset == preset
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            onClick = { onUpdateAnimation { it.copy(preset = preset) } },
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSilver,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) PrimaryOrange else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Aa",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selected) PrimaryOrange else Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) preset.nameAr else preset.nameEn,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                repeat(3 - rowPresets.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Customizable Animation Parameters: Duration, Intensity, Easing, Direction
        Text(
            text = if (isArabic) "مدة الحركة: ${animation.durationMs}ms" else "Animation Duration: ${animation.durationMs}ms",
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = animation.durationMs.toFloat(),
            onValueChange = { v -> onUpdateAnimation { it.copy(durationMs = v.toLong()) } },
            valueRange = 100f..1200f,
            colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
        )

        Text(
            text = if (isArabic) "شدة الحركة (Intensity): ${String.format("%.1fx", animation.intensity)}" else "Intensity: ${String.format("%.1fx", animation.intensity)}",
            style = MaterialTheme.typography.labelLarge
        )
        Slider(
            value = animation.intensity,
            onValueChange = { v -> onUpdateAnimation { it.copy(intensity = v) } },
            valueRange = 0.3f..2.0f,
            colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimationEasing.entries.forEach { easing ->
                FilterChip(
                    selected = animation.easing == easing,
                    onClick = { onUpdateAnimation { it.copy(easing = easing) } },
                    label = { Text(if (isArabic) easing.labelAr else easing.labelEn, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

/**
 * Video Effects Studio Panel (Section 11 — 20 Effects & Color Grading)
 */
@Composable
fun VideoEffectsStudioPanel(
    isArabic: Boolean,
    effects: VideoEffectsConfig,
    onUpdateEffects: ((VideoEffectsConfig) -> VideoEffectsConfig) -> Unit,
    onResetEffects: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isArabic) "تأثيرات الفيديو وتدريج الألوان" else "Video Effects & Color Grading",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onResetEffects) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "إعادة ضبط" else "Reset All", color = PrimaryOrange)
            }
        }

        // Color Grading Presets Grid (matching screenshot: أصلي | سينمائي | دافئ | بارد | تباين | سطوع | آرت | أبيض وأسود)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ColorGradingPreset.entries) { preset ->
                val selected = effects.colorGradingPreset == preset
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        onUpdateEffects { it.copy(colorGradingPreset = preset) }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 68.dp, height = 78.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(
                                    when (preset) {
                                        ColorGradingPreset.WARM -> listOf(Color(0xFFE8872F), Color(0xFF4E342E))
                                        ColorGradingPreset.COOL -> listOf(Color(0xFF4FC3F7), Color(0xFF1E2B2E))
                                        ColorGradingPreset.BLACK_AND_WHITE -> listOf(Color.Gray, Color.Black)
                                        ColorGradingPreset.CINEMATIC -> listOf(Color(0xFF26A69A), Color(0xFF1D2227))
                                        else -> listOf(StudioTealSlate, PrimaryOrangeDark)
                                    }
                                )
                            )
                            .border(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) PrimaryOrange else SilverGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Landscape, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) preset.labelAr else preset.labelEn,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Adjustable Sliders for Brightness, Contrast, Saturation, Exposure, Temperature, Vignette, Blur, Sharpen
        EffectSliderRow(
            label = if (isArabic) "السطوع (Brightness)" else "Brightness",
            value = effects.brightness,
            range = -100f..100f,
            onValueChange = { v -> onUpdateEffects { it.copy(brightness = v) } }
        )
        EffectSliderRow(
            label = if (isArabic) "التباين (Contrast)" else "Contrast",
            value = effects.contrast,
            range = -100f..100f,
            onValueChange = { v -> onUpdateEffects { it.copy(contrast = v) } }
        )
        EffectSliderRow(
            label = if (isArabic) "التشبع اللوني (Saturation)" else "Saturation",
            value = effects.saturation,
            range = -100f..100f,
            onValueChange = { v -> onUpdateEffects { it.copy(saturation = v) } }
        )
        EffectSliderRow(
            label = if (isArabic) "التعريض (Exposure)" else "Exposure",
            value = effects.exposure,
            range = -100f..100f,
            onValueChange = { v -> onUpdateEffects { it.copy(exposure = v) } }
        )
        EffectSliderRow(
            label = if (isArabic) "حرارة اللون (Temperature)" else "Temperature",
            value = effects.temperature,
            range = -100f..100f,
            onValueChange = { v -> onUpdateEffects { it.copy(temperature = v) } }
        )
        EffectSliderRow(
            label = if (isArabic) "التظليل المحيطي (Vignette)" else "Vignette",
            value = effects.vignette,
            range = 0f..100f,
            onValueChange = { v -> onUpdateEffects { it.copy(vignette = v) } }
        )
        EffectSliderRow(
            label = if (isArabic) "الضبابية (Blur)" else "Blur",
            value = effects.blur,
            range = 0f..100f,
            onValueChange = { v -> onUpdateEffects { it.copy(blur = v) } }
        )
        EffectSliderRow(
            label = if (isArabic) "تقريب سينمائي (Zoom In)" else "Dynamic Zoom In",
            value = effects.zoomInAmount,
            range = 0f..100f,
            onValueChange = { v -> onUpdateEffects { it.copy(zoomInAmount = v) } }
        )

        // Transform controls: Rotation & Flip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onUpdateEffects { it.copy(rotationDegrees = (it.rotationDegrees + 90f) % 360f) } },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "تدوير 90°" else "Rotate 90°")
            }
            OutlinedButton(
                onClick = { onUpdateEffects { it.copy(flipHorizontal = !it.flipHorizontal) } },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "قلب أفقي" else "Flip H")
            }
            OutlinedButton(
                onClick = { onUpdateEffects { it.copy(flipVertical = !it.flipVertical) } },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isArabic) "قلب عمودي" else "Flip V")
            }
        }
    }
}

@Composable
private fun EffectSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = if (value > 0) "+${value.toInt()}" else "${value.toInt()}",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryOrange
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
        )
    }
}

/**
 * Audio Editing Studio Panel (Section 12 — matching 'تحرير الصوت' screenshot)
 */
@Composable
fun AudioStudioPanel(
    isArabic: Boolean,
    audioConfig: AudioTrackConfig,
    onUpdateAudio: ((AudioTrackConfig) -> AudioTrackConfig) -> Unit,
    onImportMusic: () -> Unit,
    onImportSoundEffect: () -> Unit,
    onRemoveAudioClip: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = PrimaryOrange)
                Text(
                    text = if (isArabic) "الصوت الأصلي (${(audioConfig.originalVolume * 100).toInt()}%)" else "Original Audio (${(audioConfig.originalVolume * 100).toInt()}%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            FilterChip(
                selected = audioConfig.isOriginalMuted,
                onClick = { onUpdateAudio { it.copy(isOriginalMuted = !it.isOriginalMuted) } },
                label = { Text(if (isArabic) "كتم الصوت الأصلي" else "Mute Original") }
            )
        }

        Slider(
            value = if (audioConfig.isOriginalMuted) 0f else audioConfig.originalVolume,
            onValueChange = { v -> onUpdateAudio { it.copy(originalVolume = v, isOriginalMuted = v == 0f) } },
            valueRange = 0f..1.5f,
            colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
        )

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onImportMusic,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isArabic) "إضافة موسيقى خلفية" else "Add Music")
            }
            OutlinedButton(
                onClick = onImportSoundEffect,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isArabic) "إضافة مؤثر صوتي" else "Add SFX")
            }
        }

        audioConfig.additionalClips.forEach { clip ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(clip.name, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onRemoveAudioClip(clip.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = StudioError)
                        }
                    }
                    Text(
                        text = if (isArabic) "مستوى الصوت: ${(clip.volume * 100).toInt()}%" else "Volume: ${(clip.volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = clip.volume,
                        onValueChange = { v ->
                            onUpdateAudio { cfg ->
                                cfg.copy(
                                    additionalClips = cfg.additionalClips.map {
                                        if (it.id == clip.id) it.copy(volume = v) else it
                                    }
                                )
                            }
                        },
                        valueRange = 0f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryOrange, activeTrackColor = PrimaryOrange)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isArabic) "تدرج الدخول (Fade In)" else "Fade In")
                        Switch(
                            checked = clip.fadeInMs > 0L,
                            onCheckedChange = { enabled ->
                                onUpdateAudio { cfg ->
                                    cfg.copy(
                                        additionalClips = cfg.additionalClips.map {
                                            if (it.id == clip.id) it.copy(fadeInMs = if (enabled) 800L else 0L) else it
                                        }
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryOrange)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isArabic) "تدرج الخروج (Fade Out)" else "Fade Out")
                        Switch(
                            checked = clip.fadeOutMs > 0L,
                            onCheckedChange = { enabled ->
                                onUpdateAudio { cfg ->
                                    cfg.copy(
                                        additionalClips = cfg.additionalClips.map {
                                            if (it.id == clip.id) it.copy(fadeOutMs = if (enabled) 800L else 0L) else it
                                        }
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryOrange)
                        )
                    }
                }
            }
        }
    }
}

/**
 * AI Smart Editing Tools Panel (Section 17 — matching 'أدوات الذكاء الاصطناعي' screenshot)
 */
@Composable
fun AiSmartToolsPanel(
    isArabic: Boolean,
    isLoading: Boolean,
    errorBanner: String?,
    activeSuggestion: AiSmartSuggestion?,
    onRunTool: (Int) -> Unit,
    onApplySuggestion: (AiSmartSuggestion) -> Unit,
    onDismissSuggestion: () -> Unit
) {
    val tools = listOf(
        Triple(1, Icons.Default.ClosedCaption, if (isArabic) "توليد الكابشن تلقائياً" else "Automatic Captions"),
        Triple(2, Icons.Default.Language, if (isArabic) "اكتشاف اللغة واللهجة تلقائياً" else "Auto Language & Dialect Detection"),
        Triple(3, Icons.Default.Spellcheck, if (isArabic) "تصحيح النص وتدقيق الإملاء" else "Transcript Correction Assistance"),
        Triple(4, Icons.Default.FormatQuote, if (isArabic) "الترقيم التلقائي الذكي" else "Automatic Punctuation"),
        Triple(5, Icons.Default.Segment, if (isArabic) "تقسيم ذكي للجمل" else "Smart Sentence Segmentation"),
        Triple(6, Icons.Default.ContentCut, if (isArabic) "إزالة فترات الصمت الطويلة" else "Remove Long Silences"),
        Triple(7, Icons.Default.MovieFilter, if (isArabic) "اكتشاف تغيرات المشاهد" else "Identify Possible Scene Changes"),
        Triple(8, Icons.Default.AutoFixHigh, if (isArabic) "اقتراح قالب مناسب" else "Suggest Caption Template"),
        Triple(9, Icons.Default.Highlight, if (isArabic) "اقتراح الكلمات المميزة" else "Suggest Highlight Words"),
        Triple(10, Icons.Default.Description, if (isArabic) "توليد وصف قصير للفيديو" else "Generate Short Video Description")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (isArabic) "أدوات الذكاء الاصطناعي (Gemini AI)" else "AI Smart Editing Tools (Gemini AI)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryOrange
            )
        }

        if (!errorBanner.isNullOrBlank()) {
            Surface(
                color = StudioError.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorBanner,
                    color = StudioError,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // AI Suggestion Review Card (requires explicit user confirmation before applying!)
        if (activeSuggestion != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryOrange.copy(alpha = 0.18f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, PrimaryOrange, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isArabic) "اقتراح الذكاء الاصطناعي: ${activeSuggestion.toolTitleAr}" else "AI Suggestion: ${activeSuggestion.toolTitleEn}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Text(
                        text = if (isArabic) activeSuggestion.summaryAr else activeSuggestion.summaryEn,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!activeSuggestion.generatedDescription.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activeSuggestion.generatedDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismissSuggestion) {
                            Text(if (isArabic) "تجاهل" else "Dismiss")
                        }
                        if (activeSuggestion.updatedSegments != null || !activeSuggestion.suggestedTemplateId.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onApplySuggestion(activeSuggestion) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Text(if (isArabic) "تأكيد وتطبيق التعديل" else "Confirm & Apply")
                            }
                        }
                    }
                }
            }
        }

        // List of 10 AI Tools
        tools.forEach { (index, icon, title) ->
            Surface(
                onClick = { onRunTool(index) },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_tool_$index")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(icon, contentDescription = title, tint = PrimaryOrange)
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
