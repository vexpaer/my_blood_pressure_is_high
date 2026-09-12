package io.github.vexpaer.mybp.ui.move

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import io.github.vexpaer.mybp.core.move.ExerciseInput
import io.github.vexpaer.mybp.core.move.ExerciseMode
import io.github.vexpaer.mybp.core.move.ExerciseValues
import io.github.vexpaer.mybp.data.db.ExerciseDefEntity
import io.github.vexpaer.mybp.data.db.RecordRow
import io.github.vexpaer.mybp.ui.theme.AppTheme

/**
 * 记一笔 / 修改记录弹层。
 * 路径：选（或新建）运动 → 按模式填数字 → 保存，目标 ≤3 步。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSheet(
    defs: List<ExerciseDefEntity>,
    editing: RecordRow?,
    preselectDefId: Long? = null,
    onDismiss: () -> Unit,
    onSaveNewExerciseAndRecord: (name: String, mode: ExerciseMode, values: ExerciseValues) -> Unit,
    onSaveRecord: (defId: Long, values: ExerciseValues) -> Unit,
    onUpdateRecord: (recordId: Long, values: ExerciseValues) -> Unit,
    onDeleteRecord: (recordId: Long) -> Unit,
) {
    // 新建运动流程 / 复用已有运动
    var creatingNew by remember(editing) { mutableStateOf(defs.isEmpty() && editing == null) }
    var newMode by remember { mutableStateOf(ExerciseMode.SETS_REPS) }
    var newName by remember { mutableStateOf("") }
    var selectedDefId by remember(editing, preselectDefId) {
        mutableStateOf<Long?>(editing?.defId ?: preselectDefId ?: defs.firstOrNull()?.id)
    }

    val activeMode: ExerciseMode = when {
        editing != null -> ExerciseMode.valueOf(editing.defMode)
        creatingNew -> newMode
        else -> ExerciseMode.valueOf(defs.firstOrNull { it.id == selectedDefId }?.mode ?: "SETS_REPS")
    }

    var sets by remember(activeMode, editing) { mutableStateOf(editing?.sets?.toString() ?: "") }
    var reps by remember(activeMode, editing) { mutableStateOf(editing?.reps?.toString() ?: "") }
    var minutes by remember(activeMode, editing) {
        mutableStateOf(editing?.seconds?.let { (it / 60.0).toString() } ?: "")
    }
    var meters by remember(activeMode, editing) { mutableStateOf(editing?.meters?.toString() ?: "") }
    var kilograms by remember(activeMode, editing) { mutableStateOf(editing?.kilograms?.toString() ?: "") }

    val secondsText: String? = if (activeMode == ExerciseMode.TIME || activeMode == ExerciseMode.DISTANCE) {
        minutes.trim().takeIf { it.isNotEmpty() }?.let {
            runCatching { ((it.toDouble() * 60).toInt()).toString() }.getOrNull()
        }
    } else {
        null
    }

    val parsed = ExerciseInput.parse(
        mode = activeMode,
        setsText = sets,
        repsText = reps,
        secondsText = secondsText,
        metersText = meters,
        kilogramsText = kilograms,
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(
                when {
                    editing != null -> "修改这条记录"
                    creatingNew -> "新建运动"
                    else -> "记一笔"
                },
                style = MaterialTheme.typography.titleMedium,
            )

            when {
                editing != null -> Text(
                    editing.defName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                creatingNew -> {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(12) },
                        label = { Text("运动名称") },
                        placeholder = { Text("比如：深蹲 / 跑步 / 锤式弯举") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    ModeChips(mode = newMode, onSelect = { newMode = it })
                }
                else -> {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        defs.forEach { def ->
                            FilterChip(
                                selected = selectedDefId == def.id,
                                onClick = { selectedDefId = def.id },
                                label = { Text(def.name, maxLines = 1) },
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { creatingNew = true },
                            label = { Text("＋ 新的运动") },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            ModeInputs(
                mode = activeMode,
                sets = sets, reps = reps, minutes = minutes, meters = meters, kilograms = kilograms,
                onSets = { sets = it }, onReps = { reps = it }, onMinutes = { minutes = it },
                onMeters = { meters = it }, onKilograms = { kilograms = it },
            )

            if (parsed.values == null && parsed.errorField != null) {
                Text(
                    "${parsed.errorField}要大于 0",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.extended.saltBad,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Row {
                if (editing != null) {
                    TextButton(onClick = {
                        onDeleteRecord(editing.id)
                        onDismiss()
                    }) { Text("删除") }
                    Spacer(Modifier.width(8.dp))
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        val values = parsed.values ?: return@Button
                        when {
                            editing != null -> onUpdateRecord(editing.id, values)
                            creatingNew -> {
                                if (newName.isBlank()) return@Button
                                onSaveNewExerciseAndRecord(newName, activeMode, values)
                            }
                            else -> selectedDefId?.let { onSaveRecord(it, values) }
                        }
                        onDismiss()
                    },
                    enabled = parsed.values != null &&
                        (!creatingNew || newName.isNotBlank()),
                    modifier = Modifier
                        .width(120.dp)
                        .height(48.dp),
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun ModeChips(mode: ExerciseMode, onSelect: (ExerciseMode) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = mode == ExerciseMode.SETS_REPS, onClick = { onSelect(ExerciseMode.SETS_REPS) }, label = { Text("组 × 次") })
            FilterChip(selected = mode == ExerciseMode.REPS, onClick = { onSelect(ExerciseMode.REPS) }, label = { Text("次数") })
            FilterChip(selected = mode == ExerciseMode.TIME, onClick = { onSelect(ExerciseMode.TIME) }, label = { Text("时间") })
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = mode == ExerciseMode.DISTANCE, onClick = { onSelect(ExerciseMode.DISTANCE) }, label = { Text("距离") })
            FilterChip(selected = mode == ExerciseMode.WEIGHT_SETS_REPS, onClick = { onSelect(ExerciseMode.WEIGHT_SETS_REPS) }, label = { Text("重量") })
        }
    }
}

@Composable
private fun ModeInputs(
    mode: ExerciseMode,
    sets: String, reps: String, minutes: String, meters: String, kilograms: String,
    onSets: (String) -> Unit, onReps: (String) -> Unit, onMinutes: (String) -> Unit,
    onMeters: (String) -> Unit, onKilograms: (String) -> Unit,
) {
    when (mode) {
        ExerciseMode.REPS -> NumField("次数", reps, onReps, "20")
        ExerciseMode.SETS_REPS -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumField("组数", sets, onSets, "3", Modifier.weight(1f))
            NumField("每组次数", reps, onReps, "12", Modifier.weight(1f))
        }
        ExerciseMode.TIME -> NumField("分钟", minutes, onMinutes, "17", hint = "支持小数，比如 2.5")
        ExerciseMode.DISTANCE -> Column {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumField("距离（米）", meters, onMeters, "2400", Modifier.weight(1f))
                NumField("分钟（可选）", minutes, onMinutes, "17", Modifier.weight(1f))
            }
            Text(
                "2400 米 = 2.4 km",
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.extended.inkFaint,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        ExerciseMode.WEIGHT_SETS_REPS -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumField("重量 kg", kilograms, onKilograms, "12.5", Modifier.weight(1f))
            NumField("组数", sets, onSets, "4", Modifier.weight(1f))
            NumField("每组次数", reps, onReps, "8", Modifier.weight(1f))
        }
    }
}

@Composable
private fun NumField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.extended.inkFaint,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
