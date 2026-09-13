package io.github.vexpaer.mybp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.vexpaer.mybp.BuildConfig
import io.github.vexpaer.mybp.MyApp
import io.github.vexpaer.mybp.core.export.ExportBuilder
import io.github.vexpaer.mybp.core.settings.SettingsRepository
import io.github.vexpaer.mybp.core.settings.TabNames
import io.github.vexpaer.mybp.core.settings.ThemeMode
import io.github.vexpaer.mybp.data.move.ExerciseRepository
import io.github.vexpaer.mybp.data.sleep.SleepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/** 导出状态：null = 空闲；用户取消系统保存对话框不产生任何状态。 */
sealed interface ExportState {
    data object Working : ExportState
    data object Success : ExportState
    data object Failed : ExportState
}

/**
 * 应用级状态：底部导航名称、主题、数据导出。设置页修改后立即同步到底部导航。
 */
class MainViewModel(
    private val settings: SettingsRepository,
    private val sleepRepository: SleepRepository,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

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

    private val _exportState = MutableStateFlow<ExportState?>(null)
    val exportState: StateFlow<ExportState?> = _exportState.asStateFlow()

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

    /**
     * 构建导出 ZIP。文件写入由 UI 层用 Storage Access Framework 完成，
     * 这里只负责把本地数据组装成字节（在 IO 线程）。
     */
    suspend fun buildExport(): ByteArray = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        ExportBuilder.buildZip(
            sleep = sleepRepository.allNights(),
            exercise = exerciseRepository.allRowsOnce(),
            settings = ExportBuilder.SettingsSnapshot(
                tabNames = TabNames.resolve(settings.tabNames.first()),
                theme = themeMode.value.name,
                appVersion = BuildConfig.VERSION_NAME,
                exportedAt = Instant.now(),
                zone = zone,
            ),
        )
    }

    fun onExportSuccess() {
        _exportState.value = ExportState.Success
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            if (_exportState.value == ExportState.Success) _exportState.value = null
        }
    }

    fun onExportFailed() {
        _exportState.value = ExportState.Failed
        viewModelScope.launch {
            kotlinx.coroutines.delay(4000)
            if (_exportState.value == ExportState.Failed) _exportState.value = null
        }
    }

    fun onExportCancelled() {
        _exportState.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyApp
                MainViewModel(
                    settings = app.container.settingsRepository,
                    sleepRepository = app.container.sleepRepository,
                    exerciseRepository = app.container.exerciseRepository,
                )
            }
        }
    }
}
