package com.zirakusa.gatewayhub.data

import android.content.Context
import androidx.core.content.edit

enum class ThemeChoice { NIGHT, OCEAN, FOREST, RUBY, LIGHT }

/** SAFE: nothing runs in the background. BACKGROUND: a service with a persistent notification watches the hub. */
enum class McpMode { SAFE, BACKGROUND }

/** The app's own preferences (never secrets; kept after logout). */
data class AppSettings(
    /** "es" or "en"; null until the person chooses on the first launch. */
    val language: String? = null,
    val theme: ThemeChoice = ThemeChoice.NIGHT,
    /** Screenshots are allowed by default; only the person turns this on, inside the app. */
    val blockScreenshots: Boolean = false,
    /** Background by default: the AI must reach Stone without Gateway on screen (copy, paste, done). */
    val mode: McpMode = McpMode.BACKGROUND,
    val activityNotifications: Boolean = true,
    val summaryMinutes: Int = 30,
) {
    companion object {
        val LANGUAGES = listOf("es", "en")
        val SUMMARY_MINUTES = listOf(15, 30, 60, 180)
    }
}

interface SettingsStore {
    fun load(): AppSettings
    fun save(settings: AppSettings)
}

class MemorySettingsStore(var saved: AppSettings = AppSettings()) : SettingsStore {
    override fun load() = saved
    override fun save(settings: AppSettings) { saved = settings }
}

class PrefsSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("hub_settings", Context.MODE_PRIVATE)

    override fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            language = prefs.getString("language", null)?.takeIf { it in AppSettings.LANGUAGES },
            theme = enumOr(prefs.getString("theme", null), d.theme),
            blockScreenshots = prefs.getBoolean("blockScreenshots", d.blockScreenshots),
            mode = enumOr(prefs.getString("mode", null), d.mode),
            activityNotifications = prefs.getBoolean("activityNotifications", d.activityNotifications),
            summaryMinutes = prefs.getInt("summaryMinutes", d.summaryMinutes).takeIf { it in AppSettings.SUMMARY_MINUTES } ?: d.summaryMinutes,
        )
    }

    override fun save(settings: AppSettings) = prefs.edit {
        putString("language", settings.language); putString("theme", settings.theme.name)
        putBoolean("blockScreenshots", settings.blockScreenshots); putString("mode", settings.mode.name)
        putBoolean("activityNotifications", settings.activityNotifications); putInt("summaryMinutes", settings.summaryMinutes)
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}
