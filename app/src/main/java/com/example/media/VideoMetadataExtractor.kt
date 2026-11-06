package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

data class AudioChunkFile(
    val file: File,
    val chunkIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long
)

object VideoMetadataExtractor {

    private val supportedMimeTypes = setOf(
        "video/mp4",
        "video/quicktime", // MOV
        "video/webm",
        "video/x-matroska",
        "video/3gpp"
    )

    private val supportedExtensions = setOf("mp4", "mov", "webm", "m4v")

    suspend fun extractMetadata(context: Context, uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        var fileName = "video.mp4"
        var fileSize = 0L
        var mimeType = context.contentResolver.getType(uri) ?: "video/mp4"

        try {
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                fileName = f.name
                fileSize = f.length()
            } else {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                        if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {}

        val ext = fileName.substringAfterLast('.', "mp4").lowercase()
        val isFormatSupported = supportedMimeTypes.contains(mimeType.lowercase()) || supportedExtensions.contains(ext)
        if (!isFormatSupported) {
            return@withContext VideoMetadata(
                uriString = uri.toString(),
                fileName = fileName,
                fileSizeBytes = fileSize,
                mimeType = mimeType,
                isSupported = false,
                errorMessage = "صيغة الفيديو غير مدعومة ($ext). يرجى اختيار ملف MP4 أو MOV أو WebM."
            )
        }

        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10_000L
            val fps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 30f
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

            val finalW = if (rotation == 90 || rotation == 270) height else width
            val finalH = if (rotation == 90 || rotation == 270) width else height

            VideoMetadata(
                uriString = uri.toString(),
                fileName = fileName,
                width = finalW,
                height = finalH,
                durationMs = durationMs.coerceAtLeast(1000L),
                frameRate = fps.coerceIn(15f, 120f),
                fileSizeBytes = fileSize,
                mimeType = mimeType,
                isSupported = true
            )
        } catch (e: Exception) {
            VideoMetadata(
                uriString = uri.toString(),
                fileName = fileName,
                fileSizeBytes = fileSize,
                mimeType = mimeType,
                isSupported = false,
                errorMessage = "تعذر قراءة بيانات الفيديو: ${e.localizedMessage ?: "ملف غير صالح"}"
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun extractThumbnail(context: Context, uri: Uri, timeMs: Long = 1000L): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Extracts audio track from video in overlapping chunks (Section 6) to support long videos
     * and keep inline payloads well under 20MB without OOM.
     */
    suspend fun extractAudioChunks(
        context: Context,
        videoUri: Uri,
        totalDurationMs: Long,
        chunkDurationMs: Long = 25_000L,
        overlapMs: Long = 1_500L
    ): List<AudioChunkFile> = withContext(Dispatchers.IO) {
        val chunks = mutableListOf<AudioChunkFile>()
        val cacheDir = File(context.cacheDir, "audio_chunks").apply { mkdirs() }

        var currentStartMs = 0L
        var index = 0
        val effectiveDuration = totalDurationMs.coerceAtLeast(3000L)

        while (currentStartMs < effectiveDuration && index < 20) {
            val currentEndMs = (currentStartMs + chunkDurationMs).coerceAtMost(effectiveDuration)
            val outFile = File(cacheDir, "chunk_${System.currentTimeMillis()}_$index.m4a")
            val success = muxAudioRange(context, videoUri, outFile, currentStartMs * 1000L, currentEndMs * 1000L)
            if (success && outFile.exists() && outFile.length() > 0) {
                chunks.add(
                    AudioChunkFile(
                        file = outFile,
                        chunkIndex = index,
                        startTimeMs = currentStartMs,
                        endTimeMs = currentEndMs
                    )
                )
            }
            if (currentEndMs >= effectiveDuration) break
            currentStartMs = (currentEndMs - overlapMs).coerceAtLeast(currentStartMs + 1000L)
            index++
        }
        chunks
    }

    private fun muxAudioRange(
        context: Context,
        videoUri: Uri,
        outputFile: File,
        startTimeUs: Long,
        endTimeUs: Long
    ): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            if (videoUri.scheme == "file") {
                extractor.setDataSource(videoUri.path ?: return false)
            } else {
                extractor.setDataSource(context, videoUri, null)
            }
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) return false

            extractor.selectTrack(audioTrackIndex)
            extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val dstTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(256 * 1024)
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endTimeUs) break
                if (sampleTimeUs >= startTimeUs) {
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = (sampleTimeUs - startTimeUs).coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                }
                extractor.advance()
            }
            muxer.stop()
            true
        } catch (_: Exception) {
            false
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }
    }
}
