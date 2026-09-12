package io.github.vexpaer.mybp.ui.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.vexpaer.mybp.MyApp
import io.github.vexpaer.mybp.core.sleep.SleepNight
import io.github.vexpaer.mybp.data.sleep.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class SleepUiState(
    val usageAccessGranted: Boolean = false,
    val nights: List<SleepNight> = emptyList(),
    val todayEpochDay: Long = 0L,
) {
    val tonight: SleepNight? get() = nights.firstOrNull { it.wakeDayEpochDay == todayEpochDay }
    val nightsWithData: List<SleepNight> get() = nights.filter { it.durationMinutes > 0 }
    val averageDurationMinutes: Int?
        get() = nightsWithData.takeIf { it.isNotEmpty() }
            ?.map { it.durationMinutes }?.average()?.toInt()
}

class SleepViewModel(private val repository: SleepRepository) : ViewModel() {

    private val granted = MutableStateFlow(false)
    private val zone: ZoneId = ZoneId.systemDefault()

    val uiState: StateFlow<SleepUiState> = combine(granted, repository.nights) { ok, nights ->
        SleepUiState(
            usageAccessGranted = ok,
            nights = nights,
            todayEpochDay = LocalDate.now(zone).toEpochDay(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepUiState())

    /** 每次回到「早睡早起」页面时调用：复查权限，有权限则刷新估算。 */
    fun onScreenResumed() {
        viewModelScope.launch {
            granted.value = repository.hasUsageAccess()
            if (granted.value) repository.refreshRecent()
        }
    }

    /** 手动修正某晚（起床日为键），保存后标记「手动修改」。 */
    fun saveManual(wakeDay: LocalDate, bedtimeMillis: Long, wakeMillis: Long) {
        viewModelScope.launch { repository.saveManual(wakeDay, bedtimeMillis, wakeMillis) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyApp
                SleepViewModel(app.container.sleepRepository)
            }
        }
    }
}
