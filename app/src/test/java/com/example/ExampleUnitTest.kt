package com.example

import com.example.ai.FirebaseGeminiSpeechProvider
import com.example.data.local.ProjectJsonSerializer
import com.example.domain.model.*
import com.example.media.CaptionAnimationEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test 25 animation presets compute valid frame states and timing`() {
        assertEquals(25, CaptionAnimationPreset.entries.size)
        for (preset in CaptionAnimationPreset.entries) {
            val params = AnimationParameters(
                preset = preset,
                durationMs = 400L,
                intensity = 1.2f
            )
            // Before segment start -> alpha 0
            val beforeState = CaptionAnimationEngine.computeFrameState(params, 100L, 500L, 2500L)
            assertEquals(0f, beforeState.alpha, 0.01f)

            // Mid-animation -> active visible state
            val midState = CaptionAnimationEngine.computeFrameState(params, 700L, 500L, 2500L)
            assertTrue("Preset $preset alpha should be > 0", midState.alpha > 0f)
            assertTrue("Preset $preset scaleX should be > 0", midState.scaleX > 0f)
        }
    }

    @Test
    fun `test 20 built-in caption templates across all 5 categories`() {
        val templates = CaptionTemplateCatalog.builtInTemplates
        assertEquals(20, templates.size)
        assertEquals(4, templates.count { it.category == TemplateCategory.PODCAST })
        assertEquals(4, templates.count { it.category == TemplateCategory.SOCIAL_MEDIA })
        assertEquals(4, templates.count { it.category == TemplateCategory.CINEMATIC })
        assertEquals(4, templates.count { it.category == TemplateCategory.DYNAMIC })
        assertEquals(4, templates.count { it.category == TemplateCategory.ARABIC })
    }

    @Test
    fun `test word-level synchronization and overlapping chunk deduplication`() {
        val chunk1 = listOf(
            CaptionSegment(
                text = "في هذه الحلقة سنتحدث",
                startTimeMs = 0L,
                endTimeMs = 1650L,
                words = listOf(
                    CaptionWord(text = "في", startTimeMs = 0L, endTimeMs = 320L),
                    CaptionWord(text = "هذه", startTimeMs = 320L, endTimeMs = 700L),
                    CaptionWord(text = "الحلقة", startTimeMs = 700L, endTimeMs = 1180L),
                    CaptionWord(text = "سنتحدث", startTimeMs = 1180L, endTimeMs = 1650L)
                )
            )
        )
        // Chunk 2 starts at 1100ms (overlapping 'سنتحدث' at 1180..1650ms) and adds 'عن أهمية صناعة المحتوى'
        val chunk2 = listOf(
            CaptionSegment(
                text = "سنتحدث عن أهمية صناعة المحتوى",
                startTimeMs = 1180L,
                endTimeMs = 2800L,
                words = listOf(
                    CaptionWord(text = "سنتحدث", startTimeMs = 1180L, endTimeMs = 1650L),
                    CaptionWord(text = "عن", startTimeMs = 1650L, endTimeMs = 1900L),
                    CaptionWord(text = "أهمية", startTimeMs = 1900L, endTimeMs = 2320L),
                    CaptionWord(text = "صناعة", startTimeMs = 2320L, endTimeMs = 2580L),
                    CaptionWord(text = "المحتوى", startTimeMs = 2580L, endTimeMs = 2800L)
                )
            )
        )

        val merged = mutableListOf<CaptionSegment>()
        FirebaseGeminiSpeechProvider.mergeChunkSegmentsWithoutDuplicates(merged, chunk1)
        FirebaseGeminiSpeechProvider.mergeChunkSegmentsWithoutDuplicates(merged, chunk2)

        assertEquals(2, merged.size)
        // Verify 'سنتحدث' was not duplicated in the second merged segment
        assertEquals("عن أهمية صناعة المحتوى", merged[1].text)
        assertEquals(1650L, merged[1].startTimeMs)
        assertEquals(2800L, merged[1].endTimeMs)
    }

    @Test
    fun `test project persistence serializer roundtrip for Arabic and English captions effects and audio`() {
        val originalSegments = listOf(
            CaptionSegment(
                id = "seg_test_1",
                text = "مرحباً بكم في استوديو ASL Captions",
                startTimeMs = 100L,
                endTimeMs = 2100L,
                words = listOf(
                    CaptionWord(id = "w1", text = "مرحباً", startTimeMs = 100L, endTimeMs = 500L, isHighlighted = true),
                    CaptionWord(id = "w2", text = "بكم", startTimeMs = 500L, endTimeMs = 900L),
                    CaptionWord(id = "w3", text = "في", startTimeMs = 900L, endTimeMs = 1200L),
                    CaptionWord(id = "w4", text = "ASL", startTimeMs = 1200L, endTimeMs = 1600L),
                    CaptionWord(id = "w5", text = "Captions", startTimeMs = 1600L, endTimeMs = 2100L)
                )
            )
        )
        val json = ProjectJsonSerializer.serializeSegments(originalSegments)
        val restored = ProjectJsonSerializer.deserializeSegments(json)

        assertEquals(1, restored.size)
        assertEquals("مرحباً بكم في استوديو ASL Captions", restored[0].text)
        assertEquals(5, restored[0].words.size)
        assertTrue(restored[0].words[0].isHighlighted)
        assertEquals(1200L, restored[0].words[3].startTimeMs)

        // Test VideoEffectsConfig roundtrip
        val effects = VideoEffectsConfig(
            brightness = 15f,
            contrast = 20f,
            colorGradingPreset = ColorGradingPreset.CINEMATIC,
            flipHorizontal = true
        )
        val restoredEffects = ProjectJsonSerializer.deserializeEffects(ProjectJsonSerializer.serializeEffects(effects))
        assertEquals(15f, restoredEffects.brightness, 0.01f)
        assertEquals(20f, restoredEffects.contrast, 0.01f)
        assertEquals(ColorGradingPreset.CINEMATIC, restoredEffects.colorGradingPreset)
        assertTrue(restoredEffects.flipHorizontal)

        // Test Audio & ExportConfig roundtrip
        val audio = AudioTrackConfig(
            originalVolume = 0.85f,
            isOriginalMuted = false,
            additionalClips = listOf(
                AudioClip(name = "موسيقى هادئة.mp3", uriString = "content://audio/1", volume = 0.6f, fadeInMs = 800L)
            )
        )
        val restoredAudio = ProjectJsonSerializer.deserializeAudioConfig(ProjectJsonSerializer.serializeAudioConfig(audio))
        assertEquals(0.85f, restoredAudio.originalVolume, 0.01f)
        assertEquals(1, restoredAudio.additionalClips.size)
        assertEquals("موسيقى هادئة.mp3", restoredAudio.additionalClips[0].name)

        val exportCfg = ExportConfig(
            resolution = ExportResolution.RES_1080P,
            frameRate = 60,
            bitrateMbps = 16,
            outputFileName = "حلقة_البودكاست"
        )
        val restoredExport = ProjectJsonSerializer.deserializeExportConfig(ProjectJsonSerializer.serializeExportConfig(exportCfg))
        assertEquals(ExportResolution.RES_1080P, restoredExport.resolution)
        assertEquals(60, restoredExport.frameRate)
        assertEquals("حلقة_البودكاست", restoredExport.outputFileName)
    }
}
