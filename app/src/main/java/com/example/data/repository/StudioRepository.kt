package com.example.data.repository

import com.example.data.local.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class StudioProject(
    val id: String,
    val title: String,
    val videoMetadata: VideoMetadata,
    val aspectRatioPreset: AspectRatioPreset,
    val captionMode: CaptionDisplayMode,
    val segments: List<CaptionSegment>,
    val captionStyle: CaptionStyle,
    val videoEffects: VideoEffectsConfig,
    val audioConfig: AudioTrackConfig,
    val timelineSegments: List<TimelineVideoSegment>,
    val exportConfig: ExportConfig,
    val hasActiveSession: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastModifiedMillis: Long = System.currentTimeMillis()
)

class StudioRepository(private val dao: StudioDao) {

    val allProjectsFlow: Flow<List<StudioProject>> = dao.observeAllProjects().map { list ->
        list.map { it.toDomain() }
    }

    val customTemplatesFlow: Flow<List<CaptionTemplate>> = dao.observeCustomTemplates().map { list ->
        list.map { entity ->
            CaptionTemplate(
                id = entity.id,
                nameAr = entity.nameAr,
                nameEn = entity.nameEn,
                category = TemplateCategory.CUSTOM,
                captionMode = runCatching { CaptionDisplayMode.valueOf(entity.captionMode) }
                    .getOrDefault(CaptionDisplayMode.WORD_BY_WORD),
                style = ProjectJsonSerializer.deserializeStyle(entity.styleJson),
                isCustom = true
            )
        }
    }

    val recentExportsFlow: Flow<List<RecentExportItem>> = dao.observeRecentExports().map { list ->
        list.map { entity ->
            RecentExportItem(
                id = entity.id,
                projectName = entity.projectName,
                outputPath = entity.outputPath,
                resolutionLabel = entity.resolutionLabel,
                frameRate = entity.frameRate,
                exportedAtMillis = entity.exportedAtMillis,
                fileSizeBytes = entity.fileSizeBytes
            )
        }
    }

    suspend fun getProjectById(id: String): StudioProject? {
        return dao.getProjectById(id)?.toDomain()
    }

    suspend fun getInterruptedSessionProject(): StudioProject? {
        return dao.getInterruptedSessionProject()?.toDomain()
    }

    suspend fun getProjectCount(): Int = dao.getProjectCount()

    suspend fun saveProject(project: StudioProject, markSessionActive: Boolean = false) {
        if (markSessionActive) {
            dao.clearAllActiveSessions()
        }
        val entity = project.copy(
            hasActiveSession = markSessionActive,
            lastModifiedMillis = System.currentTimeMillis()
        ).toEntity()
        dao.upsertProject(entity)
    }

