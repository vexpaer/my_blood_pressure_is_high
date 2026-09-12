package io.github.vexpaer.mybp.ui.salt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.vexpaer.mybp.R
import io.github.vexpaer.mybp.ui.components.EmptyState
import io.github.vexpaer.mybp.ui.theme.AppTheme

/** PR2 占位：PR4 替换为完整「少吃点盐」。 */
@Composable
fun SaltPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            iconRes = R.drawable.ic_bowl,
            tint = MaterialTheme.colorScheme.primary,
            container = MaterialTheme.colorScheme.primaryContainer,
            title = "附近的低盐选择",
            body = "看看附近有什么相对适合少吃点盐的。",
        )
    }
}
