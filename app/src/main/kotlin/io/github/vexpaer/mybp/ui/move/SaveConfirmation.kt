package io.github.vexpaer.mybp.ui.move

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vexpaer.mybp.ui.theme.ChartTokens
import io.github.vexpaer.mybp.ui.theme.LocalReducedMotion
import io.github.vexpaer.mybp.ui.theme.MotionTokens

/**
 * 保存成功的轻量反馈（400–600ms）：
 * 圆环描画一圈 + 对勾划出，随后整体淡出消失。Canvas 自绘，不引 Lottie；
 * 系统关闭动画时直接显示最终状态一瞬。
 */
@Composable
fun SaveConfirmation(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val progress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            progress.snapTo(1f)
            alpha.snapTo(1f)
            kotlinx.coroutines.delay(350)
        } else {
            progress.animateTo(1f, tween(MotionTokens.Draw, easing = MotionTokens.Easing))
            alpha.animateTo(0f, tween(200))
        }
        onFinished()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val checkColor = Color(0xFF3E7C59)
        Canvas(
            modifier = Modifier
                .size(88.dp)
                .alpha(alpha.value)
                .semantics { contentDescription = "已保存" },
        ) {
            val stroke = ChartTokens.RingStrokeSmall.toPx()
            drawArc(
                color = checkColor,
                startAngle = -90f, sweepAngle = 360f * progress.value, useCenter = false,
                topLeft = Offset(stroke, stroke),
                size = Size(this.size.minDimension - stroke * 2, this.size.minDimension - stroke * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            // 对勾：进度后半段划出
            val p = ((progress.value - 0.45f) / 0.55f).coerceIn(0f, 1f)
            if (p > 0f) {
                val w = this.size.minDimension
                val start = Offset(w * 0.32f, w * 0.52f)
                val mid = Offset(w * 0.45f, w * 0.65f)
                val end = Offset(w * 0.70f, w * 0.37f)
                val midX = start.x + (mid.x - start.x) * p.coerceIn(0f, 0.5f) / 0.5f
                val midY = start.y + (mid.y - start.y) * p.coerceIn(0f, 0.5f) / 0.5f
                drawLine(checkColor, start, Offset(midX, midY), stroke, cap = StrokeCap.Round)
                if (p > 0.5f) {
                    val p2 = (p - 0.5f) / 0.5f
                    drawLine(
                        checkColor, mid,
                        Offset(mid.x + (end.x - mid.x) * p2, mid.y + (end.y - mid.y) * p2),
                        stroke, cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}
