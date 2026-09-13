package io.github.vexpaer.mybp.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 统一可视化 tokens：Sleep Arc / Sleep Trend / Score Ring / Streak Ring / WeekBars
 * 共用同一套描边、点半径、圆角、标签与动画时长——保证四页像同一个 App。
 */
object ChartTokens {
    val Stroke: Dp = 2.dp
    val Point: Dp = 4.dp
    val BarWidth: Dp = 16.dp
    val BarCorner: Dp = 6.dp

    /** 大圆环（Streak Ring / Sleep Arc）描边。 */
    val RingStroke: Dp = 10.dp

    /** 小圆环（Score Ring）。 */
    val RingStrokeSmall: Dp = 7.dp

    const val AnimMs = MotionTokens.Draw
    const val StaggerMs = MotionTokens.Stagger
}
