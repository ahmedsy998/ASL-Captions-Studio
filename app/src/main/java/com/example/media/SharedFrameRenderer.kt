package com.example.media

import android.content.Context
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.domain.model.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Shared Rendering Engine (Section 13) used to render video effects and animated
 * captions consistently across preview and final MP4 export.
 */
object SharedFrameRenderer {

    fun buildColorMatrix(effects: VideoEffectsConfig): ColorMatrix {
        val combined = ColorMatrix()

        // 1. Preset grading
        when (effects.colorGradingPreset) {
            ColorGradingPreset.NONE -> {}
            ColorGradingPreset.CINEMATIC -> {
                val m = ColorMatrix().apply { setSaturation(1.18f) }
                combined.postConcat(m)
                combined.postConcat(createContrastBrightnessMatrix(1.12f, -6f, -4f, 6f))
            }
            ColorGradingPreset.WARM -> {
                combined.postConcat(createContrastBrightnessMatrix(1.04f, 16f, 6f, -12f))
            }
            ColorGradingPreset.COOL -> {
                combined.postConcat(createContrastBrightnessMatrix(1.04f, -12f, 2f, 18f))
            }
            ColorGradingPreset.CONTRAST -> {
                combined.postConcat(createContrastBrightnessMatrix(1.28f, -12f, -12f, -12f))
            }
            ColorGradingPreset.BRIGHT -> {
                combined.postConcat(createContrastBrightnessMatrix(1.05f, 22f, 22f, 22f))
            }
            ColorGradingPreset.ART -> {
                val sat = ColorMatrix().apply { setSaturation(1.45f) }
                combined.postConcat(sat)
                combined.postConcat(createContrastBrightnessMatrix(1.15f, 8f, -5f, 12f))
            }
            ColorGradingPreset.BLACK_AND_WHITE -> {
                val bw = ColorMatrix().apply { setSaturation(0f) }
                combined.postConcat(bw)
                combined.postConcat(createContrastBrightnessMatrix(1.15f, 0f, 0f, 0f))
            }
        }

        // 2. Manual Saturation
        if (effects.saturation != 0f) {
            val satValue = (1f + effects.saturation / 100f).coerceIn(0f, 3f)
            val satMatrix = ColorMatrix().apply { setSaturation(satValue) }
            combined.postConcat(satMatrix)
        }

        // 3. Brightness, Contrast, Exposure, Temperature, Tint
        val contrastFactor = (1f + effects.contrast / 100f).coerceIn(0.2f, 2.5f) *
            (1f + effects.exposure / 150f).coerceIn(0.4f, 2.0f)
        val brightnessShift = (effects.brightness * 1.4f) + (effects.exposure * 0.9f)
        val rShift = brightnessShift + (effects.temperature * 0.6f)
        val gShift = brightnessShift + (effects.tint * 0.5f)
        val bShift = brightnessShift - (effects.temperature * 0.6f)

        combined.postConcat(createContrastBrightnessMatrix(contrastFactor, rShift, gShift, bShift))
        return combined
    }

