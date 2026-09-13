package io.github.vexpaer.mybp.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** 统一动效 tokens（DIRECTION.md Paper Minimal 2.0 §Motion）。 */
object MotionTokens {
    val Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    const val Fast = 150
    const val Normal = 220
    const val Emphasized = 320

    /** 图形描画（Arc / Ring）首次出现：400–600ms。 */
    const val Draw = 500

    /** 柱条生长：300–450ms。 */
    const val Grow = 380

    /** Marker 错峰间隔。 */
    const val Stagger = 30L

    fun <T> navSpring() = spring<T>(dampingRatio = 0.85f, stiffness = 380f)
}

/** 系统「移除动画」开启时为 true：所有进场/描画动画直接呈现最终状态。 */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun provideReducedMotion(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scale = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    CompositionLocalProvider(LocalReducedMotion provides (scale == 0f), content = content)
}
