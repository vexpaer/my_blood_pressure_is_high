package io.github.vexpaer.mybp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.vexpaer.mybp.ui.BootstrapScreen
import io.github.vexpaer.mybp.ui.theme.MyBPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyBPTheme {
                BootstrapScreen()
            }
        }
    }
}
