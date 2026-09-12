package io.github.vexpaer.mybp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.vexpaer.mybp.MyApp
import io.github.vexpaer.mybp.core.settings.SettingsRepository
import io.github.vexpaer.mybp.core.settings.TabNames
import io.github.vexpaer.mybp.core.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 应用级状态：底部导航名称与主题。设置页修改后立即同步到底部导航。
 */
class MainViewModel(private val settings: SettingsRepository) : ViewModel() {

    val tabNames: StateFlow<List<String>> = settings.tabNames
        .map { TabNames.resolve(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TabNames.DEFAULTS)

    val rawTabNames: StateFlow<List<String?>> = settings.tabNames
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(null, null, null, null))

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val onboardingDone: StateFlow<Boolean?> = settings.onboardingDone
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null) // null = 尚未读出，避免首帧误入引导

    fun setTabName(index: Int, value: String) {
        viewModelScope.launch { settings.setTabName(index, value) }
    }

    fun resetTabNames() {
        viewModelScope.launch { settings.resetTabNames() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settings.setOnboardingDone() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MyApp
                MainViewModel(app.container.settingsRepository)
            }
        }
    }
}
