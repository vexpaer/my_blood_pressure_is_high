package io.github.vexpaer.mybp.ui.sleep

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vexpaer.mybp.core.sleep.SleepNight
import io.github.vexpaer.mybp.ui.theme.AppTheme
import io.github.vexpaer.mybp.ui.theme.ChartTokens
import io.github.vexpaer.mybp.ui.theme.LocalReducedMotion
import io.github.vexpaer.mybp.ui.theme.MotionTokens
import io.github.vexpaer.mybp.ui.util.TimeFormats
import java.time.ZoneId
import kotlin.math.min

/**
 * Sleep Arc —— 早睡早起页的视觉中心：
 * 半圆夜间时间轴，左端入睡、右端起床，中央是时长。
 * 首次出现时从 0% 描画到 100%（400–600ms）；数据变化时重新描画；
 * 系统关闭动画时直接呈现最终状态。
 */
@Composable
fun SleepArc(
    night: SleepNight,
    zone: ZoneId = ZoneId.systemDefault(),
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(night.wakeDayEpochDay, night.bedtimeEpochMillis, night.wakeEpochMillis) {
        if (reducedMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(MotionTokens.Draw, easing = MotionTokens.Easing))
        }
    }

    val sleepColor = AppTheme.extended.sleep
    val wakeColor = MaterialTheme.colorScheme.primary
    val hairline = AppTheme.extended.hairline
    val inkSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val description = "估算入睡 ${spokenTime(night.bedtimeEpochMillis, zone)}，" +
        "起床 ${spokenTime(night.wakeEpochMillis, zone)}，" +
        "共 ${TimeFormats.duration(night.durationMinutes)}。"

    Column(modifier = modifier.fillMaxWidth().semantics { contentDescription = description }) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Canvas(modifier = Modifier.fillMaxWidth().height(172.dp)) {
                val stroke = ChartTokens.RingStroke.toPx()
                val cx = size.width / 2f
                val cy = size.height - stroke
                val radius = min(size.width / 2f, size.height) - stroke

                // 背景弧（180°）+ 进度弧
                drawArc(
                    color = hairline,
                    startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = sleepColor,
                    startAngle = 180f, sweepAngle = 180f * progress.value, useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                // 端点：入睡（左）/ 起床（右）
                drawCircle(sleepColor, ChartTokens.Point.toPx(), Offset(cx - radius, cy))
                drawCircle(wakeColor, ChartTokens.Point.toPx(), Offset(cx + radius, cy))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                Text(
                    TimeFormats.duration(night.durationMinutes),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                TimeFormats.clock(night.bedtimeEpochMillis, zone),
                style = MaterialTheme.typography.labelMedium,
                color = inkSecondary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                TimeFormats.clock(night.wakeEpochMillis, zone),
                style = MaterialTheme.typography.labelMedium,
                color = inkSecondary,
            )
        }
    }
}

/** 无障碍朗读用：23:47 → "23 点 47 分"。 */
private fun spokenTime(epochMillis: Long, zone: ZoneId): String {
    val t = java.time.Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
    return "${t.hour} 点 ${t.minute} 分"
}