    suspend fun renameProject(projectId: String, newTitle: String) {
        val current = dao.getProjectById(projectId) ?: return
        dao.upsertProject(
            current.copy(
                title = newTitle.trim(),
                lastModifiedMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun duplicateProject(projectId: String): StudioProject? {
        val current = dao.getProjectById(projectId)?.toDomain() ?: return null
        val copy = current.copy(
            id = UUID.randomUUID().toString(),
            title = "${current.title} (نسخة)",
            hasActiveSession = false,
            createdAtMillis = System.currentTimeMillis(),
            lastModifiedMillis = System.currentTimeMillis()
        )
        dao.upsertProject(copy.toEntity())
        return copy
    }

    suspend fun deleteProject(projectId: String) {
        dao.deleteProjectById(projectId)
    }

    suspend fun clearActiveSessions() {
        dao.clearAllActiveSessions()
    }

    suspend fun saveCustomTemplate(
        nameAr: String,
        nameEn: String,
        captionMode: CaptionDisplayMode,
        style: CaptionStyle,
        existingId: String? = null
    ) {
        val entity = CustomTemplateEntity(
            id = existingId ?: "custom_${UUID.randomUUID()}",
            nameAr = nameAr.ifBlank { "قالب مخصص" },
            nameEn = nameEn.ifBlank { "Custom Template" },
            captionMode = captionMode.name,
            styleJson = ProjectJsonSerializer.serializeStyle(style)
        )
        dao.upsertCustomTemplate(entity)
    }

    suspend fun deleteCustomTemplate(templateId: String) {
        dao.deleteCustomTemplateById(templateId)
    }

    suspend fun recordRecentExport(item: RecentExportItem) {
        dao.insertRecentExport(
            RecentExportEntity(
                id = item.id,
                projectName = item.projectName,
                outputPath = item.outputPath,
                resolutionLabel = item.resolutionLabel,
                frameRate = item.frameRate,
                fileSizeBytes = item.fileSizeBytes,
                exportedAtMillis = item.exportedAtMillis
            )
        )
    }

    private fun StudioProjectEntity.toDomain(): StudioProject {
        val segments = ProjectJsonSerializer.deserializeSegments(segmentsJson)
        val style = ProjectJsonSerializer.deserializeStyle(styleJson)
        val effects = ProjectJsonSerializer.deserializeEffects(effectsJson)
        val audio = ProjectJsonSerializer.deserializeAudioConfig(audioJson)
        val timelineClips = ProjectJsonSerializer.deserializeTimelineSegments(timelineSegmentsJson)
        val exportCfg = ProjectJsonSerializer.deserializeExportConfig(exportConfigJson)

        return StudioProject(
            id = id,
            title = title,
            videoMetadata = VideoMetadata(
                uriString = videoUriString,
                fileName = videoFileName,
                width = videoWidth,
                height = videoHeight,
                durationMs = durationMs,
                frameRate = frameRate,
                fileSizeBytes = fileSizeBytes
            ),
            aspectRatioPreset = runCatching { AspectRatioPreset.valueOf(aspectRatioPreset) }
                .getOrDefault(AspectRatioPreset.RATIO_16_9),
            captionMode = runCatching { CaptionDisplayMode.valueOf(captionMode) }
                .getOrDefault(CaptionDisplayMode.WORD_BY_WORD),
            segments = segments,
            captionStyle = style,
            videoEffects = effects,
            audioConfig = audio,
            timelineSegments = if (timelineClips.isEmpty()) {
                listOf(
                    TimelineVideoSegment(
                        sourceStartMs = 0L,
                        sourceEndMs = durationMs.coerceAtLeast(5000L),
                        timelineStartMs = 0L,
                        label = videoFileName.ifBlank { "Main Video" }
                    )
                )
            } else timelineClips,
            exportConfig = exportCfg,
            hasActiveSession = hasActiveSession,
            createdAtMillis = createdAtMillis,
            lastModifiedMillis = lastModifiedMillis
        )
    }

    private fun StudioProject.toEntity(): StudioProjectEntity {
        return StudioProjectEntity(
            id = id,
            title = title,
            videoUriString = videoMetadata.uriString,
            videoFileName = videoMetadata.fileName,
            videoWidth = videoMetadata.width,
            videoHeight = videoMetadata.height,
            durationMs = videoMetadata.durationMs,
            frameRate = videoMetadata.frameRate,
            fileSizeBytes = videoMetadata.fileSizeBytes,
            aspectRatioPreset = aspectRatioPreset.name,
            captionMode = captionMode.name,
            segmentsJson = ProjectJsonSerializer.serializeSegments(segments),
            styleJson = ProjectJsonSerializer.serializeStyle(captionStyle),
            effectsJson = ProjectJsonSerializer.serializeEffects(videoEffects),
            audioJson = ProjectJsonSerializer.serializeAudioConfig(audioConfig),
            timelineSegmentsJson = ProjectJsonSerializer.serializeTimelineSegments(timelineSegments),
            exportConfigJson = ProjectJsonSerializer.serializeExportConfig(exportConfig),
            hasActiveSession = hasActiveSession,
            createdAtMillis = createdAtMillis,
            lastModifiedMillis = lastModifiedMillis
        )
    }
}
