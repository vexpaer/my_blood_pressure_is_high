package io.github.vexpaer.mybp.ui.move

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.vexpaer.mybp.MyApp
import io.github.vexpaer.mybp.core.move.ExerciseInput
import io.github.vexpaer.mybp.core.move.ExerciseMode
import io.github.vexpaer.mybp.core.move.ExerciseValues
import io.github.vexpaer.mybp.core.move.StreakCalculator
import io.github.vexpaer.mybp.data.db.ExerciseDefEntity
import io.github.vexpaer.mybp.data.db.RecordRow
import io.github.vexpaer.mybp.data.move.ExerciseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MoveUiState(
    val defs: List<ExerciseDefEntity> = emptyList(),
    val todayRecords: List<RecordRow> = emptyList(),
    val weekDays: List<Pair<Long, Int>> = emptyList(),
    val streak: Int = 0,
    val last7Count: Int = 0,
    val todayEpochDay: Long = 0L,
)

/**
 * 抬腿跑跑：今天动了没。极快记录路径：选运动 → 输数字 → 保存。
 */
class MoveViewModel(private val repository: ExerciseRepository) : ViewModel() {

    val uiState: StateFlow<MoveUiState> = combine(repository.defs, repository.records) { defs, records ->
        val today = LocalDate.now().toEpochDay()
        val days = records.mapTo(HashSet()) { it.dayEpochDay }
        MoveUiState(
            defs = defs,
            todayRecords = records.filter { it.dayEpochDay == today }.sortedByDescending { it.createdAt },
            weekDays = (6 downTo 0).map { i ->
                val day = today - i
                day to records.count { it.dayEpochDay == day }
            },
            streak = StreakCalculator.currentStreak(days, today),
            last7Count = StreakCalculator.last7DaysCount(days, today),
            todayEpochDay = today,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoveUiState())

    fun saveNewExerciseAndRecord(name: String, mode: ExerciseMode, values: ExerciseValues, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.createDef(name, mode) }
                .onSuccess { defId -> repository.addRecord(defId, LocalDate.now().toEpochDay(), values) }
                .onFailure { onError("名字得有一个，最短一个字。") }
        }
    }

    fun saveRecord(defId: Long, values: ExerciseValues) {
        viewModelScope.launch { repository.addRecord(defId, LocalDate.now().toEpochDay(), values) }
    }

    fun updateRecord(recordId: Long, values: ExerciseValues) {
        viewModelScope.launch {
            repository.getRecord(recordId)?.let { repository.updateRecord(it, values) }
        }
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch { repository.deleteRecord(recordId) }
    }

    fun archiveExercise(defId: Long) {
        viewModelScope.launch { repository.archiveDef(defId) }
    }

    companion object {
        fun parse(mode: ExerciseMode, texts: Map<String, String>) =
            ExerciseInput.parse(
                mode = mode,
                setsText = texts["sets"],
                repsText = texts["reps"],
                secondsText = texts["seconds"],
                metersText = texts["meters"],
                kilogramsText = texts["kilograms"],
            )

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MyApp
                MoveViewModel(app.container.exerciseRepository)
            }
        }
    }
}
