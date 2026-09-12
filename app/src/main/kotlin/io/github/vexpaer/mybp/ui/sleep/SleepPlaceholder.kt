package io.github.vexpaer.mybp.ui.sleep

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.vexpaer.mybp.R
import io.github.vexpaer.mybp.ui.components.EmptyState
import io.github.vexpaer.mybp.ui.theme.AppTheme

/** PR2 占位：PR3 替换为完整「早睡早起」。 */
@Composable
fun SleepPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            iconRes = R.drawable.ic_moon,
            tint = AppTheme.extended.sleep,
            container = AppTheme.extended.sleepContainer,
            title = "昨晚睡了多久？",
            body = "根据手机使用情况，估算昨晚睡了多久。",
        )
    }
}
