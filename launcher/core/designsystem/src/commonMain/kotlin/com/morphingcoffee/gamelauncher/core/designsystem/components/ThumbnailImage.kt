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
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.toBitmap
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.extractAmbientColor
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.THUMBNAIL_CROSSFADE_MILLIS
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.buildThumbnailValidationRequest
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.invalidateThumbnailMemoryCache

@Composable
fun ThumbnailImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    ambientColor: Color = Color.Transparent,
    onColorExtracted: ((Color) -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(LauncherSpacing.DetailPaneHeroHeight),
    ) {
        if (imageUrl == null) {
            ThumbnailAbsent(
                ambientColor = ambientColor,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            ThumbnailImageContent(
                imageUrl = imageUrl,
                contentDescription = contentDescription,
                onColorExtracted = onColorExtracted,
            )
        }

        HeroViewportScrim(ambientColor = ambientColor)

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(LauncherColors.Rule.copy(alpha = LauncherColors.RuleAlpha)),
        )
    }
}

@Composable
private fun ThumbnailImageContent(
    imageUrl: String,
    contentDescription: String?,
    onColorExtracted: ((Color) -> Unit)?,
) {
    val context = LocalPlatformContext.current
    val imageLoader = remember(context) { SingletonImageLoader.get(context) }
    var refreshGeneration by remember(imageUrl) { mutableIntStateOf(0) }

    // Display request - uses standard cache strategy initially
    // If refreshGeneration > 0, we bypass memory cache to show the new version from disk
    val displayRequest =
        remember(imageUrl, refreshGeneration) {
            ImageRequest
                .Builder(context)
                .data(imageUrl)
                .apply {
                    if (refreshGeneration > 0) {
                        memoryCachePolicy(CachePolicy.DISABLED)
                    }
                }.crossfade(THUMBNAIL_CROSSFADE_MILLIS)
                .build()
        }

    // Background revalidation logic: Trust the HTTP protocol
    LaunchedEffect(imageUrl) {
        val result = imageLoader.execute(buildThumbnailValidationRequest(context, imageUrl))

        if (result is SuccessResult && result.dataSource == DataSource.NETWORK) {
            // DataSource.NETWORK means the server responded with 200 OK (not 304),
            // so we have fresh image bytes on disk now.
            invalidateThumbnailMemoryCache(imageLoader, imageUrl)
            refreshGeneration += 1

            // Update ambient color from the fresh bytes immediately
            val color = extractColorFromImage(result.image)
            if (color != Color.Transparent) {
                onColorExtracted?.invoke(color)
            }
        } else if (result is SuccessResult && refreshGeneration == 0) {
            // Initial load from cache (or first network fetch) - still update color
            val color = extractColorFromImage(result.image)
            if (color != Color.Transparent) {
                onColorExtracted?.invoke(color)
            }
        }
    }

    AsyncImage(
        model = displayRequest,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        onSuccess = { state ->
            // Update color on success if we haven't already from revalidation
            if (refreshGeneration == 0) {
                val color = extractColorFromImage(state.result.image)
                if (color != Color.Transparent) {
                    onColorExtracted?.invoke(color)
                }
            }
        },
    )
}

@Composable
private fun ThumbnailAbsent(
    ambientColor: Color,
    modifier: Modifier = Modifier,
) {
    val backdropColor =
        ambientColor.copy(alpha = 0.14f).takeIf { ambientColor != Color.Transparent }
            ?: LauncherColors.Surface

    Box(
        modifier =
            modifier.background(
                brush =
                    Brush.verticalGradient(
                        colorStops =
                            arrayOf(
                                0f to backdropColor,
                                0.55f to LauncherColors.Background.copy(alpha = 0.92f),
                                1f to LauncherColors.Background,
                            ),
                    ),
            ),
    )
}

@Composable
private fun HeroViewportScrim(ambientColor: Color) {
    val topTint =
        ambientColor.copy(alpha = 0.06f).takeIf { ambientColor != Color.Transparent }
            ?: Color.Transparent

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colorStops =
                                arrayOf(
                                    0f to topTint,
                                    0.35f to Color.Transparent,
                                    0.62f to Color.Transparent,
                                    0.88f to LauncherColors.Background.copy(alpha = 0.72f),
                                    1f to LauncherColors.Background,
                                ),
                        ),
                ),
    )
}

private fun extractColorFromImage(image: coil3.Image): Color =
    try {
        val bitmap =
            when (image) {
                is BitmapImage -> image.bitmap
                else -> image.toBitmap(width = 96, height = 96)
            }
        extractAmbientColor(bitmap)
    } catch (_: Exception) {
        Color.Transparent
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
                .fillMaxSize()
                .background(LauncherColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        MonoLabel(text = "[ NO SIGNAL ]", accent = true)
    }
}
