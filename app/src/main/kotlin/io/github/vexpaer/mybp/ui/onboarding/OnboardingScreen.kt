package io.github.vexpaer.mybp.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.github.vexpaer.mybp.R
import io.github.vexpaer.mybp.ui.theme.AppTheme

private data class OnboardingPage(
    val iconRes: Int,
    val accent: @Composable () -> Color,
    val container: @Composable () -> Color,
    val title: String,
    val body: String,
)

/**
 * 首次打开的 3 页引导（DIRECTION.md）：极简、不申请任何权限（Permission on demand）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = listOf(
        OnboardingPage(
            iconRes = R.drawable.ic_moon,
            accent = { AppTheme.extended.sleep },
            container = { AppTheme.extended.sleepContainer },
            title = "早睡早起",
            body = "根据手机使用情况，估算昨晚睡了多久。",
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_bowl,
            accent = { MaterialTheme.colorScheme.primary },
            container = { MaterialTheme.colorScheme.primaryContainer },
            title = "少吃点盐",
            body = "看看附近有什么相对适合少吃点盐的。",
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_dumbbell,
            accent = { AppTheme.extended.move },
            container = { AppTheme.extended.moveContainer },
            title = "抬腿跑跑",
            body = "记一下今天有没有动。",
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (pagerState.currentPage < pages.lastIndex) {
                TextButton(onClick = onFinish) { Text("跳过") }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(p.container(), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(p.iconRes),
                        contentDescription = null,
                        tint = p.accent(),
                        modifier = Modifier.size(38.dp),
                    )
                }
                Spacer(Modifier.height(28.dp))
                Text(p.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(
                    p.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pages.size) { i ->
                val selected = pagerState.currentPage == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
                )
            }
        }

        Button(
            onClick = {
                if (pagerState.currentPage < pages.lastIndex) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp),
        ) {
            Text(if (pagerState.currentPage == pages.lastIndex) "开始使用" else "继续")
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "数据只存在这台手机上。",
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.extended.inkFaint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.Center,
        )
    }
}
