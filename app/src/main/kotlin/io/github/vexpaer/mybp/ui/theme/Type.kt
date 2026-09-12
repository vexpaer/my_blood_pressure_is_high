package io.github.vexpaer.mybp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字阶（DIRECTION.md §2.3）。中文正文用系统默认无衬线；
 * 数字统一 tabular（tnum），大数字不跳动。
 */
private const val TNUM = "tnum"

val AppTypography = Typography(
    displayMedium = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight(700),
        fontFeatureSettings = TNUM,
    ),
    displaySmall = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight(700),
        fontFeatureSettings = TNUM,
    ),
    headlineSmall = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight(600),
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight(600),
    ),
    titleMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(600),
    ),
    titleSmall = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(600),
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(400),
    ),
    bodyMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight(400),
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight(400),
    ),
    labelLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight(600),
    ),
    labelMedium = TextStyle(
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight(500),
    ),
    labelSmall = TextStyle(
        fontSize = 11.5.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight(400),
        fontFeatureSettings = TNUM,
    ),
)
