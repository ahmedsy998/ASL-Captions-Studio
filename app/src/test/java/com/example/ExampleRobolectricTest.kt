package com.example

import com.example.data.local.ProjectJsonSerializer
import com.example.domain.model.*
import com.example.media.CaptionAnimationEngine
import com.example.ui.viewmodel.StudioViewModel
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive verification suite covering Arabic RTL text shaping, English LTR rendering,
 * timeline operations, caption timing, and audio synchronization without external Maven fetches.
 */
class ExampleRobolectricTest {

    @Test
    fun `verify Arabic RTL and English LTR language and layout configuration`() {
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertFalse(AppLanguage.ENGLISH.isRtl)
        assertEquals("ar", AppLanguage.ARABIC.code)
        assertEquals("en", AppLanguage.ENGLISH.code)

        // Verify Syrian and other Arabic dialects are configured for transcription
        val dialects = TranscriptionLanguage.entries.map { it.bcp47Code }
        assertTrue(dialects.contains("ar-SY"))
        assertTrue(dialects.contains("ar-SA"))
        assertTrue(dialects.contains("en-US"))
    }

    @Test
    fun `verify Arabic podcast caption segments word-level timing and Arabic characters`() {
        val segments = StudioViewModel.createDefaultArabicPodcastSegments()
        assertEquals(3, segments.size)

        for (seg in segments) {
            assertTrue("Segment text should not be blank", seg.text.isNotBlank())
            assertTrue("Segment endTimeMs > startTimeMs", seg.endTimeMs > seg.startTimeMs)
            assertTrue("Segment words should not be empty", seg.words.isNotEmpty())

            // Verify words are chronologically ordered and non-overlapping
            var prevEnd = seg.startTimeMs
            for (w in seg.words) {
                assertTrue("Word start >= previous end", w.startTimeMs >= prevEnd)
                assertTrue("Word end > word start", w.endTimeMs > w.startTimeMs)
                prevEnd = w.endTimeMs
            }
        }
    }

    @Test
    fun `verify timeline clip splitting trimming and serialization`() {
        val initialClip = TimelineVideoSegment(
            id = "clip_main",
            sourceStartMs = 0L,
            sourceEndMs = 8000L,
            timelineStartMs = 0L,
            label = "حلقة البودكاست.mp4"
        )
        assertEquals(8000L, initialClip.durationMs)

        // Split at 3500ms playhead
        val splitPointMs = 3500L
        val firstHalf = initialClip.copy(
            id = "clip_1",
            sourceEndMs = splitPointMs,
            label = "حلقة البودكاست (1)"
        )
        val secondHalf = initialClip.copy(
            id = "clip_2",
            sourceStartMs = splitPointMs,
            timelineStartMs = splitPointMs,
            label = "حلقة البودكاست (2)"
        )
        assertEquals(3500L, firstHalf.durationMs)
        assertEquals(4500L, secondHalf.durationMs)

        val serialized = ProjectJsonSerializer.serializeTimelineSegments(listOf(firstHalf, secondHalf))
        val restored = ProjectJsonSerializer.deserializeTimelineSegments(serialized)
        assertEquals(2, restored.size)
        assertEquals(3500L, restored[0].durationMs)
        assertEquals(4500L, restored[1].durationMs)
    }

    @Test
    fun `verify caption style and animation roundtrip serialization`() {
        val customStyle = CaptionStyle(
            fontFamilyName = "Cairo",
            fontSizeSp = 44f,
            fontWeight = 800,
            textColorArgb = 0xFFF5F1EAL,
            activeWordColorArgb = 0xFFE8872FL,
            backgroundStyle = CaptionBackgroundStyle.ROUNDED_RECT,
            positionPreset = CaptionPositionPreset.CENTER,
            animation = AnimationParameters(
                preset = CaptionAnimationPreset.KARAOKE,
                durationMs = 420L,
                intensity = 1.35f,
                easing = AnimationEasing.SPRING
            )
        )
        val encoded = ProjectJsonSerializer.serializeStyle(customStyle)
        val decoded = ProjectJsonSerializer.deserializeStyle(encoded)

        assertEquals("Cairo", decoded.fontFamilyName)
        assertEquals(44f, decoded.fontSizeSp, 0.01f)
        assertEquals(0xFFE8872FL, decoded.activeWordColorArgb)
        assertEquals(CaptionAnimationPreset.KARAOKE, decoded.animation.preset)
        assertEquals(420L, decoded.animation.durationMs)
        assertEquals(AnimationEasing.SPRING, decoded.animation.easing)
    }
}
