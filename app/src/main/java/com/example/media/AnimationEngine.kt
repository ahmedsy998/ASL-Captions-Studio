package com.example.media

import com.example.domain.model.AnimationDirection
import com.example.domain.model.AnimationEasing
import com.example.domain.model.AnimationParameters
import com.example.domain.model.CaptionAnimationPreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Computed frame-level visual transformation shared identically between
 * Jetpack Compose Live Preview and Android Canvas MP4 Export Renderer (Sections 9 & 13).
 */
data class ComputedCaptionFrameState(
    val alpha: Float = 1f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val translationXPx: Float = 0f,
    val translationYPx: Float = 0f,
    val rotationDegrees: Float = 0f,
    val extraBlurRadius: Float = 0f,
    val extraLetterSpacingEm: Float = 0f,
    val glowBlurRadius: Float = 0f,
    val characterRevealFraction: Float = 1f,
    val backgroundWidthFraction: Float = 1f,
    val activeWordScale: Float = 1.0f,
    val waveVerticalOffsetFactor: Float = 0f
)

object CaptionAnimationEngine {

    fun computeFrameState(
        params: AnimationParameters,
        currentPositionMs: Long,
        segmentStartMs: Long,
        segmentEndMs: Long
    ): ComputedCaptionFrameState {
        val effectiveStart = segmentStartMs + params.startOffsetMs + params.delayMs
        val effectiveEnd = (segmentEndMs - params.endOffsetMs).coerceAtLeast(effectiveStart + 50L)

        if (currentPositionMs < effectiveStart) {
            return ComputedCaptionFrameState(alpha = 0f, scaleX = 0.8f, scaleY = 0.8f)
        }

        val elapsedMs = (currentPositionMs - effectiveStart).coerceAtLeast(0L)
        val remainingMs = (effectiveEnd - currentPositionMs).coerceAtLeast(0L)
        val animDuration = params.durationMs.coerceAtLeast(80L).toFloat()
        val intensity = params.intensity.coerceIn(0.2f, 2.5f)

        val rawProgress = (elapsedMs.toFloat() / animDuration).coerceIn(0f, 1f)
        val directedProgress = when (params.direction) {
            AnimationDirection.NORMAL -> rawProgress
            AnimationDirection.REVERSE -> 1f - rawProgress
            AnimationDirection.ALTERNATE -> {
                val cycle = (elapsedMs / params.durationMs.coerceAtLeast(80L)).toInt()
                if (cycle % 2 == 0) rawProgress else 1f - rawProgress
            }
        }
        val t = applyEasing(directedProgress, params.easing)

        return when (params.preset) {
            CaptionAnimationPreset.POP_UP -> {
                val scale = if (t < 0.7f) {
                    0.3f + (t / 0.7f) * (0.7f + 0.18f * intensity)
                } else {
                    (1f + 0.18f * intensity) - ((t - 0.7f) / 0.3f) * (0.18f * intensity)
                }
                ComputedCaptionFrameState(
                    alpha = (t * 2f).coerceIn(0f, 1f),
                    scaleX = scale,
                    scaleY = scale,
                    activeWordScale = 1.08f
                )
            }

            CaptionAnimationPreset.BOUNCE -> {
                val bounceOffset = -absSin(t * PI.toFloat() * 2f) * (1f - t) * 42f * intensity
                val wordBounce = 1f + 0.12f * intensity * sin((elapsedMs % 400L) / 400f * PI.toFloat())
                ComputedCaptionFrameState(
                    alpha = (t * 2.5f).coerceIn(0f, 1f),
                    translationYPx = bounceOffset,
                    scaleX = wordBounce,
                    scaleY = wordBounce,
                    activeWordScale = 1.15f
                )
            }

            CaptionAnimationPreset.FADE_IN -> {
                ComputedCaptionFrameState(alpha = t)
            }

            CaptionAnimationPreset.FADE_OUT -> {
                val outProgress = if (remainingMs < animDuration) {
                    (remainingMs.toFloat() / animDuration).coerceIn(0f, 1f)
                } else 1f
                ComputedCaptionFrameState(alpha = outProgress)
            }

            CaptionAnimationPreset.SLIDE_UP -> {
                ComputedCaptionFrameState(
                    alpha = t,
                    translationYPx = (1f - t) * 56f * intensity
                )
            }

            CaptionAnimationPreset.SLIDE_DOWN -> {
                ComputedCaptionFrameState(
                    alpha = t,
                    translationYPx = -(1f - t) * 56f * intensity
                )
            }

            CaptionAnimationPreset.SLIDE_LEFT -> {
                ComputedCaptionFrameState(
                    alpha = t,
                    translationXPx = (1f - t) * 80f * intensity
                )
            }

            CaptionAnimationPreset.SLIDE_RIGHT -> {
                ComputedCaptionFrameState(
                    alpha = t,
                    translationXPx = -(1f - t) * 80f * intensity
                )
            }

            CaptionAnimationPreset.ZOOM_IN -> {
                val s = 0.4f + 0.6f * t
                ComputedCaptionFrameState(alpha = t, scaleX = s, scaleY = s)
            }

            CaptionAnimationPreset.ZOOM_OUT -> {
                val s = (1f + 0.6f * intensity) - (0.6f * intensity * t)
                ComputedCaptionFrameState(alpha = t, scaleX = s, scaleY = s)
            }

            CaptionAnimationPreset.TYPEWRITER -> {
                val totalSegDuration = (effectiveEnd - effectiveStart).coerceAtLeast(200L).toFloat()
                val reveal = ((elapsedMs.toFloat() / (totalSegDuration * 0.75f))).coerceIn(0.05f, 1f)
                ComputedCaptionFrameState(
                    alpha = 1f,
                    characterRevealFraction = reveal
                )
            }

            CaptionAnimationPreset.WORD_HIGHLIGHT -> {
                ComputedCaptionFrameState(
                    alpha = 1f,
                    activeWordScale = 1f + 0.14f * intensity
                )
            }

            CaptionAnimationPreset.KARAOKE -> {
                val totalSegDuration = (effectiveEnd - effectiveStart).coerceAtLeast(200L).toFloat()
                val sweep = (elapsedMs.toFloat() / totalSegDuration).coerceIn(0f, 1f)
                ComputedCaptionFrameState(
                    alpha = 1f,
                    backgroundWidthFraction = sweep,
                    activeWordScale = 1.08f
                )
            }

            CaptionAnimationPreset.NEON_GLOW -> {
                val pulse = 0.5f + 0.5f * sin(elapsedMs.toFloat() / 180f)
                ComputedCaptionFrameState(
                    alpha = (0.7f + 0.3f * t).coerceIn(0f, 1f),
                    glowBlurRadius = (12f + 16f * pulse) * intensity,
                    activeWordScale = 1.1f
                )
            }

            CaptionAnimationPreset.SCALE_PULSE -> {
                val pulse = 1f + 0.08f * intensity * sin(elapsedMs.toFloat() / 140f)
                ComputedCaptionFrameState(
                    alpha = 1f,
                    scaleX = pulse,
                    scaleY = pulse,
                    activeWordScale = 1.12f
                )
            }

            CaptionAnimationPreset.BLUR_IN -> {
                ComputedCaptionFrameState(
                    alpha = t,
                    extraBlurRadius = (1f - t) * 18f * intensity
                )
            }

            CaptionAnimationPreset.ROTATE_IN -> {
                ComputedCaptionFrameState(
                    alpha = t,
                    rotationDegrees = (1f - t) * -18f * intensity,
                    scaleX = 0.7f + 0.3f * t,
                    scaleY = 0.7f + 0.3f * t
                )
            }

            CaptionAnimationPreset.ELASTIC -> {
                val elasticScale = if (t == 0f || t == 1f) t else {
                    1f + exp(-6f * t) * cos(t * PI.toFloat() * 3.5f) * 0.35f * intensity
                }
                ComputedCaptionFrameState(
                    alpha = (t * 2.5f).coerceIn(0f, 1f),
                    scaleX = elasticScale,
                    scaleY = elasticScale
                )
            }

            CaptionAnimationPreset.TRACKING_REVEAL -> {
                ComputedCaptionFrameState(
                    alpha = t,
                    extraLetterSpacingEm = (1f - t) * 0.25f * intensity
                )
            }

            CaptionAnimationPreset.ANIMATED_BACKGROUND -> {
                ComputedCaptionFrameState(
                    alpha = 1f,
                    backgroundWidthFraction = t.coerceIn(0.15f, 1f),
                    activeWordScale = 1.08f
                )
            }

            CaptionAnimationPreset.WAVE -> {
                val wave = sin(elapsedMs.toFloat() / 160f) * 10f * intensity
                ComputedCaptionFrameState(
                    alpha = 1f,
                    translationYPx = wave,
                    waveVerticalOffsetFactor = sin(elapsedMs.toFloat() / 150f) * intensity
                )
            }

            CaptionAnimationPreset.FLIP -> {
                val flipScaleY = cos((1f - t) * (PI.toFloat() / 2f)).coerceIn(0.05f, 1f)
                ComputedCaptionFrameState(
                    alpha = t,
                    scaleY = flipScaleY
                )
            }

            CaptionAnimationPreset.SPRING -> {
                val damped = 1f - exp(-5f * t) * cos(t * PI.toFloat() * 2.5f) * 0.4f * intensity
                ComputedCaptionFrameState(
                    alpha = (t * 2f).coerceIn(0f, 1f),
                    scaleX = damped,
                    scaleY = damped,
                    translationYPx = (1f - damped) * 28f
                )
            }

            CaptionAnimationPreset.STRETCH -> {
                val stretchX = 1f + (1f - t) * 0.45f * intensity
                val stretchY = 1f - (1f - t) * 0.25f * intensity
                ComputedCaptionFrameState(
                    alpha = t,
                    scaleX = stretchX.coerceAtLeast(0.5f),
                    scaleY = stretchY.coerceAtLeast(0.5f)
                )
            }

            CaptionAnimationPreset.SMOOTH_REVEAL -> {
                ComputedCaptionFrameState(
                    alpha = t,
                    translationYPx = (1f - t) * 20f * intensity,
                    scaleX = 0.94f + 0.06f * t,
                    scaleY = 0.94f + 0.06f * t
                )
            }
        }
    }

    private fun applyEasing(t: Float, easing: AnimationEasing): Float {
        val x = t.coerceIn(0f, 1f)
        return when (easing) {
            AnimationEasing.LINEAR -> x
            AnimationEasing.EASE_IN_OUT -> x * x * (3f - 2f * x)
            AnimationEasing.OVERSHOOT -> {
                val s = 1.70158f
                val p = x - 1f
                (p * p * ((s + 1f) * p + s) + 1f).coerceIn(0f, 1.25f)
            }
            AnimationEasing.SPRING -> {
                (1f - exp(-6f * x) * cos(x * PI.toFloat() * 2f)).coerceIn(0f, 1.2f)
            }
        }
    }

    private fun absSin(v: Float): Float = kotlin.math.abs(sin(v))
}
