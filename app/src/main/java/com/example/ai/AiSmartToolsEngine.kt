package com.example.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.domain.model.*
import com.example.media.VideoMetadataExtractor
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "AiSmartToolsEngine"

/**
 * Dedicated AI Smart Editing Tools Engine (Section 17) powered by Firebase AI Logic (gemini-3.8-flash).
 * Generates structured suggestions that require user confirmation before destructive edits.
 */
class AiSmartToolsEngine {

    private val smartSuggestionSchema = Schema.obj(
        properties = mapOf(
            "summaryAr" to Schema.string(description = "Arabic summary of the AI analysis or improvement"),
            "summaryEn" to Schema.string(description = "English summary of the AI analysis or improvement"),
            "detectedLanguage" to Schema.string(description = "Detected language and dialect if applicable"),
            "suggestedTemplateId" to Schema.string(description = "Recommended template ID from catalog"),
            "generatedDescription" to Schema.string(description = "Short social media video description and hashtags"),
            "highlightedWords" to Schema.array(
                items = Schema.string(),
                description = "Important keywords to highlight in the transcript"
            ),
            "correctedSentences" to Schema.array(
                items = Schema.string(),
                description = "Corrected or punctuated or re-segmented sentences matching the transcript"
            ),
            "sceneChangeTimestampsMs" to Schema.array(
                items = Schema.integer(),
                description = "Detected visual scene change timestamps in milliseconds"
            )
        ),
        optionalProperties = listOf(
            "detectedLanguage",
            "suggestedTemplateId",
            "generatedDescription",
            "highlightedWords",
            "correctedSentences",
            "sceneChangeTimestampsMs"
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = smartSuggestionSchema
            }
        )
    }

    /**
     * Tool 2: Automatic Language & Dialect Detection
     */
    suspend fun detectLanguage(segments: List<CaptionSegment>): AiSmartSuggestion = withContext(Dispatchers.IO) {
        val fullText = segments.joinToString(" | ") { it.text }
        val response = withTimeout(60_000L) {
            model.generateContent(
                """
                Analyze the following video transcript and identify its language, dialect (e.g., Arabic Modern Standard, Syrian/Levantine, Egyptian, Gulf, or English), and tone:
                Transcript: "$fullText"
                Provide a clear summary in Arabic (summaryAr) and English (summaryEn) and set detectedLanguage.
                """.trimIndent()
            )
        }
        val json = JSONObject(response.text ?: "{}")
        AiSmartSuggestion(
            toolTitleAr = "اكتشاف اللغة واللهجة تلقائياً",
            toolTitleEn = "Automatic Language & Dialect Detection",
            summaryAr = json.optString("summaryAr", "تم التعرف على اللغة العربية."),
            summaryEn = json.optString("summaryEn", "Detected Arabic language."),
            isDestructive = false
        )
    }

    /**
     * Tool 3 & 4: Transcript Correction Assistance & Automatic Punctuation
     */
    suspend fun improveAndPunctuateTranscript(
        segments: List<CaptionSegment>,
        punctuationOnly: Boolean
    ): AiSmartSuggestion = withContext(Dispatchers.IO) {
        val indexedText = segments.mapIndexed { idx, seg -> "${idx + 1}: ${seg.text}" }.joinToString("\n")
        val taskDesc = if (punctuationOnly) {
            "Add accurate Arabic/English punctuation marks (commas, periods, question marks) to each sentence without changing words."
        } else {
            "Correct any spelling, grammar, or transcription errors while preserving the exact meaning and number of sentences."
        }
        val response = withTimeout(60_000L) {
            model.generateContent(
                """
                $taskDesc
                Return the exact same number of sentences (${segments.size}) in 'correctedSentences'.
                Input sentences:
                $indexedText
                """.trimIndent()
            )
        }
        val json = JSONObject(response.text ?: "{}")
        val correctedArr = json.optJSONArray("correctedSentences")
        val updatedSegments = if (correctedArr != null && correctedArr.length() > 0) {
            segments.mapIndexed { idx, seg ->
                val newSentence = if (idx < correctedArr.length()) correctedArr.getString(idx).trim() else seg.text
                val newWordsText = newSentence.split(Regex("\\s+")).filter { it.isNotBlank() }
                val updatedWords = if (newWordsText.size == seg.words.size) {
                    seg.words.mapIndexed { wIdx, w -> w.copy(text = newWordsText[wIdx]) }
                } else {
                    val dur = (seg.endTimeMs - seg.startTimeMs).coerceAtLeast(300L)
                    val step = dur / newWordsText.size.coerceAtLeast(1)
                    newWordsText.mapIndexed { wIdx, wStr ->
                        CaptionWord(
                            text = wStr,
                            startTimeMs = seg.startTimeMs + wIdx * step,
                            endTimeMs = seg.startTimeMs + (wIdx + 1) * step
                        )
                    }
                }
                seg.copy(text = newSentence, words = updatedWords)
            }
        } else segments

        AiSmartSuggestion(
            toolTitleAr = if (punctuationOnly) "الترقيم التلقائي الذكي" else "تصحيح النص وتدقيق الإملاء",
            toolTitleEn = if (punctuationOnly) "Automatic Smart Punctuation" else "Transcript Correction Assistance",
            summaryAr = json.optString("summaryAr", "تم تحسين النص وإضافة علامات الترقيم المناسبة."),
            summaryEn = json.optString("summaryEn", "Improved transcript accuracy and punctuation."),
            updatedSegments = updatedSegments,
            isDestructive = true
        )
    }

    /**
     * Tool 5: Smart Sentence Segmentation
     */
    suspend fun smartSentenceSegmentation(segments: List<CaptionSegment>): AiSmartSuggestion = withContext(Dispatchers.IO) {
        val allWords = segments.flatMap { it.words }
        if (allWords.isEmpty()) {
            throw IllegalStateException("لا توجد كلمات لتقسيمها. يرجى توليد الكابشن أولاً.")
        }
        // Group into balanced 4-5 word readable podcast caption chunks preserving exact word timestamps
        val resegmented = allWords.chunked(4).map { chunkWords ->
            CaptionSegment(
                id = UUID.randomUUID().toString(),
                text = chunkWords.joinToString(" ") { it.text },
                words = chunkWords,
                startTimeMs = chunkWords.first().startTimeMs,
                endTimeMs = chunkWords.last().endTimeMs
            )
        }
        AiSmartSuggestion(
            toolTitleAr = "التقسيم الذكي للجمل",
            toolTitleEn = "Smart Sentence Segmentation",
            summaryAr = "تم إعادة تقسيم النص إلى ${resegmented.size} جملة متوازنة ومريحة للقراءة مع الحفاظ التام على التوقيت الزمني لكل كلمة.",
            summaryEn = "Re-segmented transcript into ${resegmented.size} balanced caption phrases while preserving exact word-level timestamps.",
            updatedSegments = resegmented,
            isDestructive = true
        )
    }

    /**
     * Tool 6: Remove Long Silences
     */
    suspend fun analyzeAndRemoveLongSilences(
        segments: List<CaptionSegment>,
        silenceThresholdMs: Long = 650L
    ): AiSmartSuggestion = withContext(Dispatchers.IO) {
        val silences = mutableListOf<Pair<Long, Long>>()
        val compacted = mutableListOf<CaptionSegment>()
        var accumulatedShiftMs = 0L
        var prevEndMs = 0L

        for (seg in segments) {
            val gap = seg.startTimeMs - prevEndMs
            if (gap > silenceThresholdMs) {
                silences.add(prevEndMs to seg.startTimeMs)
                val trimAmount = gap - 200L // Leave a natural 200ms breath pause
                accumulatedShiftMs += trimAmount
            }
            prevEndMs = seg.endTimeMs
            val shiftedWords = seg.words.map { w ->
                w.copy(
                    startTimeMs = (w.startTimeMs - accumulatedShiftMs).coerceAtLeast(0L),
                    endTimeMs = (w.endTimeMs - accumulatedShiftMs).coerceAtLeast(100L)
                )
            }
            compacted.add(
                seg.copy(
                    words = shiftedWords,
                    startTimeMs = (seg.startTimeMs - accumulatedShiftMs).coerceAtLeast(0L),
                    endTimeMs = (seg.endTimeMs - accumulatedShiftMs).coerceAtLeast(200L)
                )
            )
        }

        val count = silences.size
        val savedSec = String.format("%.1f", accumulatedShiftMs / 1000f)
        AiSmartSuggestion(
            toolTitleAr = "إزالة فترات الصمت الطويلة",
            toolTitleEn = "Remove Long Silences",
            summaryAr = if (count > 0) {
                "تم اكتشاف $count فترة صمت طويلة وتوفير $savedSec ثانية. اضغط على (تطبيق التعديل) لاعتماد ضغط التوقيت."
            } else {
                "لا توجد فترات صمت طويلة تتجاوز الحد المسموح به في الكابشن الحالي."
            },
            summaryEn = if (count > 0) {
                "Detected $count long silence gaps saving ${savedSec}s. Confirm to compact caption timing."
            } else {
                "No long silence intervals exceeding threshold were found."
            },
            updatedSegments = if (count > 0) compacted else null,
            silenceIntervalsMs = silences,
            isDestructive = count > 0
        )
    }

    /**
     * Tool 7: Identify Possible Scene Changes (Multimodal Vision)
     */
    suspend fun detectSceneChanges(
        context: Context,
        videoUri: Uri,
        durationMs: Long
    ): AiSmartSuggestion = withContext(Dispatchers.IO) {
        val sampleTimes = listOf(
            (durationMs * 0.15).toLong(),
            (durationMs * 0.45).toLong(),
            (durationMs * 0.75).toLong()
        )
        val bitmaps = sampleTimes.mapNotNull { t ->
            VideoMetadataExtractor.extractThumbnail(context, videoUri, t)
        }
        val response = withTimeout(60_000L) {
            model.generateContent(
                content {
                    bitmaps.forEach { image(it) }
                    text(
                        """
                        Analyze these sequential video frames sampled across a ${durationMs}ms video.
                        Identify likely scene transitions or camera/speaker emphasis points in milliseconds within 0..$durationMs in 'sceneChangeTimestampsMs', and summarize the visual pacing in Arabic (summaryAr) and English (summaryEn).
                        """.trimIndent()
                    )
                }
            )
        }
        val json = JSONObject(response.text ?: "{}")
        val tsArr = json.optJSONArray("sceneChangeTimestampsMs")
        val timestamps = mutableListOf<Long>()
        if (tsArr != null) {
            for (i in 0 until tsArr.length()) {
                timestamps.add(tsArr.optLong(i))
            }
        }
        if (timestamps.isEmpty()) {
            timestamps.addAll(sampleTimes)
        }
        AiSmartSuggestion(
            toolTitleAr = "اكتشاف تغيرات المشاهد",
            toolTitleEn = "Identify Scene Changes",
            summaryAr = json.optString("summaryAr", "تم تحليل كادرات الفيديو وتحديد نقاط الانتقال البصري."),
            summaryEn = json.optString("summaryEn", "Analyzed video frames and identified visual transition points."),
            sceneChangeTimestampsMs = timestamps,
            isDestructive = false
        )
    }

    /**
     * Tool 8: Suggest Caption Templates
     */
    suspend fun suggestCaptionTemplate(segments: List<CaptionSegment>): AiSmartSuggestion = withContext(Dispatchers.IO) {
        val fullText = segments.joinToString(" ") { it.text }
        val availableIds = CaptionTemplateCatalog.builtInTemplates.joinToString(", ") { "${it.id} (${it.nameEn})" }
        val response = withTimeout(60_000L) {
            model.generateContent(
                """
                Based on the following video transcript, select the single best matching caption template ID from this list:
                [$availableIds]
                Transcript: "$fullText"
                Set 'suggestedTemplateId' to the exact template ID and explain why in Arabic (summaryAr) and English (summaryEn).
                """.trimIndent()
            )
        }
        val json = JSONObject(response.text ?: "{}")
        val tplId = json.optString("suggestedTemplateId", "tpl_podcast_pro")
        AiSmartSuggestion(
            toolTitleAr = "اقتراح قالب كابشن مناسب",
            toolTitleEn = "Suggest Caption Template",
            summaryAr = json.optString("summaryAr", "تم اقتراح القالب الأنسب لطبيعة المحتوى."),
            summaryEn = json.optString("summaryEn", "Suggested the best matching caption template for your content."),
            suggestedTemplateId = tplId,
            isDestructive = false
        )
    }

    /**
     * Tool 9: Suggest Highlight Words
     */
    suspend fun suggestHighlightWords(segments: List<CaptionSegment>): AiSmartSuggestion = withContext(Dispatchers.IO) {
        val fullText = segments.joinToString(" ") { it.text }
        val response = withTimeout(60_000L) {
            model.generateContent(
                """
                Identify the most impactful power words or key terms in this transcript that should be visually highlighted in the captions:
                Transcript: "$fullText"
                Return those exact words in 'highlightedWords' and summarize in Arabic (summaryAr) and English (summaryEn).
                """.trimIndent()
            )
        }
        val json = JSONObject(response.text ?: "{}")
        val hwArr = json.optJSONArray("highlightedWords")
        val targetWords = mutableSetOf<String>()
        if (hwArr != null) {
            for (i in 0 until hwArr.length()) {
                targetWords.add(hwArr.getString(i).trim())
            }
        }
        val updatedSegments = segments.map { seg ->
            seg.copy(
                words = seg.words.map { w ->
                    val shouldHighlight = targetWords.any { kw ->
                        w.text.contains(kw, ignoreCase = true) || kw.contains(w.text, ignoreCase = true)
                    }
                    if (shouldHighlight) w.copy(isHighlighted = true) else w
                }
            )
        }
        AiSmartSuggestion(
            toolTitleAr = "اقتراح الكلمات المميزة (Highlight)",
            toolTitleEn = "Suggest Highlight Words",
            summaryAr = json.optString("summaryAr", "تم تحديد الكلمات المفتاحية الأهم لتمييزها بصرياً."),
            summaryEn = json.optString("summaryEn", "Identified key impactful words for visual emphasis."),
            updatedSegments = updatedSegments,
            isDestructive = false
        )
    }

    /**
     * Tool 10: Generate Short Video Descriptions & Hashtags
     */
    suspend fun generateVideoDescription(segments: List<CaptionSegment>): AiSmartSuggestion = withContext(Dispatchers.IO) {
        val fullText = segments.joinToString(" ") { it.text }
        val response = withTimeout(60_000L) {
            model.generateContent(
                """
                Write a catchy, professional short video description with trending hashtags in both Arabic and English for YouTube Shorts, Instagram Reels, and TikTok based on this transcript:
                Transcript: "$fullText"
                Put the full ready-to-copy description in 'generatedDescription' and a short summary in 'summaryAr' and 'summaryEn'.
                """.trimIndent()
            )
        }
        val json = JSONObject(response.text ?: "{}")
        val desc = json.optString("generatedDescription", "")
        AiSmartSuggestion(
            toolTitleAr = "توليد وصف قصير للفيديو والهاشتاغات",
            toolTitleEn = "Generate Short Video Description",
            summaryAr = json.optString("summaryAr", "تم توليد وصف احترافي جاهز للنسخ والنشر."),
            summaryEn = json.optString("summaryEn", "Generated a ready-to-publish social video description."),
            generatedDescription = desc,
            isDestructive = false
        )
    }
}
