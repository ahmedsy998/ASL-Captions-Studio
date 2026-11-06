package com.example.data.local

import com.example.domain.model.*

/**
 * Pure-Kotlin deterministic serializer/deserializer for Room Database entities.
 * Works identically on Android runtime and pure JVM unit tests without relying on
 * Android's stubbed org.json classes in unit test environments.
 */
object ProjectJsonSerializer {

    private const val FIELD_SEP = "\u001F"
    private const val ITEM_SEP = "\u001E"
    private const val WORD_SEP = "\u001D"
    private const val SUB_SEP = "\u001C"

    private fun esc(s: String): String = s
        .replace("\\", "\\\\")
        .replace(FIELD_SEP, " ")
        .replace(ITEM_SEP, " ")
        .replace(WORD_SEP, " ")
        .replace(SUB_SEP, " ")

    private fun unesc(s: String): String = s.replace("\\\\", "\\")

    fun serializeSegments(segments: List<CaptionSegment>): String {
        if (segments.isEmpty()) return ""
        return segments.joinToString(ITEM_SEP) { seg ->
            val wordsEncoded = seg.words.joinToString(WORD_SEP) { w ->
                listOf(
                    esc(w.id),
                    esc(w.text),
                    w.startTimeMs.toString(),
                    w.endTimeMs.toString(),
                    w.confidence.toString(),
                    w.isHighlighted.toString()
                ).joinToString(SUB_SEP)
            }
            val styleEncoded = seg.styleOverride?.let { serializeStyle(it) } ?: ""
            listOf(
                esc(seg.id),
                esc(seg.text),
                seg.startTimeMs.toString(),
                seg.endTimeMs.toString(),
                seg.isSelected.toString(),
                wordsEncoded,
                styleEncoded
            ).joinToString(FIELD_SEP)
        }
    }

