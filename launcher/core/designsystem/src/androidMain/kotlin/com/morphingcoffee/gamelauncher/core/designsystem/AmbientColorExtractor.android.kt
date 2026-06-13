package com.morphingcoffee.gamelauncher.core.designsystem

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.Color
import coil3.Bitmap as CoilBitmap

actual fun extractAmbientColor(bitmap: CoilBitmap): Color {
    val readable = bitmap.toReadableBitmap()
    return weightedSampledColor(
        width = readable.width,
        height = readable.height,
        samplePixel = { x, y -> readable.getPixel(x, y) },
    )
}

private fun CoilBitmap.toReadableBitmap(): CoilBitmap {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
        return copy(Bitmap.Config.ARGB_8888, false) ?: this
    }
    return this
}
