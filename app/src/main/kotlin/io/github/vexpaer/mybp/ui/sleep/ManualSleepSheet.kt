package io.github.vexpaer.mybp.ui.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.vexpaer.mybp.ui.theme.AppTheme
import io.github.vexpaer.mybp.ui.util.TimeFormats
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 手动修正某一晚：两个时间选择器，保存后标记「手动修改」。
 * 起床日为键：入睡时间落在前一天，起床时间落在当天。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSleepSheet(
    target: SleepEditTarget,
    onDismiss: () -> Unit,
    onSave: (wakeDay: java.time.LocalDate, bedtimeMillis: Long, wakeMillis: Long) -> Unit,
) {
    val bedState = rememberTimePickerState(target.bedtime.hour, target.bedtime.minute, is24Hour = true)
    val wakeState = rememberTimePickerState(target.wake.hour, target.wake.minute, is24Hour = true)
    val zone = ZoneId.systemDefault()

    val bedtime = ZonedDateTime.of(target.wakeDay.minusDays(1), java.time.LocalTime.of(bedState.hour, bedState.minute), zone)
        .toInstant().toEpochMilli()
    val wake = ZonedDateTime.of(target.wakeDay, java.time.LocalTime.of(wakeState.hour, wakeState.minute), zone)
        .toInstant().toEpochMilli()
    val invalid = bedtime >= wake

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "手动修正 · ${TimeFormats.dateLabel(target.wakeDay.toEpochDay(), target.wakeDay.toEpochDay())}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "保存后会标记为「手动修改」，估算不会再覆盖这一晚。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "入睡时间",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                TimePicker(state = bedState)
                Text(
                    "起床时间",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                TimePicker(state = wakeState)
            }
            if (invalid) {
                Text(
                    "起床时间要晚于入睡时间，再检查一下？",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.extended.saltBad,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(target.wakeDay, bedtime, wake) },
                enabled = !invalid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("保存")
            }
        }
    }
}
