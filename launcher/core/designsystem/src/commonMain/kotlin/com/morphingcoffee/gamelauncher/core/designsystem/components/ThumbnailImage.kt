package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.BitmapImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.toBitmap
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.extractAmbientColor

@Composable
fun ThumbnailImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    ambientColor: Color = Color.Transparent,
    onColorExtracted: ((Color) -> Unit)? = null,
) {
    val letterboxColor =
        ambientColor.copy(alpha = 0.12f).takeIf { ambientColor != Color.Transparent }
            ?: LauncherColors.ShimmerBase

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(4.dp))
                .background(letterboxColor),
    ) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = LauncherSpacing.DetailPaneMaxThumbnailHeight),
            contentScale = ContentScale.Fit,
            success = { state ->
                LaunchedEffect(imageUrl) {
                    val color = extractColorFromImage(state.result.image)
                    if (color != Color.Transparent) {
                        onColorExtracted?.invoke(color)
                    }
                }
                SubcomposeAsyncImageContent()
            },
            loading = {
                ThumbnailShimmer(modifier = Modifier.fillMaxSize())
            },
            error = {
                ThumbnailError(modifier = Modifier.fillMaxSize())
            },
        )
    }
}

private fun extractColorFromImage(image: coil3.Image): Color {
    val bitmap =
        when (image) {
            is BitmapImage -> image.bitmap
            else -> image.toBitmap(width = 96, height = 96)
        }
    return extractAmbientColor(bitmap)
}

@Composable
private fun ThumbnailShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thumbnail_shimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmer_offset",
    )

    Box(
        modifier =
            modifier.background(
                brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                LauncherColors.ShimmerBase,
                                LauncherColors.ShimmerHighlight,
                                LauncherColors.ShimmerBase,
                            ),
                        start = Offset(offset * 400f, offset * 400f),
                        end = Offset(offset * 400f + 300f, offset * 400f + 300f),
                    ),
            ),
    )
}

@Composable
private fun ThumbnailError(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(LauncherColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        MonoLabel(text = "[ NO SIGNAL ]", accent = true)
    }
}
