package com.morphingcoffee.gamelauncher.core.designsystem

import androidx.compose.ui.graphics.Color
import coil3.Bitmap

actual fun extractAmbientColor(bitmap: Bitmap): Color =
    weightedSampledColor(
        width = bitmap.width,
        height = bitmap.height,
        samplePixel = { x, y -> bitmap.getColor(x, y) },
    )
