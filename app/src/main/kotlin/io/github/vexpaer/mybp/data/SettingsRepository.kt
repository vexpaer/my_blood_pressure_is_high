package io.github.vexpaer.mybp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 主题：跟随系统 / 浅色 / 深色。 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 全部设置项，默认仅存本地 DataStore。
 * 注意：这里存的是用户原始输入，展示时统一经 TabNames.sanitize 收敛。
 */
class SettingsRepository(private val context: Context) {

    private val store get() = context.settingsDataStore

    private val tabKeys = listOf(
        stringPreferencesKey("tab_name_0"),
        stringPreferencesKey("tab_name_1"),
        stringPreferencesKey("tab_name_2"),
        stringPreferencesKey("tab_name_3"),
    )
    private val themeKey = stringPreferencesKey("theme_mode")
    private val onboardingKey = booleanPreferencesKey("onboarding_done")

    val tabNames: Flow<List<String?>> = store.data.map { p -> tabKeys.map { p[it] } }

    val themeMode: Flow<ThemeMode> = store.data.map { p ->
        p[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val onboardingDone: Flow<Boolean> = store.data.map { it[onboardingKey] ?: false }

    suspend fun setTabName(index: Int, value: String) {
        require(index in tabKeys.indices)
        store.edit { p ->
            val cleaned = value.trim()
            if (cleaned.isEmpty()) p.remove(tabKeys[index]) else p[tabKeys[index]] = cleaned
        }
    }

    suspend fun resetTabNames() {
        store.edit { p -> tabKeys.forEach { p.remove(it) } }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.name }
    }

    suspend fun setOnboardingDone() {
        store.edit { it[onboardingKey] = true }
    }
}
