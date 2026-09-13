package io.github.vexpaer.mybp.ui.nav

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.vexpaer.mybp.R
import io.github.vexpaer.mybp.ui.theme.AppTheme
import io.github.vexpaer.mybp.ui.theme.LocalReducedMotion
import io.github.vexpaer.mybp.ui.theme.MotionTokens

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
 * 品牌自定义底部导航（Paper Minimal 2.0）：
 * 选中药丸在 Tab 间平滑滑动（spring），图标 0.94→1.0 缩放、标签透明度过渡。
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
    val reducedMotion = LocalReducedMotion.current
    val selectedIndex = Tab.entries.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Column {
            HorizontalDivider(color = AppTheme.extended.hairline, thickness = 1.dp)
            BoxWithConstraints(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(68.dp)
                    .fillMaxWidth(),
            ) {
                val itemWidth = maxWidth / Tab.entries.size
                val pillWidth = 48.dp
                val pillTarget = itemWidth * selectedIndex + (itemWidth - pillWidth) / 2
                val pillOffset by animateDpAsState(
                    targetValue = pillTarget,
                    animationSpec = if (reducedMotion) {
                        spring<Dp>(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
                    } else {
                        MotionTokens.navSpring<Dp>()
                    },
                    label = "pillOffset",
                )

                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(pillWidth)
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(15.dp)),
                )

                Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    Tab.entries.forEachIndexed { index, tab ->
                        val name = names.getOrElse(index) { tab.defaultName }
                        val isSelected = index == selectedIndex
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
}

@Composable
private fun BottomBarItem(iconRes: Int, label: String, selected: Boolean) {
    val reducedMotion = LocalReducedMotion.current
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = if (reducedMotion) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            MotionTokens.navSpring()
        },
        label = "iconScale",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.72f,
        animationSpec = if (reducedMotion) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.9f, stiffness = 400f)
        },
        label = "labelAlpha",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = null, // 语义由父级 contentDescription 承担
            tint = contentColor,
            modifier = Modifier
                .size(22.dp)
                .scale(iconScale),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.copy(alpha = labelAlpha),
            maxLines = 1,
        )
    }
}
