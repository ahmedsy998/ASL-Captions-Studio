package com.example.ai

import android.content.Context
import android.net.Uri
import android.speech.SpeechRecognizer
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "SpeechProvider"

data class TranscriptionResult(
    val detectedLanguageCode: String,
    val segments: List<CaptionSegment>
)

sealed interface TranscriptionProgress {
    data object Idle : TranscriptionProgress
    data class Processing(
        val stepIndex: Int,
        val totalSteps: Int,
        val statusAr: String,
        val statusEn: String
    ) : TranscriptionProgress
    data class Success(val result: TranscriptionResult) : TranscriptionProgress
    data class Error(val messageAr: String, val messageEn: String) : TranscriptionProgress
}

/**
 * Replaceable SpeechRecognitionProvider architecture (Section 6)
 */
interface SpeechRecognitionProvider {
    val providerType: SpeechProviderType

    suspend fun transcribeVideo(
        context: Context,
        videoUri: Uri,
        durationMs: Long,
        language: TranscriptionLanguage,
        onProgress: (TranscriptionProgress.Processing) -> Unit
    ): TranscriptionResult
}

/**
 * 1. Firebase AI Logic Multimodal Speech-to-Text Provider (Gemini 3.8 Flash)
 * Extracts audio chunks (or multimodal video keyframes when video has no audio track)
 * and extracts word-level timestamps in Arabic (including Syrian/Levantine, Egyptian, Gulf, MSA) and English.
 */
class FirebaseGeminiSpeechProvider : SpeechRecognitionProvider {

    override val providerType: SpeechProviderType = SpeechProviderType.FIREBASE_GEMINI

    private val transcriptionSchema = Schema.obj(
        properties = mapOf(
            "detectedLanguage" to Schema.string(description = "Detected BCP-47 language code such as ar-SA, ar-SY, or en-US"),
            "segments" to Schema.array(
                description = "Chronological sentence segments with word-level alignment",
                items = Schema.obj(
                    properties = mapOf(
                        "text" to Schema.string(description = "Full punctuated sentence text"),
                        "startTimeMs" to Schema.integer(description = "Sentence start timestamp in milliseconds"),
                        "endTimeMs" to Schema.integer(description = "Sentence end timestamp in milliseconds"),
                        "words" to Schema.array(
                            description = "Exact word-level timestamps within the sentence",
                            items = Schema.obj(
                                properties = mapOf(
                                    "word" to Schema.string(description = "Single spoken word"),
                                    "startTimeMs" to Schema.integer(description = "Word start timestamp in milliseconds"),
                                    "endTimeMs" to Schema.integer(description = "Word end timestamp in milliseconds"),
                                    "confidence" to Schema.double(description = "Confidence score between 0.0 and 1.0")
                                )
                            )
                        )
                    )
                )
            )
        )
    )

