package io.github.vexpaer.mybp.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vexpaer.mybp.R
import io.github.vexpaer.mybp.ui.theme.AppTheme

/** 四个底部入口：早睡早起 / 少吃点盐 / 抬腿跑跑 / 设置。 */
enum class Tab(
    val route: String,
    val iconRes: Int,
    val defaultName: String,
) {
    SLEEP("sleep", R.drawable.ic_moon, "早睡早起"),
    SALT("salt", R.drawable.ic_bowl, "少吃点盐"),
    MOVE("move", R.drawable.ic_dumbbell, "抬腿跑跑"),
    SETTINGS("settings", R.drawable.ic_sliders, "设置"),
}

/**
 * 品牌自定义底部导航（DIRECTION.md §2.7）：
 * 顶部一根 hairline，选中态为 primaryContainer 药丸 + 主色图标。
 * 不使用默认 NavigationBar。
 */
@Composable
fun BottomBar(
    names: List<String>,
    selectedRoute: String,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = AppTheme.extended.hairline, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(68.dp)
                    .fillMaxWidth(),
            ) {
                Tab.entries.forEachIndexed { index, tab ->
                    val name = names.getOrElse(index) { tab.defaultName }
                    val isSelected = tab.route == selectedRoute
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                            ) {
                                if (!isSelected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(tab)
                                }
                            }
                            .semantics {
                                selected = isSelected
                                contentDescription = name
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        BottomBarItem(iconRes = tab.iconRes, label = name, selected = isSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(iconRes: Int, label: String, selected: Boolean) {
    val pillColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = spring(stiffness = 380f),
        label = "pillColor",
    )
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(30.dp)
                .background(pillColor, RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = null, // 语义已由父级 contentDescription 承担
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
        )
    }
}
