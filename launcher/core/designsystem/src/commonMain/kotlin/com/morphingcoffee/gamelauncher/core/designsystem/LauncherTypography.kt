package com.morphingcoffee.gamelauncher.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val MonoFamily = FontFamily.Monospace
private val DisplayFamily = FontFamily.SansSerif

val LauncherTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = DisplayFamily,
                fontWeight = FontWeight.Light,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = 3.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.25.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = MonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 1.sp,
            ),
    )
