package io.github.vexpaer.mybp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vexpaer.mybp.data.ThemeMode
import io.github.vexpaer.mybp.ui.MainViewModel
import io.github.vexpaer.mybp.ui.nav.MainShell
import io.github.vexpaer.mybp.ui.onboarding.OnboardingScreen
import io.github.vexpaer.mybp.ui.theme.MyBPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()

            MyBPTheme(
                darkTheme = when (themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (onboardingDone) {
                        // 还没读出存储值：保持主题底色，避免闪烁错误内容
                        null -> Unit
                        false -> OnboardingScreen(onFinish = viewModel::completeOnboarding)
                        else -> MainShell(
                            tabNames = viewModel.tabNames.collectAsStateWithLifecycle().value,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}
