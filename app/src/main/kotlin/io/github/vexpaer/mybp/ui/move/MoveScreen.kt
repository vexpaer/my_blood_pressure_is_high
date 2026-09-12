package io.github.vexpaer.mybp.ui.move

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vexpaer.mybp.core.move.ExerciseMode
import io.github.vexpaer.mybp.core.move.ExerciseValues
import io.github.vexpaer.mybp.data.db.RecordRow
import io.github.vexpaer.mybp.ui.components.SectionLabel
import io.github.vexpaer.mybp.ui.theme.AppTheme
import io.github.vexpaer.mybp.ui.util.TimeFormats

/**
 * 抬腿跑跑：今天动了没。
 * 目标交互：打开 → 记一笔 → 输数字 → 保存，全程 ≤3 步。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveScreen(viewModel: MoveViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecordRow?>(null) }
    var preselectDefId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            "抬腿跑跑",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 20.dp),
        )
        Spacer(Modifier.height(16.dp))

        // Hero：今天 + 连续天数
        Surface(
            color = AppTheme.extended.moveContainer,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "今天",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.todayRecords.isEmpty()) {
                        Text(
                            "还没动",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            "从两个深蹲开始也行。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "${state.todayRecords.size} 项运动",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            state.todayRecords.firstOrNull()?.let {
                                "${it.defName} · ${ExerciseValues(
                                    sets = it.sets, reps = it.reps, seconds = it.seconds,
                                    meters = it.meters, kilograms = it.kilograms,
                                ).summary(ExerciseMode.valueOf(it.defMode))}"
                            } ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${state.streak}",
                        style = MaterialTheme.typography.displayMedium,
                        color = AppTheme.extended.move,
                    )
                    Text(
                        "连续天数",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { adding = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("记一笔")
        }

        SectionLabel("最近 7 天")
        WeekBars(weekDays = state.weekDays, todayEpochDay = state.todayEpochDay)
        if (state.last7Count > 0) {
            Text(
                "这周动了 ${state.last7Count} 天，保持住。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SectionLabel("今天")
        if (state.todayRecords.isEmpty()) {
            Text(
                "记下的都会留在这里。",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.extended.inkFaint,
            )
        }
        state.todayRecords.forEachIndexed { index, record ->
            TodayRecordRow(record) { editing = record }
            if (index < state.todayRecords.lastIndex) {
                HorizontalDivider(color = AppTheme.extended.hairline, thickness = 1.dp)
            }
        }

        if (state.defs.isNotEmpty()) {
            SectionLabel("我的运动")
            state.defs.forEach { def ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            preselectDefId = def.id
                            adding = true
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(def.name, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        ExerciseMode.valueOf(def.mode).label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { viewModel.archiveExercise(def.id) }) {
                        Text("归档")
                    }
                }
            }
        }
    }

    if (adding) {
        RecordSheet(
            defs = state.defs,
            editing = null,
            preselectDefId = preselectDefId,
            onDismiss = { adding = false },
            onSaveNewExerciseAndRecord = { name, mode, values ->
                viewModel.saveNewExerciseAndRecord(name, mode, values) { }
            },
            onSaveRecord = { defId, values -> viewModel.saveRecord(defId, values) },
            onUpdateRecord = { _, _ -> },
            onDeleteRecord = { },
        )
    }
    editing?.let { record ->
        RecordSheet(
            defs = state.defs,
            editing = record,
            onDismiss = { editing = null },
            onSaveNewExerciseAndRecord = { _, _, _ -> },
            onSaveRecord = { _, _ -> },
            onUpdateRecord = { recordId, values -> viewModel.updateRecord(recordId, values) },
            onDeleteRecord = { viewModel.deleteRecord(it) },
        )
    }
}

@Composable
private fun TodayRecordRow(record: RecordRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(record.defName, style = MaterialTheme.typography.titleSmall)
            Text(
                TimeFormats.clock(record.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.extended.inkFaint,
            )
        }
        Text(
            ExerciseValues(
                sets = record.sets,
                reps = record.reps,
                seconds = record.seconds,
                meters = record.meters,
                kilograms = record.kilograms,
            ).summary(ExerciseMode.valueOf(record.defMode)),
            style = MaterialTheme.typography.labelLarge,
            color = AppTheme.extended.move,
        )
    }
}