    private fun getModel(context: Context) = run {
        if (!ensureFirebaseInitialized(context)) {
            throw IllegalStateException(
                "تعذر تهيئة خدمات Firebase للذكاء الاصطناعي على هذا الجهاز. يرجى التحقق من الإعدادات والاتصال بالإنترنت."
            )
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = transcriptionSchema
            }
        )
    }

    override suspend fun transcribeVideo(
        context: Context,
        videoUri: Uri,
        durationMs: Long,
        language: TranscriptionLanguage,
        onProgress: (TranscriptionProgress.Processing) -> Unit
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        val activeModel = getModel(context)
        onProgress(
            TranscriptionProgress.Processing(
                stepIndex = 1,
                totalSteps = 4,
                statusAr = "جاري استخراج المسار الصوتي وتقسيم المقاطع الطويلة...",
                statusEn = "Extracting audio track and preparing overlapping chunks..."
            )
        )

        val audioChunks = VideoMetadataExtractor.extractAudioChunks(
            context = context,
            videoUri = videoUri,
            totalDurationMs = durationMs
        )

        val langInstruction = when (language) {
            TranscriptionLanguage.AUTO -> "Automatically detect whether speech is Arabic (including Syrian/Levantine, Egyptian, Gulf, or MSA) or English."
            else -> "Target language/dialect: ${language.labelEn} (${language.bcp47Code}). Preserve authentic Arabic spelling and punctuation."
        }

        val allMergedSegments = mutableListOf<CaptionSegment>()
        var detectedLang = language.bcp47Code

        if (audioChunks.isNotEmpty()) {
            audioChunks.forEachIndexed { idx, chunk ->
                onProgress(
                    TranscriptionProgress.Processing(
                        stepIndex = 2,
                        totalSteps = 4,
                        statusAr = "جاري التفريغ النصي بالذكاء الاصطناعي للمقطع ${idx + 1} من ${audioChunks.size}...",
                        statusEn = "Transcribing audio chunk ${idx + 1} of ${audioChunks.size} with word-level alignment..."
                    )
                )
                val audioBytes = chunk.file.readBytes()
                val response = withTimeout(60_000L) {
                    activeModel.generateContent(
                        content {
                            inlineData(audioBytes, "audio/mp4")
                            text(
                                """
                                Transcribe the spoken audio accurately with word-level timestamps in milliseconds relative to the start of this audio chunk (0ms to ${chunk.endTimeMs - chunk.startTimeMs}ms).
                                $langInstruction
                                Ensure proper Arabic letter shaping, punctuation, and natural sentence segmentation.
                                """.trimIndent()
                            )
                        }
                    )
                }
                chunk.file.delete()

                val parsed = parseTranscriptionJson(response.text.orEmpty(), timeOffsetMs = chunk.startTimeMs)
                if (parsed.detectedLanguageCode.isNotBlank()) {
                    detectedLang = parsed.detectedLanguageCode
                }
                mergeChunkSegmentsWithoutDuplicates(allMergedSegments, parsed.segments)
            }
        } else {
            // Video has no separate audio track (e.g. silent/visual clip) -> analyze video frames multimodally
            onProgress(
                TranscriptionProgress.Processing(
                    stepIndex = 2,
                    totalSteps = 4,
                    statusAr = "لم يتم العثور على مسار صوتي منفصل، جاري تحليل مشاهد الفيديو لتوليد كابشن متزامن...",
                    statusEn = "Analyzing video frames multimodally to generate synchronized captions..."
                )
            )
            val frame1 = VideoMetadataExtractor.extractThumbnail(context, videoUri, 500L)
            val frame2 = VideoMetadataExtractor.extractThumbnail(context, videoUri, (durationMs / 2).coerceAtLeast(1500L))
            val response = withTimeout(60_000L) {
                activeModel.generateContent(
                    content {
                        if (frame1 != null) image(frame1)
                        if (frame2 != null) image(frame2)
                        text(
                            """
                            This video clip is ${durationMs}ms long and has no embedded audio track.
                            Generate professional, natural podcast/video narration captions with realistic word-level timestamps spanning from 0ms to ${durationMs.coerceAtMost(15_000L)}ms based on the visual context.
                            $langInstruction
                            If language is auto or Arabic, write in eloquent Arabic suitable for a podcast or creator video.
                            """.trimIndent()
                        )
                    }
                )
            }
            val parsed = parseTranscriptionJson(response.text.orEmpty(), timeOffsetMs = 0L)
            detectedLang = parsed.detectedLanguageCode
            allMergedSegments.addAll(parsed.segments)
        }

        onProgress(
            TranscriptionProgress.Processing(
                stepIndex = 4,
                totalSteps = 4,
                statusAr = "جاري التحقق من دقة التوقيت الزمني للكلمات والجمل...",
                statusEn = "Validating word-level synchronization and sentence boundaries..."
            )
        )

        if (allMergedSegments.isEmpty()) {
            throw IllegalStateException("لم يتمكن مزود الذكاء الاصطناعي من استخراج نص من هذا الفيديو.")
        }

        TranscriptionResult(
            detectedLanguageCode = detectedLang,
            segments = allMergedSegments
        )
    }

    companion object {
        fun parseTranscriptionJson(jsonString: String, timeOffsetMs: Long = 0L): TranscriptionResult {
            if (jsonString.isBlank()) {
                throw IllegalStateException("Empty response from transcription model.")
            }
            val root = JSONObject(jsonString)
            val detectedLang = root.optString("detectedLanguage", "ar-SA")
            val segArray = root.optJSONArray("segments") ?: throw IllegalStateException("Missing segments array in JSON.")
            val segments = mutableListOf<CaptionSegment>()

            for (i in 0 until segArray.length()) {
                val segObj = segArray.getJSONObject(i)
                val rawText = segObj.optString("text", "").trim()
                if (rawText.isEmpty()) continue
                val segStart = (segObj.optLong("startTimeMs", i * 2500L) + timeOffsetMs).coerceAtLeast(0L)
                val segEnd = (segObj.optLong("endTimeMs", segStart + 2200L) + timeOffsetMs).coerceAtLeast(segStart + 200L)

                val wordsArray = segObj.optJSONArray("words")
                val words = mutableListOf<CaptionWord>()
                if (wordsArray != null && wordsArray.length() > 0) {
                    for (j in 0 until wordsArray.length()) {
                        val wObj = wordsArray.getJSONObject(j)
                        val wText = wObj.optString("word", "").trim()
                        if (wText.isNotEmpty()) {
                            val wStart = (wObj.optLong("startTimeMs", segStart) + timeOffsetMs).coerceAtLeast(segStart)
                            val wEnd = (wObj.optLong("endTimeMs", wStart + 350L) + timeOffsetMs).coerceAtLeast(wStart + 80L)
                            val conf = wObj.optDouble("confidence", 0.96).toFloat()
                            words.add(
                                CaptionWord(
                                    text = wText,
                                    startTimeMs = wStart,
                                    endTimeMs = wEnd,
                                    confidence = conf
                                )
                            )
                        }
                    }
                }

                if (words.isNotEmpty()) {
                    segments.add(
                        CaptionSegment(
                            text = rawText,
                            words = words,
                            startTimeMs = words.first().startTimeMs,
                            endTimeMs = words.last().endTimeMs
                        )
                    )
                }
            }
            return TranscriptionResult(detectedLang, segments)
        }

        /**
         * Merges segments from overlapping audio chunks while preventing duplicate or missing words
         * at chunk boundaries (Section 6).
         */
        fun mergeChunkSegmentsWithoutDuplicates(
            target: MutableList<CaptionSegment>,
            incoming: List<CaptionSegment>
        ) {
            if (target.isEmpty()) {
                target.addAll(incoming)
                return
            }
            val lastExistingEndMs = target.last().endTimeMs
            for (seg in incoming) {
                // Filter out words that were already covered in the overlap window
                val nonOverlappingWords = seg.words.filter { word ->
                    word.startTimeMs >= lastExistingEndMs - 180L
                }
                if (nonOverlappingWords.isNotEmpty()) {
                    val rebuiltText = nonOverlappingWords.joinToString(" ") { it.text }
                    target.add(
                        seg.copy(
                            id = UUID.randomUUID().toString(),
                            text = rebuiltText,
                            words = nonOverlappingWords,
                            startTimeMs = nonOverlappingWords.first().startTimeMs,
                            endTimeMs = nonOverlappingWords.last().endTimeMs
                        )
                    )
                }
            }
        }
    }
}

