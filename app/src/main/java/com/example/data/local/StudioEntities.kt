package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "studio_projects")
data class StudioProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val videoUriString: String,
    val videoFileName: String,
    val videoWidth: Int,
    val videoHeight: Int,
    val durationMs: Long,
    val frameRate: Float,
    val fileSizeBytes: Long,
    val aspectRatioPreset: String,
    val captionMode: String,
    val segmentsJson: String,
    val styleJson: String,
    val effectsJson: String,
    val audioJson: String,
    val timelineSegmentsJson: String,
    val exportConfigJson: String,
    val hasActiveSession: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastModifiedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_templates")
data class CustomTemplateEntity(
    @PrimaryKey val id: String,
    val nameAr: String,
    val nameEn: String,
    val captionMode: String,
    val styleJson: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_exports")
data class RecentExportEntity(
    @PrimaryKey val id: String,
    val projectName: String,
    val outputPath: String,
    val resolutionLabel: String,
    val frameRate: Int,
    val fileSizeBytes: Long,
    val exportedAtMillis: Long = System.currentTimeMillis()
)