    private fun createContrastBrightnessMatrix(
        contrast: Float,
        rTranslate: Float,
        gTranslate: Float,
        bTranslate: Float
    ): ColorMatrix {
        val offset = 128f * (1f - contrast)
        return ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, offset + rTranslate,
                0f, contrast, 0f, 0f, offset + gTranslate,
                0f, 0f, contrast, 0f, offset + bTranslate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    fun resolveTypeface(context: Context, fontFamilyName: String, fontWeight: Int): Typeface {
        val fontRes = when (fontFamilyName.lowercase()) {
            "cairo" -> R.font.cairo
            "amiri" -> R.font.amiri
            "changa" -> R.font.changa
            "montserrat" -> R.font.montserrat
            else -> R.font.tajawal
        }
        val base = runCatching { ResourcesCompat.getFont(context, fontRes) }.getOrNull() ?: Typeface.DEFAULT
        val style = if (fontWeight >= 700) Typeface.BOLD else Typeface.NORMAL
        return Typeface.create(base, style)
    }

    /**
     * Renders full video frame + effects + animated Arabic/English captions onto target Bitmap
     * for 1080p/720p MP4 encoding.
     */
    fun renderExportFrame(
        context: Context,
        targetCanvas: Canvas,
        sourceFrame: Bitmap?,
        frameWidth: Int,
        frameHeight: Int,
        positionMs: Long,
        totalDurationMs: Long,
        captionMode: CaptionDisplayMode,
        segments: List<CaptionSegment>,
        defaultStyle: CaptionStyle,
        effects: VideoEffectsConfig
    ) {
        // 1. Background Clear
        targetCanvas.drawColor(Color.parseColor("#1D2227"))

        // 2. Compute Zoom / Scale / Rotation / Flip
        val durationSafe = totalDurationMs.coerceAtLeast(1000L)
        val progress = (positionMs.toFloat() / durationSafe.toFloat()).coerceIn(0f, 1f)
        val zoomFactor = 1f + (effects.zoomInAmount / 100f * 0.35f * progress) -
            (effects.zoomOutAmount / 100f * 0.25f * progress)
        val totalScale = (effects.scale * zoomFactor).coerceIn(0.4f, 3.0f)

        targetCanvas.save()
        targetCanvas.translate(frameWidth / 2f, frameHeight / 2f)
        targetCanvas.rotate(effects.rotationDegrees)
        val sx = if (effects.flipHorizontal) -totalScale else totalScale
        val sy = if (effects.flipVertical) -totalScale else totalScale
        targetCanvas.scale(sx, sy)
        targetCanvas.translate(-frameWidth / 2f, -frameHeight / 2f)

        // 3. Draw Source Video Frame with ColorMatrix
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(buildColorMatrix(effects))
        }

        if (sourceFrame != null && !sourceFrame.isRecycled) {
            val srcRatio = sourceFrame.width.toFloat() / sourceFrame.height.coerceAtLeast(1).toFloat()
            val dstRatio = frameWidth.toFloat() / frameHeight.toFloat()
            val dstRect = if (srcRatio > dstRatio) {
                val h = (frameWidth / srcRatio).toInt()
                val top = (frameHeight - h) / 2
                Rect(0, top, frameWidth, top + h)
            } else {
                val w = (frameHeight * srcRatio).toInt()
                val left = (frameWidth - w) / 2
                Rect(left, 0, left + w, frameHeight)
            }
            targetCanvas.drawBitmap(sourceFrame, null, dstRect, framePaint)
        } else {
            // Studio visual backdrop if generating synthetic frame
            val bgGrad = LinearGradient(
                0f, 0f, frameWidth.toFloat(), frameHeight.toFloat(),
                intArrayOf(Color.parseColor("#1D2227"), Color.parseColor("#243438"), Color.parseColor("#343A40")),
                null,
                Shader.TileMode.CLAMP
            )
            framePaint.shader = bgGrad
            targetCanvas.drawRect(0f, 0f, frameWidth.toFloat(), frameHeight.toFloat(), framePaint)
        }
        targetCanvas.restore()

        // 4. Vignette Effect
        if (effects.vignette > 0f) {
            val radius = max(frameWidth, frameHeight) * 0.7f
            val alpha = ((effects.vignette / 100f) * 210).toInt().coerceIn(0, 230)
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    frameWidth / 2f,
                    frameHeight / 2f,
                    radius,
                    intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0)),
                    floatArrayOf(0.45f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            targetCanvas.drawRect(0f, 0f, frameWidth.toFloat(), frameHeight.toFloat(), vignettePaint)
        }

        // 5. Fade In / Fade Out Overlay
        var fadeOverlayAlpha = 0f
        if (effects.fadeInMs > 0L && positionMs < effects.fadeInMs) {
            fadeOverlayAlpha = 1f - (positionMs.toFloat() / effects.fadeInMs.toFloat())
        } else if (effects.fadeOutMs > 0L && positionMs > (durationSafe - effects.fadeOutMs)) {
            val rem = (durationSafe - positionMs).coerceAtLeast(0L)
            fadeOverlayAlpha = 1f - (rem.toFloat() / effects.fadeOutMs.toFloat())
        }
        if (fadeOverlayAlpha > 0.01f) {
            val fadePaint = Paint().apply {
                color = Color.argb((fadeOverlayAlpha * 255).toInt().coerceIn(0, 255), 0, 0, 0)
            }
            targetCanvas.drawRect(0f, 0f, frameWidth.toFloat(), frameHeight.toFloat(), fadePaint)
        }