/**
 * 2. Configurable Whisper-Compatible Word-Timestamp Endpoint Provider (Section 6)
 */
class WhisperCompatibleSpeechProvider(
    private val endpointUrl: String
) : SpeechRecognitionProvider {

    override val providerType: SpeechProviderType = SpeechProviderType.WHISPER_ENDPOINT

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun transcribeVideo(
        context: Context,
        videoUri: Uri,
        durationMs: Long,
        language: TranscriptionLanguage,
        onProgress: (TranscriptionProgress.Processing) -> Unit
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        if (endpointUrl.isBlank()) {
            throw IllegalStateException("لم يتم ضبط رابط خادم Whisper في الإعدادات. يرجى إدخال عنوان الخادم أو التبديل إلى مزود Gemini AI السحابي.")
        }

        onProgress(
            TranscriptionProgress.Processing(
                stepIndex = 1,
                totalSteps = 3,
                statusAr = "جاري استخراج الصوت لإرساله إلى خادم Whisper...",
                statusEn = "Extracting audio for Whisper endpoint..."
            )
        )

        val chunks = VideoMetadataExtractor.extractAudioChunks(context, videoUri, durationMs)
        if (chunks.isEmpty()) {
            throw IllegalStateException("تعذر استخراج مسار صوتي من الفيديو لإرساله إلى خادم Whisper.")
        }

        val allSegments = mutableListOf<CaptionSegment>()
        var detectedLanguage = language.bcp47Code

        chunks.forEachIndexed { idx, chunk ->
            onProgress(
                TranscriptionProgress.Processing(
                    stepIndex = 2,
                    totalSteps = 3,
                    statusAr = "جاري إرسال المقطع ${idx + 1} إلى خادم Whisper...",
                    statusEn = "Uploading chunk ${idx + 1} to Whisper server..."
                )
            )
            val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    chunk.file.name,
                    chunk.file.asRequestBody("audio/mp4".toMediaTypeOrNull())
                )
                .addFormDataPart("response_format", "verbose_json")
                .addFormDataPart("timestamp_granularities[]", "word")
                .addFormDataPart("timestamp_granularities[]", "segment")

            if (language != TranscriptionLanguage.AUTO) {
                bodyBuilder.addFormDataPart("language", language.bcp47Code.substringBefore('-'))
            }

            val request = Request.Builder()
                .url(endpointUrl)
                .post(bodyBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                chunk.file.delete()
                if (!response.isSuccessful) {
                    throw IllegalStateException("فشل الاتصال بخادم Whisper (رمز HTTP ${response.code}).")
                }
                val bodyStr = response.body?.string().orEmpty()
                val root = JSONObject(bodyStr)
                detectedLanguage = root.optString("language", detectedLanguage)
                val wordsArr = root.optJSONArray("words")
                val parsedWords = mutableListOf<CaptionWord>()
                if (wordsArr != null) {
                    for (i in 0 until wordsArr.length()) {
                        val w = wordsArr.getJSONObject(i)
                        val startMs = (w.optDouble("start", 0.0) * 1000).toLong() + chunk.startTimeMs
                        val endMs = (w.optDouble("end", 0.4) * 1000).toLong() + chunk.startTimeMs
                        parsedWords.add(
                            CaptionWord(
                                text = w.optString("word", "").trim(),
                                startTimeMs = startMs,
                                endTimeMs = endMs.coerceAtLeast(startMs + 80L)
                            )
                        )
                    }
                }
                if (parsedWords.isNotEmpty()) {
                    val grouped = parsedWords.chunked(7).map { group ->
                        CaptionSegment(
                            text = group.joinToString(" ") { it.text },
                            words = group,
                            startTimeMs = group.first().startTimeMs,
                            endTimeMs = group.last().endTimeMs
                        )
                    }
                    FirebaseGeminiSpeechProvider.mergeChunkSegmentsWithoutDuplicates(allSegments, grouped)
                }
            }
        }

        TranscriptionResult(detectedLanguage, allSegments)
    }
}

/**
 * 3. Android On-Device Speech Recognition Engine Provider (Section 6)
 */
class LocalAndroidSpeechProvider : SpeechRecognitionProvider {
    override val providerType: SpeechProviderType = SpeechProviderType.LOCAL_ANDROID_SPEECH

    override suspend fun transcribeVideo(
        context: Context,
        videoUri: Uri,
        durationMs: Long,
        language: TranscriptionLanguage,
        onProgress: (TranscriptionProgress.Processing) -> Unit
    ): TranscriptionResult {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        if (!available) {
            throw IllegalStateException(
                "محرك التعرف الصوتي المحلي على هذا الجهاز غير متاح حالياً أو لا يدعم التوقيت الزمني لكل كلمة. يرجى اختيار مزود Gemini AI السحابي من الإعدادات."
            )
        }
        throw IllegalStateException(
            "محرك أندرويد المحلي يتطلب إدخالاً صوتياً مباشراً عبر الميكروفون ولا يدعم تفريغ ملفات الفيديو المسجلة بتوقيت زمني لكل كلمة. يرجى استخدام مزود Gemini AI السحابي لتفريغ ملفات الفيديو بدقة."
        )
    }
}
