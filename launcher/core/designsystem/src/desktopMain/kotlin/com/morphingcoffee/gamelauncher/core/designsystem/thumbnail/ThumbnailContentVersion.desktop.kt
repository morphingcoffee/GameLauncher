package com.morphingcoffee.gamelauncher.core.designsystem.thumbnail

import coil3.Bitmap

internal actual fun bitmapContentHash(bitmap: Bitmap): String {
    var hash = bitmap.width
    hash = 31 * hash + bitmap.height
    val stepX = maxOf(1, bitmap.width / 8)
    val stepY = maxOf(1, bitmap.height / 8)
    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            hash = 31 * hash + bitmap.getColor(x, y).toInt()
            x += stepX
        }
        y += stepY
    }
    return hash.toUInt().toString(16)
}
