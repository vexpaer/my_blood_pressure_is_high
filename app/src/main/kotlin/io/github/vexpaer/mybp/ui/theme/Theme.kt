package io.github.vexpaer.mybp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 功能色与结构色（hairline / 功能 tint），M3 ColorScheme 之外的品牌扩展。 */
@Immutable
data class ExtendedColors(
    val hairline: Color,
    val inkFaint: Color,
    val sleep: Color,
    val sleepContainer: Color,
    val saltGood: Color,
    val saltMid: Color,
    val saltBad: Color,
    val move: Color,
    val moveContainer: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        hairline = HairlineLight,
        inkFaint = InkFaintLight,
        sleep = SleepLight,
        sleepContainer = SleepContainerLight,
        saltGood = SaltGoodLight,
        saltMid = SaltMidLight,
        saltBad = SaltBadLight,
        move = MoveLight,
        moveContainer = MoveContainerLight,
    )
}

/** 便捷访问扩展 token：`AppTheme.extended.hairline` */
object AppTheme {
    val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

private fun lightScheme(): ColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = SleepContainerLight,
    onSecondaryContainer = SleepLight,
    tertiary = SleepLight,
    onTertiary = Color.White,
    background = BgLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = InkSecondaryLight,
    outline = HairlineLight,
    outlineVariant = HairlineLight,
    error = SaltBadLight,
    onError = Color.White,
)

private fun darkScheme(): ColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = Color(0xFF14261C),
    secondaryContainer = SleepContainerDark,
    onSecondaryContainer = SleepDark,
    tertiary = SleepDark,
    onTertiary = Color(0xFF14261C),
    background = BgDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = InkSecondaryDark,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
    error = SaltBadDark,
    onError = Color(0xFF2A120B),
)

private val lightExtended = ExtendedColors(
    hairline = HairlineLight,
    inkFaint = InkFaintLight,
    sleep = SleepLight,
    sleepContainer = SleepContainerLight,
    saltGood = SaltGoodLight,
    saltMid = SaltMidLight,
    saltBad = SaltBadLight,
    move = MoveLight,
    moveContainer = MoveContainerLight,
)

private val darkExtended = ExtendedColors(
    hairline = HairlineDark,
    inkFaint = InkFaintDark,
    sleep = SleepDark,
    sleepContainer = SleepContainerDark,
    saltGood = SaltGoodDark,
    saltMid = SaltMidDark,
    saltBad = SaltBadDark,
    move = MoveDark,
    moveContainer = MoveContainerDark,
)

@Composable
fun MyBPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) darkScheme() else lightScheme()
    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) darkExtended else lightExtended) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
