package com.example.domain.model

import java.util.UUID

enum class AppLanguage(val code: String, val displayNameAr: String, val displayNameEn: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية (RTL)", "Arabic (RTL)", true),
    ENGLISH("en", "الإنجليزية (LTR)", "English (LTR)", false)
}

enum class TranscriptionLanguage(val bcp47Code: String, val labelAr: String, val labelEn: String) {
    AUTO("auto", "اكتشاف تلقائي", "Auto Detect"),
    ARABIC_MSA("ar-SA", "العربية الفصحى", "Arabic (Modern Standard)"),
    ARABIC_SYRIAN("ar-SY", "العربية (لهجة سورية / شامية)", "Arabic (Syrian / Levantine)"),
    ARABIC_EGYPTIAN("ar-EG", "العربية (لهجة مصرية)", "Arabic (Egyptian)"),
    ARABIC_GULF("ar-AE", "العربية (لهجة خليجية)", "Arabic (Gulf)"),
    ENGLISH_US("en-US", "الإنجليزية (أمريكا)", "English (US)"),
    ENGLISH_UK("en-GB", "الإنجليزية (بريطانيا)", "English (UK)")
}

enum class AspectRatioPreset(
    val id: String,
    val labelAr: String,
    val labelEn: String,
    val widthRatio: Int,
    val heightRatio: Int
) {
    RATIO_9_16("9:16", "9:16 (ريلز / تيك توك / شورتس)", "9:16 (TikTok, Reels, Shorts)", 9, 16),
    RATIO_16_9("16:9", "16:9 (يوتيوب / عرضي)", "16:9 (YouTube & Landscape)", 16, 9),
    RATIO_1_1("1:1", "1:1 (مربع)", "1:1 (Square Social)", 1, 1),
    RATIO_4_5("4:5", "4:5 (إنستغرام)", "4:5 (Instagram Feed)", 4, 5),
    ORIGINAL("Original", "الأصلي", "Original Ratio", 0, 0);

    fun toFloatRatio(defaultRatio: Float = 16f / 9f): Float {
        return if (widthRatio > 0 && heightRatio > 0) {
            widthRatio.toFloat() / heightRatio.toFloat()
        } else {
            defaultRatio
        }
    }
}

enum class CaptionDisplayMode(val labelAr: String, val labelEn: String) {
    WORD_BY_WORD("كلمة بكلمة", "Word by Word"),
    FULL_SENTENCE("جملة كاملة", "Full Sentence")
}

enum class CaptionTextAlignment(val labelAr: String, val labelEn: String) {
    START("محاذاة للبداية", "Start"),
    CENTER("توسيط", "Center"),
    END("محاذاة للنهاية", "End")
}

enum class CaptionBackgroundStyle(val labelAr: String, val labelEn: String) {
    NONE("بدون خلفية", "No Background"),
    SOLID("خلفية مصمتة", "Solid Background"),
    TRANSPARENT("خلفية شفافة", "Transparent"),
    ROUNDED_RECT("مستطيل بحواف دائرية", "Rounded Rectangle")
}

enum class CaptionPositionPreset(val labelAr: String, val labelEn: String, val defaultY: Float) {
    TOP("أعلى", "Top", 0.16f),
    CENTER("وسط", "Center", 0.50f),
    BOTTOM("أسفل", "Bottom", 0.80f),
    CUSTOM("مخصص (X / Y)", "Custom (X / Y)", 0.80f)
}

enum class AnimationEasing(val labelAr: String, val labelEn: String) {
    LINEAR("خطي", "Linear"),
    EASE_IN_OUT("سلس", "Ease In Out"),
    OVERSHOOT("حيوي", "Overshoot"),
    SPRING("نابض", "Spring")
}

enum class AnimationDirection(val labelAr: String, val labelEn: String) {
    NORMAL("عادي", "Normal"),
    REVERSE("عكسي", "Reverse"),
    ALTERNATE("متناوب", "Alternate")
}

/**
 * Complete 25 professional caption animation presets (Section 9)
 */
