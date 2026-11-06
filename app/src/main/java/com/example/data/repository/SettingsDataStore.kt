package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore by preferencesDataStore(name = "asl_studio_settings")

data class StudioAppSettings(
    val language: AppLanguage = AppLanguage.ARABIC,
    val isDarkTheme: Boolean = true,
    val defaultResolution: ExportResolution = ExportResolution.RES_1080P,
    val defaultFrameRate: Int = 30,
    val defaultAspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_16_9,
    val defaultTemplateId: String = "tpl_podcast_pro",
    val speechProvider: SpeechProviderType = SpeechProviderType.FIREBASE_GEMINI,
    val whisperEndpointUrl: String = "",
    val hasAcceptedAiCloudConsent: Boolean = true
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val DEFAULT_RESOLUTION = stringPreferencesKey("default_resolution")
        val DEFAULT_FPS = intPreferencesKey("default_fps")
        val DEFAULT_RATIO = stringPreferencesKey("default_aspect_ratio")
        val DEFAULT_TEMPLATE = stringPreferencesKey("default_template_id")
        val SPEECH_PROVIDER = stringPreferencesKey("speech_provider")
        val WHISPER_URL = stringPreferencesKey("whisper_endpoint_url")
        val AI_CONSENT = booleanPreferencesKey("ai_cloud_consent")
    }

    val settingsFlow: Flow<StudioAppSettings> = context.studioDataStore.data.map { prefs ->
        val langCode = prefs[Keys.LANGUAGE] ?: AppLanguage.ARABIC.code
        val lang = if (langCode == "en") AppLanguage.ENGLISH else AppLanguage.ARABIC
        val resName = prefs[Keys.DEFAULT_RESOLUTION] ?: ExportResolution.RES_1080P.name
        val res = runCatching { ExportResolution.valueOf(resName) }.getOrDefault(ExportResolution.RES_1080P)
        val ratioName = prefs[Keys.DEFAULT_RATIO] ?: AspectRatioPreset.RATIO_16_9.name
        val ratio = runCatching { AspectRatioPreset.valueOf(ratioName) }.getOrDefault(AspectRatioPreset.RATIO_16_9)
        val providerName = prefs[Keys.SPEECH_PROVIDER] ?: SpeechProviderType.FIREBASE_GEMINI.name
        val provider = runCatching { SpeechProviderType.valueOf(providerName) }
            .getOrDefault(SpeechProviderType.FIREBASE_GEMINI)

        StudioAppSettings(
            language = lang,
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            defaultResolution = res,
            defaultFrameRate = prefs[Keys.DEFAULT_FPS] ?: 30,
            defaultAspectRatio = ratio,
            defaultTemplateId = prefs[Keys.DEFAULT_TEMPLATE] ?: "tpl_podcast_pro",
            speechProvider = provider,
            whisperEndpointUrl = prefs[Keys.WHISPER_URL] ?: "",
            hasAcceptedAiCloudConsent = prefs[Keys.AI_CONSENT] ?: true
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.studioDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.studioDataStore.edit { it[Keys.DARK_THEME] = isDark }
    }

    suspend fun setDefaultResolution(resolution: ExportResolution) {
        context.studioDataStore.edit { it[Keys.DEFAULT_RESOLUTION] = resolution.name }
    }

    suspend fun setDefaultFrameRate(fps: Int) {
        context.studioDataStore.edit { it[Keys.DEFAULT_FPS] = fps }
    }

    suspend fun setDefaultAspectRatio(ratio: AspectRatioPreset) {
        context.studioDataStore.edit { it[Keys.DEFAULT_RATIO] = ratio.name }
    }

    suspend fun setDefaultTemplateId(templateId: String) {
        context.studioDataStore.edit { it[Keys.DEFAULT_TEMPLATE] = templateId }
    }

    suspend fun setSpeechProvider(provider: SpeechProviderType) {
        context.studioDataStore.edit { it[Keys.SPEECH_PROVIDER] = provider.name }
    }

    suspend fun setWhisperEndpointUrl(url: String) {
        context.studioDataStore.edit { it[Keys.WHISPER_URL] = url.trim() }
    }

    suspend fun setAiCloudConsent(accepted: Boolean) {
        context.studioDataStore.edit { it[Keys.AI_CONSENT] = accepted }
    }
}
