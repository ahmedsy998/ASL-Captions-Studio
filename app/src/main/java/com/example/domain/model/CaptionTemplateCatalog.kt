package com.example.domain.model

object CaptionTemplateCatalog {

    val builtInTemplates: List<CaptionTemplate> = listOf(
        // ==================== PODCAST (4) ====================
        CaptionTemplate(
            id = "tpl_podcast_pro",
            nameAr = "بودكاست احترافي",
            nameEn = "Podcast Pro",
            category = TemplateCategory.PODCAST,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Tajawal",
                fontSizeSp = 40f,
                fontWeight = 700,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
                backgroundColorArgb = 0xFF1D2227L,
                backgroundOpacity = 0.82f,
                backgroundCornerRadius = 14f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.WORD_HIGHLIGHT, durationMs = 300L)
            )
        ),
        CaptionTemplate(
            id = "tpl_modern_interview",
            nameAr = "مقابلة حديثة",
            nameEn = "Modern Interview",
            category = TemplateCategory.PODCAST,
            captionMode = CaptionDisplayMode.FULL_SENTENCE,
            style = CaptionStyle(
                fontFamilyName = "Cairo",
                fontSizeSp = 34f,
                fontWeight = 700,
                textColorArgb = 0xFFF5F1EAL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
                backgroundColorArgb = 0xFF343A40L,
                backgroundOpacity = 0.78f,
                backgroundCornerRadius = 10f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.SLIDE_UP, durationMs = 350L)
            )
        ),
        CaptionTemplate(
            id = "tpl_clean_podcast",
            nameAr = "بودكاست نقي",
            nameEn = "Clean Podcast",
            category = TemplateCategory.PODCAST,
            captionMode = CaptionDisplayMode.FULL_SENTENCE,
            style = CaptionStyle(
                fontFamilyName = "Tajawal",
                fontSizeSp = 32f,
                fontWeight = 600,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFFFD600L,
                backgroundStyle = CaptionBackgroundStyle.NONE,
                strokeColorArgb = 0xFF1D2227L,
                strokeThicknessPx = 5f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.FADE_IN, durationMs = 280L)
            )
        ),
        CaptionTemplate(
            id = "tpl_speaker_focus",
            nameAr = "تركيز المتحدث",
            nameEn = "Speaker Focus",
            category = TemplateCategory.PODCAST,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Cairo",
                fontSizeSp = 42f,
                fontWeight = 800,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.SOLID,
                backgroundColorArgb = 0xFF1D2227L,
                backgroundOpacity = 0.90f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.POP_UP, durationMs = 260L)
            )
        ),

        // ==================== SOCIAL MEDIA (4) ====================
        CaptionTemplate(
            id = "tpl_viral_shorts",
            nameAr = "شورتس تفاعلي",
            nameEn = "Viral Shorts",
            category = TemplateCategory.SOCIAL_MEDIA,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Montserrat",
                fontSizeSp = 46f,
                fontWeight = 800,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFFFD600L,
                strokeColorArgb = 0xFF000000L,
                strokeThicknessPx = 7f,
                backgroundStyle = CaptionBackgroundStyle.NONE,
                positionPreset = CaptionPositionPreset.CENTER,
                animation = AnimationParameters(preset = CaptionAnimationPreset.BOUNCE, durationMs = 320L, intensity = 1.3f)
            )
        ),
        CaptionTemplate(
            id = "tpl_reels_dynamic",
            nameAr = "ريلز ديناميكي",
            nameEn = "Reels Dynamic",
            category = TemplateCategory.SOCIAL_MEDIA,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Cairo",
                fontSizeSp = 44f,
                fontWeight = 800,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
                backgroundColorArgb = 0xFF1D2227L,
                backgroundOpacity = 0.85f,
                backgroundCornerRadius = 18f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.SCALE_PULSE, durationMs = 300L, intensity = 1.2f)
            )
        ),
        CaptionTemplate(
            id = "tpl_tiktok_bold",
            nameAr = "تيك توك عريض",
            nameEn = "TikTok Bold",
            category = TemplateCategory.SOCIAL_MEDIA,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Changa",
                fontSizeSp = 48f,
                fontWeight = 800,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFF2E9D68L,
                strokeColorArgb = 0xFF1D2227L,
                strokeThicknessPx = 8f,
                backgroundStyle = CaptionBackgroundStyle.NONE,
                animation = AnimationParameters(preset = CaptionAnimationPreset.ELASTIC, durationMs = 360L)
            )
        ),
        CaptionTemplate(
            id = "tpl_creator_style",
            nameAr = "نمط المبدعين",
            nameEn = "Creator Style",
            category = TemplateCategory.SOCIAL_MEDIA,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Tajawal",
                fontSizeSp = 42f,
                fontWeight = 700,
                textColorArgb = 0xFFF5F1EAL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
                backgroundColorArgb = 0xFFE8872FL,
                backgroundOpacity = 0.25f,
                backgroundCornerRadius = 16f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.SPRING, durationMs = 340L)
            )
        ),

        // ==================== CINEMATIC (4) ====================
        CaptionTemplate(
            id = "tpl_cinematic_white",
            nameAr = "سينمائي أبيض",
            nameEn = "Cinematic White",
            category = TemplateCategory.CINEMATIC,
            captionMode = CaptionDisplayMode.FULL_SENTENCE,
            style = CaptionStyle(
                fontFamilyName = "Tajawal",
                fontSizeSp = 30f,
                fontWeight = 500,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFFFFFFFL,
                letterSpacingEm = 0.04f,
                backgroundStyle = CaptionBackgroundStyle.NONE,
                strokeThicknessPx = 2f,
                shadowBlurRadius = 10f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.SMOOTH_REVEAL, durationMs = 450L)
            )
        ),
        CaptionTemplate(
            id = "tpl_film_subtitle",
            nameAr = "ترجمة أفلام",
            nameEn = "Film Subtitle",
            category = TemplateCategory.CINEMATIC,
            captionMode = CaptionDisplayMode.FULL_SENTENCE,
            style = CaptionStyle(
                fontFamilyName = "Tajawal",
                fontSizeSp = 32f,
                fontWeight = 600,
                textColorArgb = 0xFFFFD600L,
                activeWordColorArgb = 0xFFFFD600L,
                strokeColorArgb = 0xFF000000L,
                strokeThicknessPx = 4f,
                backgroundStyle = CaptionBackgroundStyle.NONE,
                animation = AnimationParameters(preset = CaptionAnimationPreset.FADE_IN, durationMs = 300L)
            )
        ),
        CaptionTemplate(
            id = "tpl_documentary",
            nameAr = "وثائقي",
            nameEn = "Documentary",
            category = TemplateCategory.CINEMATIC,
            captionMode = CaptionDisplayMode.FULL_SENTENCE,
            style = CaptionStyle(
                fontFamilyName = "Amiri",
                fontSizeSp = 34f,
                fontWeight = 700,
                textColorArgb = 0xFFF5F1EAL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.TRANSPARENT,
                backgroundColorArgb = 0xFF000000L,
                backgroundOpacity = 0.55f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.TRACKING_REVEAL, durationMs = 420L)
            )
        ),
        CaptionTemplate(
            id = "tpl_minimal_cinema",
            nameAr = "سينما هادئة",
            nameEn = "Minimal Cinema",
            category = TemplateCategory.CINEMATIC,
            captionMode = CaptionDisplayMode.FULL_SENTENCE,
            style = CaptionStyle(
                fontFamilyName = "Montserrat",
                fontSizeSp = 28f,
                fontWeight = 500,
                textColorArgb = 0xFFC5C8CBL,
                activeWordColorArgb = 0xFFFFFFFFL,
                backgroundStyle = CaptionBackgroundStyle.NONE,
                strokeThicknessPx = 0f,
                shadowBlurRadius = 12f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.BLUR_IN, durationMs = 400L)
            )
        ),

        // ==================== DYNAMIC (4) ====================
        CaptionTemplate(
            id = "tpl_word_highlight",
            nameAr = "كلمات متحركة",
            nameEn = "Word Highlight",
            category = TemplateCategory.DYNAMIC,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Tajawal",
                fontSizeSp = 42f,
                fontWeight = 800,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
                backgroundColorArgb = 0xFF1D2227L,
                backgroundOpacity = 0.80f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.WORD_HIGHLIGHT, durationMs = 280L)
            )
        ),
        CaptionTemplate(
            id = "tpl_karaoke",
            nameAr = "كاريوكي",
            nameEn = "Karaoke",
            category = TemplateCategory.DYNAMIC,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Cairo",
                fontSizeSp = 40f,
                fontWeight = 700,
                textColorArgb = 0xFFC5C8CBL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
                backgroundColorArgb = 0xFF1D2227L,
                backgroundOpacity = 0.85f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.KARAOKE, durationMs = 350L)
            )
        ),
        CaptionTemplate(
            id = "tpl_neon",
            nameAr = "نيون متوهج",
            nameEn = "Neon",
            category = TemplateCategory.DYNAMIC,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Changa",
                fontSizeSp = 44f,
                fontWeight = 800,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFAB47BCL,
                shadowColorArgb = 0xFFAB47BCL,
                shadowBlurRadius = 22f,
                backgroundStyle = CaptionBackgroundStyle.NONE,
                animation = AnimationParameters(preset = CaptionAnimationPreset.NEON_GLOW, durationMs = 360L, intensity = 1.4f)
            )
        ),
        CaptionTemplate(
            id = "tpl_bounce_pop",
            nameAr = "ارتداد حيوي",
            nameEn = "Bounce Pop",
            category = TemplateCategory.DYNAMIC,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Cairo",
                fontSizeSp = 46f,
                fontWeight = 800,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFE8872FL,
                strokeColorArgb = 0xFF1D2227L,
                strokeThicknessPx = 6f,
                backgroundStyle = CaptionBackgroundStyle.NONE,
                animation = AnimationParameters(preset = CaptionAnimationPreset.BOUNCE, durationMs = 300L, intensity = 1.35f)
            )
        ),

        // ==================== ARABIC (4) ====================
        CaptionTemplate(
            id = "tpl_arabic_modern",
            nameAr = "عربي حديث",
            nameEn = "Arabic Modern",
            category = TemplateCategory.ARABIC,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Tajawal",
                fontSizeSp = 42f,
                fontWeight = 700,
                textColorArgb = 0xFFF5F1EAL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
                backgroundColorArgb = 0xFF1D2227L,
                backgroundOpacity = 0.84f,
                backgroundCornerRadius = 14f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.ANIMATED_BACKGROUND, durationMs = 320L)
            )
        ),
        CaptionTemplate(
            id = "tpl_arabic_bold",
            nameAr = "عربي عريض",
            nameEn = "Arabic Bold",
            category = TemplateCategory.ARABIC,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Cairo",
                fontSizeSp = 46f,
                fontWeight = 800,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFFFD600L,
                strokeColorArgb = 0xFF1D2227L,
                strokeThicknessPx = 6f,
                backgroundStyle = CaptionBackgroundStyle.SOLID,
                backgroundColorArgb = 0xFF343A40L,
                backgroundOpacity = 0.88f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.ZOOM_IN, durationMs = 300L)
            )
        ),
        CaptionTemplate(
            id = "tpl_arabic_elegant",
            nameAr = "عربي أنيق",
            nameEn = "Arabic Elegant",
            category = TemplateCategory.ARABIC,
            captionMode = CaptionDisplayMode.FULL_SENTENCE,
            style = CaptionStyle(
                fontFamilyName = "Amiri",
                fontSizeSp = 38f,
                fontWeight = 700,
                textColorArgb = 0xFFF5F1EAL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
                backgroundColorArgb = 0xFF1D2227L,
                backgroundOpacity = 0.75f,
                backgroundCornerRadius = 20f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.SMOOTH_REVEAL, durationMs = 400L)
            )
        ),
        CaptionTemplate(
            id = "tpl_arabic_dynamic",
            nameAr = "عربي ديناميكي",
            nameEn = "Arabic Dynamic",
            category = TemplateCategory.ARABIC,
            captionMode = CaptionDisplayMode.WORD_BY_WORD,
            style = CaptionStyle(
                fontFamilyName = "Changa",
                fontSizeSp = 44f,
                fontWeight = 800,
                textColorArgb = 0xFFFFFFFFL,
                activeWordColorArgb = 0xFFE8872FL,
                backgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
                backgroundColorArgb = 0xFF1D2227L,
                backgroundOpacity = 0.85f,
                animation = AnimationParameters(preset = CaptionAnimationPreset.WAVE, durationMs = 360L, intensity = 1.25f)
            )
        )
    )
}