        // 6. Active Caption Segment Rendering
        val activeSegment = segments.firstOrNull { positionMs in it.startTimeMs..it.endTimeMs } ?: return
        val style = activeSegment.styleOverride ?: defaultStyle
        val animState = CaptionAnimationEngine.computeFrameState(
            params = style.animation,
            currentPositionMs = positionMs,
            segmentStartMs = activeSegment.startTimeMs,
            segmentEndMs = activeSegment.endTimeMs
        )
        if (animState.alpha <= 0.01f) return

        drawCaptionOverlayOnCanvas(
            context = context,
            canvas = targetCanvas,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            positionMs = positionMs,
            captionMode = captionMode,
            segment = activeSegment,
            style = style,
            animState = animState
        )
    }

    private fun drawCaptionOverlayOnCanvas(
        context: Context,
        canvas: Canvas,
        frameWidth: Int,
        frameHeight: Int,
        positionMs: Long,
        captionMode: CaptionDisplayMode,
        segment: CaptionSegment,
        style: CaptionStyle,
        animState: ComputedCaptionFrameState
    ) {
        val scaleFactor = frameHeight / 1080f
        val textSizePx = (style.fontSizeSp * 1.45f * scaleFactor).coerceAtLeast(22f)
        val typeface = resolveTypeface(context, style.fontFamilyName, style.fontWeight)

        val words = if (segment.words.isNotEmpty()) {
            segment.words
        } else {
            val split = segment.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val dur = (segment.endTimeMs - segment.startTimeMs).coerceAtLeast(300L)
            val step = dur / split.size.coerceAtLeast(1)
            split.mapIndexed { idx, w ->
                CaptionWord(
                    text = w,
                    startTimeMs = segment.startTimeMs + idx * step,
                    endTimeMs = segment.startTimeMs + (idx + 1) * step
                )
            }
        }

        val activeWordIndex = words.indexOfFirst { positionMs in it.startTimeMs..it.endTimeMs }
            .let { idx ->
                if (idx >= 0) idx else words.indexOfLast { positionMs >= it.startTimeMs }.coerceAtLeast(0)
            }

        // Determine visible words based on mode (WORD_BY_WORD vs FULL_SENTENCE)
        val displayWords: List<CaptionWord> = when (captionMode) {
            CaptionDisplayMode.WORD_BY_WORD -> {
                // Show a compact 3-word window centered around the active word for natural podcast flow
                val groupIndex = activeWordIndex / 3
                val startIdx = groupIndex * 3
                val endIdx = min(words.size, startIdx + 3)
                words.subList(startIdx, endIdx)
            }
            CaptionDisplayMode.FULL_SENTENCE -> words
        }

        val maxWordsPerLine = style.maxWordsPerLine.coerceIn(1, 10)
        val lines = displayWords.chunked(maxWordsPerLine)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSizePx
            this.letterSpacing = style.letterSpacingEm + animState.extraLetterSpacingEm
        }

        val lineHeight = textSizePx * style.lineSpacingMultiplier * 1.25f
        val totalBlockHeight = lines.size * lineHeight
        val maxLineWidth = lines.maxOfOrNull { lineWords ->
            textPaint.measureText(lineWords.joinToString(" ") { it.text })
        } ?: 200f

        val centerX = frameWidth * (if (style.positionPreset == CaptionPositionPreset.CUSTOM) style.customPositionX else 0.5f)
        val centerY = frameHeight * when (style.positionPreset) {
            CaptionPositionPreset.TOP -> 0.16f
            CaptionPositionPreset.CENTER -> 0.50f
            CaptionPositionPreset.BOTTOM -> 0.80f
            CaptionPositionPreset.CUSTOM -> style.customPositionY
        }

        canvas.save()
        canvas.translate(centerX + animState.translationXPx, centerY + animState.translationYPx)
        canvas.rotate(animState.rotationDegrees)
        canvas.scale(animState.scaleX, animState.scaleY)

        // Draw Background Box if enabled
        if (style.backgroundStyle != CaptionBackgroundStyle.NONE) {
            val padH = style.backgroundPaddingH * 1.6f * scaleFactor
            val padV = style.backgroundPaddingV * 1.6f * scaleFactor
            val boxW = (maxLineWidth + padH * 2f) * animState.backgroundWidthFraction
            val boxH = totalBlockHeight + padV * 2f
            val bgAlpha = ((style.backgroundOpacity * animState.alpha) * 255).toInt().coerceIn(0, 255)
            val baseBgColor = style.backgroundColorArgb.toInt()
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(
                    bgAlpha,
                    Color.red(baseBgColor),
                    Color.green(baseBgColor),
                    Color.blue(baseBgColor)
                )
            }
            val rect = RectF(-boxW / 2f, -boxH / 2f, boxW / 2f, boxH / 2f)
            val corner = if (style.backgroundStyle == CaptionBackgroundStyle.ROUNDED_RECT) {
                style.backgroundCornerRadius * 1.5f * scaleFactor
            } else 0f
            canvas.drawRoundRect(rect, corner, corner, bgPaint)
        }

        // Draw each line with Arabic RTL / LTR awareness and active word highlight
        val startY = -(totalBlockHeight / 2f) + textSizePx * 0.85f
        lines.forEachIndexed { lineIdx, lineWords ->
            val lineY = startY + lineIdx * lineHeight
            val isArabicLine = lineWords.any { w -> w.text.any { ch -> ch in '\u0600'..'\u06FF' } }
            val orderedWords = if (isArabicLine) lineWords.reversed() else lineWords
            val spaceWidth = textPaint.measureText(" ")
            val wordWidths = orderedWords.map { textPaint.measureText(it.text) }
            val totalLineWidth = wordWidths.sum() + spaceWidth * (orderedWords.size - 1).coerceAtLeast(0)

            var cursorX = -totalLineWidth / 2f
            orderedWords.forEachIndexed { wIdx, word ->
                val isSpoken = word.id == words.getOrNull(activeWordIndex)?.id
                val wordW = wordWidths[wIdx]
                val wordCenterX = cursorX + wordW / 2f

                // Active word highlight pill in WORD_BY_WORD or KARAOKE/WORD_HIGHLIGHT
                if (isSpoken) {
                    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        val activeCol = style.activeWordColorArgb.toInt()
                        color = Color.argb(
                            (animState.alpha * 235).toInt().coerceIn(0, 255),
                            Color.red(activeCol),
                            Color.green(activeCol),
                            Color.blue(activeCol)
                        )
                    }
                    val pillRect = RectF(
                        cursorX - 10f * scaleFactor,
                        lineY - textSizePx * 0.88f,
                        cursorX + wordW + 10f * scaleFactor,
                        lineY + textSizePx * 0.24f
                    )
                    canvas.drawRoundRect(pillRect, 12f * scaleFactor, 12f * scaleFactor, pillPaint)
                }

                // Stroke outline
                if (style.strokeThicknessPx > 0.5f) {
                    val strokePaint = Paint(textPaint).apply {
                        this.style = Paint.Style.STROKE
                        this.strokeWidth = style.strokeThicknessPx * scaleFactor
                        this.textAlign = Paint.Align.CENTER
                        val sc = style.strokeColorArgb.toInt()
                        this.color = Color.argb(
                            (animState.alpha * 255).toInt().coerceIn(0, 255),
                            Color.red(sc),
                            Color.green(sc),
                            Color.blue(sc)
                        )
                    }
                    canvas.drawText(word.text, wordCenterX, lineY, strokePaint)
                }

                // Main Fill + Shadow
                val fillPaint = Paint(textPaint).apply {
                    this.style = Paint.Style.FILL
                    this.textAlign = Paint.Align.CENTER
                    val baseColor = if (isSpoken && style.backgroundStyle == CaptionBackgroundStyle.NONE) {
                        style.activeWordColorArgb.toInt()
                    } else {
                        style.textColorArgb.toInt()
                    }
                    this.color = Color.argb(
                        (animState.alpha * 255).toInt().coerceIn(0, 255),
                        Color.red(baseColor),
                        Color.green(baseColor),
                        Color.blue(baseColor)
                    )
                    val blurR = max(style.shadowBlurRadius, animState.glowBlurRadius) * scaleFactor
                    if (blurR > 0.5f) {
                        setShadowLayer(
                            blurR,
                            style.shadowOffsetX * scaleFactor,
                            style.shadowOffsetY * scaleFactor,
                            style.shadowColorArgb.toInt()
                        )
                    }
                }
                canvas.drawText(word.text, wordCenterX, lineY, fillPaint)
                cursorX += wordW + spaceWidth
            }
        }

        canvas.restore()
    }
}
