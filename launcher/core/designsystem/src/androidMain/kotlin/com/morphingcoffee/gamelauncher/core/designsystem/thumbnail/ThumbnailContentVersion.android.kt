package com.morphingcoffee.gamelauncher.core.designsystem.thumbnail

import coil3.Bitmap
import android.graphics.Bitmap as AndroidBitmap

internal actual fun bitmapContentHash(bitmap: Bitmap): String {
    val androidBitmap = bitmap as AndroidBitmap
    var hash = androidBitmap.width
    hash = 31 * hash + androidBitmap.height
    val stepX = maxOf(1, androidBitmap.width / 8)
    val stepY = maxOf(1, androidBitmap.height / 8)
    var y = 0
    while (y < androidBitmap.height) {
        var x = 0
        while (x < androidBitmap.width) {
            hash = 31 * hash + androidBitmap.getPixel(x, y)
            x += stepX
        }
        y += stepY
    }
    return hash.toUInt().toString(16)
}