enum class CaptionAnimationPreset(
    val id: String,
    val nameAr: String,
    val nameEn: String
) {
    POP_UP("pop_up", "انبثاق (Pop Up)", "Pop Up"),
    BOUNCE("bounce", "ارتداد (Bounce)", "Bounce"),
    FADE_IN("fade_in", "ظهور تدريجي", "Fade In"),
    FADE_OUT("fade_out", "تلاشي تدريجي", "Fade Out"),
    SLIDE_UP("slide_up", "انزلاق لأعلى", "Slide Up"),
    SLIDE_DOWN("slide_down", "انزلاق لأسفل", "Slide Down"),
    SLIDE_LEFT("slide_left", "انزلاق لليسار", "Slide Left"),
    SLIDE_RIGHT("slide_right", "انزلاق لليمين", "Slide Right"),
    ZOOM_IN("zoom_in", "تكبير", "Zoom In"),
    ZOOM_OUT("zoom_out", "تصغير", "Zoom Out"),
    TYPEWRITER("typewriter", "كتابة تدريجية", "Typewriter"),
    WORD_HIGHLIGHT("word_highlight", "تمييز الكلمات", "Word Highlight"),
    KARAOKE("karaoke", "كاريوكي", "Karaoke"),
    NEON_GLOW("neon_glow", "توهج نيون", "Neon Glow"),
    SCALE_PULSE("scale_pulse", "نبض الحجم", "Scale Pulse"),
    BLUR_IN("blur_in", "ظهور ضبابي", "Blur In"),
    ROTATE_IN("rotate_in", "دوران للداخل", "Rotate In"),
    ELASTIC("elastic", "حركة مرنة", "Elastic"),
    TRACKING_REVEAL("tracking_reveal", "تمدد الأحرف", "Tracking Reveal"),
    ANIMATED_BACKGROUND("animated_bg", "خلفية متحركة", "Animated Background"),
    WAVE("wave", "موجة", "Wave"),
    FLIP("flip", "انقلاب", "Flip"),
    SPRING("spring", "نابض", "Spring"),
    STRETCH("stretch", "تمدد", "Stretch"),
    SMOOTH_REVEAL("smooth_reveal", "كشف ناعم", "Smooth Reveal")
}

data class AnimationParameters(
    val preset: CaptionAnimationPreset = CaptionAnimationPreset.WORD_HIGHLIGHT,
    val durationMs: Long = 350L,
    val intensity: Float = 1.0f, // 0.2f .. 2.0f
    val startOffsetMs: Long = 0L,
    val endOffsetMs: Long = 0L,
    val easing: AnimationEasing = AnimationEasing.EASE_IN_OUT,
    val direction: AnimationDirection = AnimationDirection.NORMAL,
    val delayMs: Long = 0L
)

data class CaptionStyle(
    val fontFamilyName: String = "Tajawal",
    val fontSizeSp: Float = 36f,
    val fontWeight: Int = 700,
    val textColorArgb: Long = 0xFFFFFFFFL,
    val activeWordColorArgb: Long = 0xFFE8872FL,
    val textAlignment: CaptionTextAlignment = CaptionTextAlignment.CENTER,
    val lineSpacingMultiplier: Float = 1.25f,
    val letterSpacingEm: Float = 0.0f,
    val maxWordsPerLine: Int = 5,
    val strokeColorArgb: Long = 0xFF1D2227L,
    val strokeThicknessPx: Float = 4f,
    val shadowColorArgb: Long = 0xAA000000L,
    val shadowBlurRadius: Float = 8f,
    val shadowOffsetX: Float = 2f,
    val shadowOffsetY: Float = 4f,
    val backgroundStyle: CaptionBackgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
    val backgroundColorArgb: Long = 0xFF1D2227L,
    val backgroundOpacity: Float = 0.72f,
    val backgroundPaddingH: Float = 16f,
    val backgroundPaddingV: Float = 8f,
    val backgroundCornerRadius: Float = 12f,
    val positionPreset: CaptionPositionPreset = CaptionPositionPreset.BOTTOM,
    val customPositionX: Float = 0.50f,
    val customPositionY: Float = 0.80f,
    val animation: AnimationParameters = AnimationParameters()
)

data class CaptionWord(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float = 0.98f,
    val isHighlighted: Boolean = false
)

data class CaptionSegment(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val words: List<CaptionWord>,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val styleOverride: CaptionStyle? = null,
    val isSelected: Boolean = false
)

enum class TemplateCategory(val id: String, val labelAr: String, val labelEn: String) {
    ALL("all", "الكل", "All"),
    PODCAST("podcast", "بودكاست", "Podcast"),
    SOCIAL_MEDIA("social", "سوشيال ميديا", "Social Media"),
    CINEMATIC("cinematic", "سينمائي", "Cinematic"),
    DYNAMIC("dynamic", "ديناميكي", "Dynamic"),
    ARABIC("arabic", "عربي", "Arabic"),
    CUSTOM("custom", "مخصص", "Custom")
}

data class CaptionTemplate(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val category: TemplateCategory,
    val captionMode: CaptionDisplayMode,
    val style: CaptionStyle,
    val sampleTextAr: String = "في هذه الحلقة سنتحدث عن أهمية صناعة المحتوى",
    val sampleTextEn: String = "In this episode we explore digital storytelling",
    val isCustom: Boolean = false
)

enum class ColorGradingPreset(val labelAr: String, val labelEn: String) {
    NONE("أصلي", "Original"),
    CINEMATIC("سينمائي", "Cinematic"),
    WARM("دافئ", "Warm"),
    COOL("بارد", "Cool"),
    CONTRAST("تباين", "Contrast"),
    BRIGHT("سطوع", "Bright"),
    ART("آرت", "Art"),
    BLACK_AND_WHITE("أبيض وأسود", "B&W")
}

/**
 * Complete 20 Video Effects & Color Adjustments (Section 11)
 */
