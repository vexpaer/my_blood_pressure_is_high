package io.github.vexpaer.mybp.ui.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vexpaer.mybp.core.sleep.SleepNight
import io.github.vexpaer.mybp.ui.theme.AppTheme
import io.github.vexpaer.mybp.ui.util.TimeFormats
import java.time.ZoneId

/**
 * 入睡/起床时间趋势（最近 7 天）：
 * 一天一列，入睡点（夜蓝）与起床点（松绿）用一撮竖线连起来；
 * y 轴从 18:00 折叠到次日 12:00。无数据画一个浅色小点。
 */
@Composable
fun SleepTrendChart(
    nights: List<SleepNight>,
    todayEpochDay: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    modifier: Modifier = Modifier,
) {
    val days: List<Pair<Long, SleepNight?>> = (6 downTo 0).map { i ->
        val day = todayEpochDay - i
        day to nights.firstOrNull { it.wakeDayEpochDay == day }
    }
    val sleepColor = AppTheme.extended.sleep
    val wakeColor = MaterialTheme.colorScheme.primary
    val hairline = AppTheme.extended.hairline
    val inkSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .semantics {
                    contentDescription = "最近 7 天入睡与起床时间趋势"
                },
        ) {
            val labelSpace = 30.dp.toPx()
            val topHour = 18f
            val bottomHour = 36f
            fun y(hour: Float): Float = (hour - topHour) / (bottomHour - topHour) * size.height

            for (h in intArrayOf(22, 26, 30, 34)) {
                val lineY = y(h.toFloat())
                drawLine(hairline, Offset(labelSpace, lineY), Offset(size.width, lineY), 1.dp.toPx())
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${h % 24}:00",
                    topLeft = Offset(0f, lineY - 7.dp.toPx()),
                    style = TextStyle(fontSize = 9.sp, color = inkSecondary),
                )
            }

            val step = (size.width - labelSpace) / days.size
            days.forEachIndexed { index, (epochDay, night) ->
                val cx = labelSpace + step * (index + 0.5f)
                if (night == null) {
                    drawCircle(hairline, 3.dp.toPx() / 2, Offset(cx, y(27f)), style = Stroke(1.dp.toPx()))
                    return@forEachIndexed
                }
                val bedY = y(TimeFormats.hourOfDay(night.bedtimeEpochMillis, zone))
                val wakeY = y(TimeFormats.hourOfDay(night.wakeEpochMillis, zone))
                drawLine(
                    sleepColor.copy(alpha = 0.35f),
                    Offset(cx, bedY),
                    Offset(cx, wakeY),
                    2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(sleepColor, 4.dp.toPx() / 2, Offset(cx, bedY))
                drawCircle(wakeColor, 4.dp.toPx() / 2, Offset(cx, wakeY))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { (epochDay, _) ->
                Text(
                    TimeFormats.dateLabel(epochDay, todayEpochDay),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(sleepColor)
            Text("入睡", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
            LegendDot(wakeColor)
            Text("起床", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        Modifier
            .padding(end = 4.dp)
            .width(8.dp)
            .height(8.dp)
            .background(color, CircleShape),
    )
}
