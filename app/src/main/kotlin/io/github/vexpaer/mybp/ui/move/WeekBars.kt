package io.github.vexpaer.mybp.ui.move

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vexpaer.mybp.ui.theme.AppTheme
import io.github.vexpaer.mybp.ui.util.TimeFormats

/** 最近 7 天运动柱状条：一天一列，动了越高；没动是一个浅点。 */
@Composable
fun WeekBars(
    weekDays: List<Pair<Long, Int>>,
    todayEpochDay: Long,
    modifier: Modifier = Modifier,
) {
    val barColor = AppTheme.extended.move
    val faint = AppTheme.extended.hairline
    val maxCount = weekDays.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .semantics {
                    contentDescription = "最近 7 天运动次数：" + weekDays.joinToString {
                        "${TimeFormats.dateLabel(it.first, todayEpochDay)} ${it.second} 次"
                    }
                },
        ) {
            val step = size.width / weekDays.size
            val barWidth = 16.dp.toPx()
            weekDays.forEachIndexed { index, (_, count) ->
                val cx = step * (index + 0.5f)
                if (count <= 0) {
                    drawCircle(faint, 2.5.dp.toPx(), Offset(cx, size.height - 2.5.dp.toPx()))
                } else {
                    val h = (count.toFloat() / maxCount) * (size.height - 8.dp.toPx())
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(cx - barWidth / 2, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            weekDays.forEach { (day, _) ->
                Text(
                    TimeFormats.dateLabel(day, todayEpochDay),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
