package io.github.vexpaer.mybp.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.vexpaer.mybp.ui.MainViewModel
import io.github.vexpaer.mybp.ui.move.MovePlaceholder
import io.github.vexpaer.mybp.ui.salt.SaltPlaceholder
import io.github.vexpaer.mybp.ui.settings.SettingsScreen
import io.github.vexpaer.mybp.ui.sleep.SleepPlaceholder

/**
 * 应用外壳：内容区 + 品牌底部导航。
 * 页面切换用淡入 + 轻微上移（DIRECTION.md §2.6），退出不叠加动画。
 */
@Composable
fun MainShell(
    tabNames: List<String>,
    viewModel: MainViewModel,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Tab.SLEEP.route

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Tab.SLEEP.route,
                enterTransition = {
                    fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 24 }
                },
                exitTransition = { fadeOut(tween(120)) },
                popEnterTransition = { fadeIn(tween(180)) },
                popExitTransition = { fadeOut(tween(120)) },
            ) {
                composable(Tab.SLEEP.route) { SleepPlaceholder() }
                composable(Tab.SALT.route) { SaltPlaceholder() }
                composable(Tab.MOVE.route) { MovePlaceholder() }
                composable(Tab.SETTINGS.route) { SettingsScreen(viewModel = viewModel) }
            }
        }
        BottomBar(
            names = tabNames,
            selectedRoute = currentRoute,
            onSelect = { tab ->
                if (tab.route != currentRoute) {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
        )
    }
}
