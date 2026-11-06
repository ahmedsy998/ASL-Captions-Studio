package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudioDao {

    @Query("SELECT * FROM studio_projects ORDER BY lastModifiedMillis DESC")
    fun observeAllProjects(): Flow<List<StudioProjectEntity>>

    @Query("SELECT * FROM studio_projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: String): StudioProjectEntity?

    @Query("SELECT * FROM studio_projects WHERE hasActiveSession = 1 ORDER BY lastModifiedMillis DESC LIMIT 1")
    suspend fun getInterruptedSessionProject(): StudioProjectEntity?

    @Query("SELECT COUNT(*) FROM studio_projects")
    suspend fun getProjectCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: StudioProjectEntity)

    @Update
    suspend fun updateProject(project: StudioProjectEntity)

    @Query("UPDATE studio_projects SET hasActiveSession = 0")
    suspend fun clearAllActiveSessions()

    @Query("DELETE FROM studio_projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    // Custom Caption Templates
    @Query("SELECT * FROM custom_templates ORDER BY createdAtMillis DESC")
    fun observeCustomTemplates(): Flow<List<CustomTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomTemplate(template: CustomTemplateEntity)

    @Query("DELETE FROM custom_templates WHERE id = :templateId")
    suspend fun deleteCustomTemplateById(templateId: String)

    // Recent Exports
    @Query("SELECT * FROM recent_exports ORDER BY exportedAtMillis DESC")
    fun observeRecentExports(): Flow<List<RecentExportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentExport(exportEntity: RecentExportEntity)

    @Query("DELETE FROM recent_exports")
    suspend fun clearRecentExports()
}