    fun deserializeSegments(encoded: String): List<CaptionSegment> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(ITEM_SEP).mapNotNull { item ->
            val parts = item.split(FIELD_SEP)
            if (parts.size < 6) return@mapNotNull null
            val id = unesc(parts[0])
            val text = unesc(parts[1])
            val startMs = parts[2].toLongOrNull() ?: 0L
            val endMs = parts[3].toLongOrNull() ?: 2000L
            val isSelected = parts[4].toBooleanStrictOrNull() ?: false
            val wordsRaw = parts[5]
            val words = if (wordsRaw.isBlank()) emptyList() else {
                wordsRaw.split(WORD_SEP).mapNotNull { wStr ->
                    val wParts = wStr.split(SUB_SEP)
                    if (wParts.size < 6) null else {
                        CaptionWord(
                            id = unesc(wParts[0]),
                            text = unesc(wParts[1]),
                            startTimeMs = wParts[2].toLongOrNull() ?: 0L,
                            endTimeMs = wParts[3].toLongOrNull() ?: 500L,
                            confidence = wParts[4].toFloatOrNull() ?: 0.98f,
                            isHighlighted = wParts[5].toBooleanStrictOrNull() ?: false
                        )
                    }
                }
            }
            val styleOverride = if (parts.size >= 7 && parts[6].isNotBlank()) {
                deserializeStyle(parts[6])
            } else null

            CaptionSegment(
                id = id,
                text = text,
                words = words,
                startTimeMs = startMs,
                endTimeMs = endMs,
                styleOverride = styleOverride,
                isSelected = isSelected
            )
        }
    }

    fun serializeStyle(style: CaptionStyle): String {
        return listOf(
            esc(style.fontFamilyName),
            style.fontSizeSp.toString(),
            style.fontWeight.toString(),
            style.textColorArgb.toString(),
            style.activeWordColorArgb.toString(),
            style.textAlignment.name,
            style.lineSpacingMultiplier.toString(),
            style.letterSpacingEm.toString(),
            style.maxWordsPerLine.toString(),
            style.strokeColorArgb.toString(),
            style.strokeThicknessPx.toString(),
            style.shadowColorArgb.toString(),
            style.shadowBlurRadius.toString(),
            style.shadowOffsetX.toString(),
            style.shadowOffsetY.toString(),
            style.backgroundStyle.name,
            style.backgroundColorArgb.toString(),
            style.backgroundOpacity.toString(),
            style.backgroundPaddingH.toString(),
            style.backgroundPaddingV.toString(),
            style.backgroundCornerRadius.toString(),
            style.positionPreset.name,
            style.customPositionX.toString(),
            style.customPositionY.toString(),
            style.animation.preset.name,
            style.animation.durationMs.toString(),
            style.animation.intensity.toString(),
            style.animation.startOffsetMs.toString(),
            style.animation.endOffsetMs.toString(),
            style.animation.easing.name,
            style.animation.direction.name,
            style.animation.delayMs.toString()
        ).joinToString("|")
    }

    fun deserializeStyle(encoded: String): CaptionStyle {
        if (encoded.isBlank()) return CaptionStyle()
        val p = encoded.split("|")
        if (p.size < 32) return CaptionStyle()
        return CaptionStyle(
            fontFamilyName = unesc(p[0]),
            fontSizeSp = p[1].toFloatOrNull() ?: 36f,
            fontWeight = p[2].toIntOrNull() ?: 700,
            textColorArgb = p[3].toLongOrNull() ?: 0xFFFFFFFFL,
            activeWordColorArgb = p[4].toLongOrNull() ?: 0xFFE8872FL,
            textAlignment = runCatching { CaptionTextAlignment.valueOf(p[5]) }.getOrDefault(CaptionTextAlignment.CENTER),
            lineSpacingMultiplier = p[6].toFloatOrNull() ?: 1.25f,
            letterSpacingEm = p[7].toFloatOrNull() ?: 0f,
            maxWordsPerLine = p[8].toIntOrNull() ?: 5,
            strokeColorArgb = p[9].toLongOrNull() ?: 0xFF1D2227L,
            strokeThicknessPx = p[10].toFloatOrNull() ?: 4f,
            shadowColorArgb = p[11].toLongOrNull() ?: 0xAA000000L,
            shadowBlurRadius = p[12].toFloatOrNull() ?: 8f,
            shadowOffsetX = p[13].toFloatOrNull() ?: 2f,
            shadowOffsetY = p[14].toFloatOrNull() ?: 4f,
            backgroundStyle = runCatching { CaptionBackgroundStyle.valueOf(p[15]) }.getOrDefault(CaptionBackgroundStyle.ROUNDED_RECT),
            backgroundColorArgb = p[16].toLongOrNull() ?: 0xFF1D2227L,
            backgroundOpacity = p[17].toFloatOrNull() ?: 0.72f,
            backgroundPaddingH = p[18].toFloatOrNull() ?: 16f,
            backgroundPaddingV = p[19].toFloatOrNull() ?: 8f,
            backgroundCornerRadius = p[20].toFloatOrNull() ?: 12f,
            positionPreset = runCatching { CaptionPositionPreset.valueOf(p[21]) }.getOrDefault(CaptionPositionPreset.BOTTOM),
            customPositionX = p[22].toFloatOrNull() ?: 0.5f,
            customPositionY = p[23].toFloatOrNull() ?: 0.8f,
            animation = AnimationParameters(
                preset = runCatching { CaptionAnimationPreset.valueOf(p[24]) }.getOrDefault(CaptionAnimationPreset.WORD_HIGHLIGHT),
                durationMs = p[25].toLongOrNull() ?: 350L,
                intensity = p[26].toFloatOrNull() ?: 1.0f,
                startOffsetMs = p[27].toLongOrNull() ?: 0L,
                endOffsetMs = p[28].toLongOrNull() ?: 0L,
                easing = runCatching { AnimationEasing.valueOf(p[29]) }.getOrDefault(AnimationEasing.EASE_IN_OUT),
                direction = runCatching { AnimationDirection.valueOf(p[30]) }.getOrDefault(AnimationDirection.NORMAL),
                delayMs = p[31].toLongOrNull() ?: 0L
            )
        )
    }

    fun serializeEffects(effects: VideoEffectsConfig): String {
        return listOf(
            effects.brightness.toString(),
            effects.contrast.toString(),
            effects.saturation.toString(),
            effects.exposure.toString(),
            effects.temperature.toString(),
            effects.tint.toString(),
            effects.vignette.toString(),
            effects.filmGrain.toString(),
            effects.blur.toString(),
            effects.sharpen.toString(),
            effects.cropRatio.name,
            effects.scale.toString(),
            effects.rotationDegrees.toString(),
            effects.flipHorizontal.toString(),
            effects.flipVertical.toString(),
            effects.fadeInMs.toString(),
            effects.fadeOutMs.toString(),
            effects.zoomInAmount.toString(),
            effects.zoomOutAmount.toString(),
            effects.colorGradingPreset.name
        ).joinToString("|")
    }

    fun deserializeEffects(encoded: String): VideoEffectsConfig {
        if (encoded.isBlank()) return VideoEffectsConfig()
        val p = encoded.split("|")
        if (p.size < 20) return VideoEffectsConfig()
        return VideoEffectsConfig(
            brightness = p[0].toFloatOrNull() ?: 0f,
            contrast = p[1].toFloatOrNull() ?: 0f,
            saturation = p[2].toFloatOrNull() ?: 0f,
            exposure = p[3].toFloatOrNull() ?: 0f,
            temperature = p[4].toFloatOrNull() ?: 0f,
            tint = p[5].toFloatOrNull() ?: 0f,
            vignette = p[6].toFloatOrNull() ?: 0f,
            filmGrain = p[7].toFloatOrNull() ?: 0f,
            blur = p[8].toFloatOrNull() ?: 0f,
            sharpen = p[9].toFloatOrNull() ?: 0f,
            cropRatio = runCatching { AspectRatioPreset.valueOf(p[10]) }.getOrDefault(AspectRatioPreset.RATIO_16_9),
            scale = p[11].toFloatOrNull() ?: 1.0f,
            rotationDegrees = p[12].toFloatOrNull() ?: 0f,
            flipHorizontal = p[13].toBooleanStrictOrNull() ?: false,
            flipVertical = p[14].toBooleanStrictOrNull() ?: false,
            fadeInMs = p[15].toLongOrNull() ?: 0L,
            fadeOutMs = p[16].toLongOrNull() ?: 0L,
            zoomInAmount = p[17].toFloatOrNull() ?: 0f,
            zoomOutAmount = p[18].toFloatOrNull() ?: 0f,
            colorGradingPreset = runCatching { ColorGradingPreset.valueOf(p[19]) }.getOrDefault(ColorGradingPreset.NONE)
        )
    }

    fun serializeAudioConfig(audio: AudioTrackConfig): String {
        val clipsStr = audio.additionalClips.joinToString(ITEM_SEP) { c ->
            listOf(
                esc(c.id),
                esc(c.name),
                esc(c.uriString),
                c.isSoundEffect.toString(),
                c.volume.toString(),
                c.fadeInMs.toString(),
                c.fadeOutMs.toString(),
                c.trimStartMs.toString(),
                c.trimEndMs.toString(),
                c.timelineStartMs.toString()
            ).joinToString(SUB_SEP)
        }
        return listOf(
            audio.originalVolume.toString(),
            audio.isOriginalMuted.toString(),
            clipsStr
        ).joinToString(FIELD_SEP)
    }

    fun deserializeAudioConfig(encoded: String): AudioTrackConfig {
        if (encoded.isBlank()) return AudioTrackConfig()
        val p = encoded.split(FIELD_SEP)
        if (p.size < 2) return AudioTrackConfig()
        val origVol = p[0].toFloatOrNull() ?: 1.0f
        val muted = p[1].toBooleanStrictOrNull() ?: false
        val clipsRaw = p.getOrNull(2).orEmpty()
        val clips = if (clipsRaw.isBlank()) emptyList() else {
            clipsRaw.split(ITEM_SEP).mapNotNull { item ->
                val cp = item.split(SUB_SEP)
                if (cp.size < 10) null else {
                    AudioClip(
                        id = unesc(cp[0]),
                        name = unesc(cp[1]),
                        uriString = unesc(cp[2]),
                        isSoundEffect = cp[3].toBooleanStrictOrNull() ?: false,
                        volume = cp[4].toFloatOrNull() ?: 0.6f,
                        fadeInMs = cp[5].toLongOrNull() ?: 500L,
                        fadeOutMs = cp[6].toLongOrNull() ?: 500L,
                        trimStartMs = cp[7].toLongOrNull() ?: 0L,
                        trimEndMs = cp[8].toLongOrNull() ?: 30_000L,
                        timelineStartMs = cp[9].toLongOrNull() ?: 0L
                    )
                }
            }
        }
        return AudioTrackConfig(originalVolume = origVol, isOriginalMuted = muted, additionalClips = clips)
    }

    fun serializeTimelineSegments(clips: List<TimelineVideoSegment>): String {
        if (clips.isEmpty()) return ""
        return clips.joinToString(ITEM_SEP) { c ->
            listOf(
                esc(c.id),
                c.sourceStartMs.toString(),
                c.sourceEndMs.toString(),
                c.timelineStartMs.toString(),
                esc(c.label)
            ).joinToString(SUB_SEP)
        }
    }

    fun deserializeTimelineSegments(encoded: String): List<TimelineVideoSegment> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(ITEM_SEP).mapNotNull { item ->
            val p = item.split(SUB_SEP)
            if (p.size < 5) null else {
                TimelineVideoSegment(
                    id = unesc(p[0]),
                    sourceStartMs = p[1].toLongOrNull() ?: 0L,
                    sourceEndMs = p[2].toLongOrNull() ?: 8000L,
                    timelineStartMs = p[3].toLongOrNull() ?: 0L,
                    label = unesc(p[4])
                )
            }
        }
    }

    fun serializeExportConfig(config: ExportConfig): String {
        return listOf(
            config.resolution.name,
            config.frameRate.toString(),
            config.bitrateMbps.toString(),
            config.audioQualityKbps.toString(),
            config.audioVolume.toString(),
            esc(config.outputFileName),
            esc(config.destinationFolder)
        ).joinToString("|")
    }

    fun deserializeExportConfig(encoded: String): ExportConfig {
        if (encoded.isBlank()) return ExportConfig()
        val p = encoded.split("|")
        if (p.size < 7) return ExportConfig()
        return ExportConfig(
            resolution = runCatching { ExportResolution.valueOf(p[0]) }.getOrDefault(ExportResolution.RES_1080P),
            frameRate = p[1].toIntOrNull() ?: 30,
            bitrateMbps = p[2].toIntOrNull() ?: 12,
            audioQualityKbps = p[3].toIntOrNull() ?: 192,
            audioVolume = p[4].toFloatOrNull() ?: 1.0f,
            outputFileName = unesc(p[5]),
            destinationFolder = unesc(p[6])
        )
    }
}
