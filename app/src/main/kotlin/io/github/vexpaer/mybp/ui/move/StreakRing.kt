package io.github.vexpaer.mybp.ui.move

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vexpaer.mybp.ui.theme.AppTheme
import io.github.vexpaer.mybp.ui.theme.ChartTokens
import io.github.vexpaer.mybp.ui.theme.LocalReducedMotion
import io.github.vexpaer.mybp.ui.theme.MotionTokens

/**
 * Streak Ring —— 抬腿跑跑页的视觉中心：
 * 柿子橙圆环，7 天为满圈（连续天数不会永远满环），中央是数字。
 * 进入页面从 0 扫到目标；系统关闭动画时直接呈现。
 */
@Composable
fun StreakRing(
    streak: Int,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
) {
    val moveColor = AppTheme.extended.move
    val hairline = AppTheme.extended.hairline
    val inkSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val reducedMotion = LocalReducedMotion.current
    val fraction = (streak.coerceAtMost(7)) / 7f
    val progress = remember { Animatable(if (reducedMotion) fraction else 0f) }

    LaunchedEffect(streak) {
        if (reducedMotion) {
            progress.snapTo(fraction)
        } else {
            progress.snapTo(0f)
            progress.animateTo(fraction, tween(MotionTokens.Draw, easing = MotionTokens.Easing))
        }
    }

    val description = "连续运动 $streak 天，7 天为满圈。"
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = ChartTokens.RingStroke.toPx()
                val inset = stroke
                val dim = this.size.minDimension - inset * 2
                drawArc(
                    color = hairline,
                    startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(dim, dim),
                    style = Stroke(stroke, cap = StrokeCap.Butt),
                )
                drawArc(
                    color = moveColor,
                    startAngle = -90f, sweepAngle = 360f * progress.value, useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(dim, dim),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Text(
                "$streak",
                style = MaterialTheme.typography.displaySmall,
                color = moveColor,
            )
        }
        Text(
            "连续天数",
            style = MaterialTheme.typography.labelSmall,
            color = inkSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
