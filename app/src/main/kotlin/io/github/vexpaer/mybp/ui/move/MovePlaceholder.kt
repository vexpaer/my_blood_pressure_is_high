package io.github.vexpaer.mybp.ui.move

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.vexpaer.mybp.R
import io.github.vexpaer.mybp.ui.components.EmptyState
import io.github.vexpaer.mybp.ui.theme.AppTheme

/** PR2 占位：PR5 替换为完整「抬腿跑跑」。 */
@Composable
fun MovePlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            iconRes = R.drawable.ic_dumbbell,
            tint = AppTheme.extended.move,
            container = AppTheme.extended.moveContainer,
            title = "今天动了没？",
            body = "记一下今天有没有动。",
        )
    }
}