data class VideoEffectsConfig(
    val brightness: Float = 0f,       // -100f..100f
    val contrast: Float = 0f,         // -100f..100f
    val saturation: Float = 0f,       // -100f..100f
    val exposure: Float = 0f,         // -100f..100f
    val temperature: Float = 0f,      // -100f..100f
    val tint: Float = 0f,             // -100f..100f
    val vignette: Float = 0f,         // 0f..100f
    val filmGrain: Float = 0f,        // 0f..100f
    val blur: Float = 0f,             // 0f..100f
    val sharpen: Float = 0f,          // 0f..100f
    val cropRatio: AspectRatioPreset = AspectRatioPreset.RATIO_16_9,
    val scale: Float = 1.0f,          // 0.5f..2.5f
    val rotationDegrees: Float = 0f,  // 0f, 90f, 180f, 270f
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val fadeInMs: Long = 0L,          // 0L..3000L
    val fadeOutMs: Long = 0L,         // 0L..3000L
    val zoomInAmount: Float = 0f,     // 0f..100f
    val zoomOutAmount: Float = 0f,    // 0f..100f
    val colorGradingPreset: ColorGradingPreset = ColorGradingPreset.NONE
)

data class AudioClip(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val uriString: String,
    val isSoundEffect: Boolean = false,
    val volume: Float = 0.6f,         // 0f..1.5f
    val fadeInMs: Long = 500L,
    val fadeOutMs: Long = 500L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 30_000L,
    val timelineStartMs: Long = 0L
)

data class AudioTrackConfig(
    val originalVolume: Float = 1.0f, // 0f..1.5f
    val isOriginalMuted: Boolean = false,
    val additionalClips: List<AudioClip> = emptyList()
)

enum class TimelineTrackType(val labelAr: String, val labelEn: String) {
    VIDEO("الفيديو", "Video"),
    ORIGINAL_AUDIO("الصوت الأصلي", "Original Audio"),
    CAPTIONS("الكابشن", "Captions"),
    EFFECTS("التأثيرات", "Effects"),
    ADDITIONAL_AUDIO("موسيقى ومؤثرات", "Music & SFX")
}

data class TimelineVideoSegment(
    val id: String = UUID.randomUUID().toString(),
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val timelineStartMs: Long,
    val label: String = "Clip"
) {
    val durationMs: Long get() = (sourceEndMs - sourceStartMs).coerceAtLeast(100L)
}

enum class ExportResolution(val heightPx: Int, val label: String) {
    RES_720P(720, "720p HD"),
    RES_1080P(1080, "1080p Full HD")
}

data class ExportConfig(
    val resolution: ExportResolution = ExportResolution.RES_1080P,
    val frameRate: Int = 30, // 30 or 60
    val bitrateMbps: Int = 12,
    val audioQualityKbps: Int = 192,
    val audioVolume: Float = 1.0f,
    val outputFileName: String = "ASL_Podcast_Export",
    val destinationFolder: String = "Movies/ASLCaptionsStudio"
)

data class VideoMetadata(
    val uriString: String = "",
    val fileName: String = "",
    val width: Int = 1920,
    val height: Int = 1080,
    val durationMs: Long = 0L,
    val frameRate: Float = 30f,
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "video/mp4",
    val isSupported: Boolean = true,
    val errorMessage: String? = null
)

data class RecentExportItem(
    val id: String = UUID.randomUUID().toString(),
    val projectName: String,
    val outputPath: String,
    val resolutionLabel: String,
    val frameRate: Int,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L
)

enum class EditorToolTab(val labelAr: String, val labelEn: String) {
    CAPTIONS("الكابشن", "Captions"),
    TEXT("النص", "Text"),
    FONTS("الخطوط", "Fonts"),
    ANIMATION("الحركة", "Animation"),
    AUDIO("الصوت", "Audio"),
    BACKGROUND("الخلفية", "Background"),
    EFFECTS("تأثيرات", "Effects"),
    FILTERS("فلاتر", "Filters"),
    CROP("قص وأبعاد", "Crop"),
    AI_TOOLS("الذكاء الاصطناعي", "AI Tools"),
    EXPORT("تصدير", "Export")
}

enum class SpeechProviderType(val id: String, val labelAr: String, val labelEn: String) {
    FIREBASE_GEMINI("firebase_gemini", "Gemini AI Multimodal (سحابي آمن)", "Gemini AI Multimodal (Secure Cloud)"),
    WHISPER_ENDPOINT("whisper_api", "خادم Whisper مخصص (Word Timestamps)", "Custom Whisper Endpoint (Word Timestamps)"),
    LOCAL_ANDROID_SPEECH("local_speech", "محرك التعرف الصوتي المحلي (أندرويد)", "Android On-Device Speech Engine")
}

data class AiSmartSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val toolTitleAr: String,
    val toolTitleEn: String,
    val summaryAr: String,
    val summaryEn: String,
    val updatedSegments: List<CaptionSegment>? = null,
    val suggestedTemplateId: String? = null,
    val generatedDescription: String? = null,
    val sceneChangeTimestampsMs: List<Long> = emptyList(),
    val silenceIntervalsMs: List<Pair<Long, Long>> = emptyList(),
    val isDestructive: Boolean = false
)
