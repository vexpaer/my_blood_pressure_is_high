package io.github.vexpaer.mybp.ui.sleep

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vexpaer.mybp.R
import io.github.vexpaer.mybp.core.sleep.SleepNight
import io.github.vexpaer.mybp.ui.components.SectionLabel
import io.github.vexpaer.mybp.ui.theme.AppTheme
import io.github.vexpaer.mybp.ui.util.TimeFormats
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** 手动修正的编辑目标：哪一晚 + 预填的入睡/起床时间。 */
data class SleepEditTarget(
    val wakeDay: LocalDate,
    val bedtime: LocalTime,
    val wake: LocalTime,
)

/**
 * 早睡早起：今晚的估算（hero）+ 最近 7 天 + 入睡/起床趋势。
 * 权限按需申请：没有「使用情况访问权限」时展示引导卡，功能本身保持可用（可手动记）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(viewModel: SleepViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()

    LifecycleResumeEffect(Unit) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }

    var editTarget by remember { mutableStateOf<SleepEditTarget?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            "早睡早起",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 20.dp),
        )

        if (!state.usageAccessGranted) {
            Spacer(Modifier.height(16.dp))
            UsageAccessCard(
                onOpen = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        TonightCard(
            tonight = state.tonight,
            granted = state.usageAccessGranted,
            onEdit = { night ->
                editTarget = SleepEditTarget(
                    wakeDay = LocalDate.ofEpochDay(night.wakeDayEpochDay),
                    bedtime = toLocalTime(night.bedtimeEpochMillis, zone),
                    wake = toLocalTime(night.wakeEpochMillis, zone),
                )
            },
            onManual = {
                editTarget = SleepEditTarget(
                    wakeDay = LocalDate.now(zone),
                    bedtime = LocalTime.of(23, 0),
                    wake = LocalTime.of(7, 0),
                )
            },
        )

        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            SectionLabel("最近 7 天")
            Spacer(Modifier.weight(1f))
            state.averageDurationMinutes?.let { avg ->
                Text(
                    "平均 ${TimeFormats.duration(avg)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val today = state.todayEpochDay
        (6 downTo 0).forEach { i ->
            val day = today - i
            val night = state.nights.firstOrNull { it.wakeDayEpochDay == day }
            // 只在"紧挨着数据的最近一个空日"提示（今天为空时必提示），避免整列噪音
            val isFirstEmptyRow = night == null &&
                (day == today || state.nights.any { it.wakeDayEpochDay == day + 1 })
            NightRow(
                night = night,
                epochDay = day,
                todayEpochDay = today,
                showHint = isFirstEmptyRow,
            ) {
                editTarget = if (night != null) {
                    SleepEditTarget(
                        wakeDay = LocalDate.ofEpochDay(day),
                        bedtime = toLocalTime(night.bedtimeEpochMillis, zone),
                        wake = toLocalTime(night.wakeEpochMillis, zone),
                    )
                } else {
                    SleepEditTarget(LocalDate.ofEpochDay(day), LocalTime.of(23, 0), LocalTime.of(7, 0))
                }
            }
            if (i > 0) HorizontalDivider(color = AppTheme.extended.hairline, thickness = 1.dp)
        }

        SectionLabel("入睡与起床趋势")
        SleepTrendChart(nights = state.nights, todayEpochDay = today, zone = zone)
    }

    editTarget?.let { target ->
        ManualSleepSheet(
            target = target,
            onDismiss = { editTarget = null },
            onSave = { day, bedtimeMillis, wakeMillis ->
                viewModel.saveManual(day, bedtimeMillis, wakeMillis)
                editTarget = null
            },
        )
    }
}

private fun toLocalTime(millis: Long, zone: ZoneId): LocalTime =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()

@Composable
private fun TonightCard(
    tonight: SleepNight?,
    granted: Boolean,
    onEdit: (SleepNight) -> Unit,
    onManual: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_moon),
                    contentDescription = null,
                    tint = AppTheme.extended.sleep,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        tonight?.isManual == true -> "昨晚 · 手动修改"
                        else -> "昨晚"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (tonight != null) {
                    TextButton(onClick = { onEdit(tonight) }) { Text("修正") }
                }
            }
            if (tonight != null) {
                Spacer(Modifier.height(8.dp))
                SleepArc(
                    night = tonight,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (tonight.isManual) "你手动修改过这一晚。" else "根据手机使用情况估算，不是医疗监测。",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.extended.inkFaint,
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (granted) "昨晚的数据还不够" else "还没有数据",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (granted) "手机使用记录太少，算不出昨晚的睡眠。可以先手动记一晚。"
                    else "开启权限后，会根据手机使用情况估算昨晚的睡眠。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                FilledTonalButton(onClick = onManual, modifier = Modifier.height(44.dp)) {
                    Text("手动记一晚")
                }
            }
        }
    }
}

@Composable
private fun UsageAccessCard(onOpen: () -> Unit) {
    Surface(
        color = AppTheme.extended.sleepContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "开启「使用情况访问权限」",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "根据手机里的『什么时候在用手机』来估算睡眠。这个权限只在这台手机上使用，不会上传任何数据。",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onOpen, modifier = Modifier.height(40.dp)) {
                    Text("去开启")
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "开启后回到 App 自动继续",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NightRow(
    night: SleepNight?,
    epochDay: Long,
    todayEpochDay: Long,
    showHint: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            TimeFormats.dateLabel(epochDay, todayEpochDay),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.width(52.dp),
        )
        if (night != null) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${TimeFormats.clock(night.bedtimeEpochMillis)} — ${TimeFormats.clock(night.wakeEpochMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 时长小条：以 13 小时为满格
                Box(
                    Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth(
                            (night.durationMinutes / 780f).coerceIn(0.06f, 1f),
                        )
                        .height(4.dp)
                        .background(
                            AppTheme.extended.sleep.copy(alpha = 0.55f),
                            RoundedCornerShape(2.dp),
                        ),
                )
            }
            Spacer(Modifier.width(12.dp))
            if (night.isManual) ManualBadge()
            Spacer(Modifier.width(8.dp))
            Text(
                TimeFormats.duration(night.durationMinutes),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.End,
            )
        } else {
            Text(
                "—",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.extended.inkFaint,
            )
            Spacer(Modifier.weight(1f))
            if (showHint) {
                Text(
                    "点一下手动记",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.extended.inkFaint,
                )
            }
        }
    }
}

@Composable
private fun ManualBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            "手动",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
