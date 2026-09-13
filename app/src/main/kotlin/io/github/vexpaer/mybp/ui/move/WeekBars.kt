package io.github.vexpaer.mybp.ui.move

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.vexpaer.mybp.ui.theme.AppTheme
import io.github.vexpaer.mybp.ui.theme.ChartTokens
import io.github.vexpaer.mybp.ui.theme.LocalReducedMotion
import io.github.vexpaer.mybp.ui.theme.MotionTokens
import io.github.vexpaer.mybp.ui.util.TimeFormats

/**
 * 最近 7 天运动柱状条（Paper Minimal 2.0）：
 * 进入页面柱条从底部生长（错峰），记录新运动时今天的柱子平滑长高；
 * 没动的一天是一个浅点。系统关闭动画时直接呈现最终高度。
 */
@Composable
fun WeekBars(
    weekDays: List<Pair<Long, Int>>,
    todayEpochDay: Long,
    modifier: Modifier = Modifier,
) {
    val barColor = AppTheme.extended.move
    val faint = AppTheme.extended.hairline
    val reducedMotion = LocalReducedMotion.current
    val entry = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            entry.snapTo(1f)
        } else {
            entry.animateTo(1f, tween(MotionTokens.Grow, easing = MotionTokens.Easing))
        }
    }

    val maxCount = weekDays.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val animatedCounts = weekDays.map { (_, count) ->
        val animated by animateFloatAsState(
            targetValue = count.toFloat(),
            animationSpec = if (reducedMotion) {
                tween(0)
            } else {
                tween(MotionTokens.Grow, easing = MotionTokens.Easing)
            },
            label = "barCount",
        )
        animated
    }

    val trainedDays = weekDays.count { it.second > 0 }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .semantics {
                    contentDescription = "最近 7 天运动 $trainedDays 天。" + weekDays.joinToString {
                        "${TimeFormats.dateLabel(it.first, todayEpochDay)} ${it.second} 次"
                    }
                },
        ) {
            val step = size.width / weekDays.size
            val barWidth = ChartTokens.BarWidth.toPx()
            weekDays.forEachIndexed { index, (_, count) ->
                val cx = step * (index + 0.5f)
                val value = animatedCounts[index] * entry.value
                if (value <= 0.05f) {
                    drawCircle(faint, 2.5.dp.toPx(), Offset(cx, size.height - 2.5.dp.toPx()))
                } else {
                    val h = (value / maxCount) * (size.height - 8.dp.toPx())
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(cx - barWidth / 2, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(ChartTokens.BarCorner.toPx()),
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
