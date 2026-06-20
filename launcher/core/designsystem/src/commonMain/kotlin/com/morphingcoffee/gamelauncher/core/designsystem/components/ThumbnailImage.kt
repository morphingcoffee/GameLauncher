package com.morphingcoffee.gamelauncher.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.BitmapImage
import coil3.Image
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.compose.asPainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherColors
import com.morphingcoffee.gamelauncher.core.designsystem.LauncherSpacing
import com.morphingcoffee.gamelauncher.core.designsystem.extractAmbientColor
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.THUMBNAIL_CROSSFADE_MILLIS
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.buildThumbnailValidationRequest
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.hasDiskCacheEntry
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.invalidateThumbnailMemoryCache
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.readThumbnailDiskEtag
import com.morphingcoffee.gamelauncher.core.designsystem.thumbnail.thumbnailDiskContentChanged

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
    var overlayImage by remember(imageUrl) { mutableStateOf<Image?>(null) }
    var overlayFadeRequest by remember(imageUrl) { mutableIntStateOf(0) }
    var overlayAlpha by remember(imageUrl) { mutableFloatStateOf(0f) }
    var awaitingBaseSyncClear by remember(imageUrl) { mutableStateOf(false) }
    var baseDiskSync by remember(imageUrl) { mutableIntStateOf(0) }

    val displayModel =
        remember(imageUrl, baseDiskSync) {
            if (baseDiskSync == 0) {
                imageUrl
            } else {
                ImageRequest
                    .Builder(context)
                    .data(imageUrl)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
        }

    LaunchedEffect(imageUrl) {
        overlayAlpha = 0f
        overlayImage = null
        overlayFadeRequest = 0
        awaitingBaseSyncClear = false
        baseDiskSync = 0

        val hadDiskEntry = hasDiskCacheEntry(imageLoader, imageUrl)
        val baselineEtag = readThumbnailDiskEtag(imageLoader, imageUrl)
        val result = imageLoader.execute(buildThumbnailValidationRequest(context, imageUrl))
        val success = result as? SuccessResult ?: return@LaunchedEffect
        val freshEtag = readThumbnailDiskEtag(imageLoader, imageUrl)

        if (
            !thumbnailDiskContentChanged(
                hadDiskEntry = hadDiskEntry,
                baselineEtag = baselineEtag,
                freshEtag = freshEtag,
            )
        ) {
            return@LaunchedEffect
        }

        overlayImage = success.image
        val color = extractColorFromImage(success.image)
        if (color != Color.Transparent) {
            onColorExtracted?.invoke(color)
        }
        overlayFadeRequest += 1
    }

    LaunchedEffect(overlayFadeRequest) {
        if (overlayFadeRequest == 0 || overlayImage == null) {
            return@LaunchedEffect
        }

        overlayAlpha = 0f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = THUMBNAIL_CROSSFADE_MILLIS,
                    easing = FastOutSlowInEasing,
                ),
        ) { value, _ ->
            overlayAlpha = value
        }

        invalidateThumbnailMemoryCache(imageLoader, imageUrl)
        baseDiskSync += 1
        awaitingBaseSyncClear = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SubcomposeAsyncImage(
            model = displayModel,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            success = { state ->
                LaunchedEffect(imageUrl, baseDiskSync, awaitingBaseSyncClear) {
                    if (awaitingBaseSyncClear && baseDiskSync > 0) {
                        overlayImage = null
                        overlayAlpha = 0f
                        awaitingBaseSyncClear = false
                    } else if (baseDiskSync == 0 && overlayImage == null) {
                        val extractedColor = extractColorFromImage(state.result.image)
                        if (extractedColor != Color.Transparent) {
                            onColorExtracted?.invoke(extractedColor)
                        }
                    }
                }
                SubcomposeAsyncImageContent()
            },
            loading = {
                if (overlayImage == null) {
                    ThumbnailShimmer(modifier = Modifier.fillMaxSize())
                }
            },
            error = {
                ThumbnailError(modifier = Modifier.fillMaxSize())
            },
        )

        overlayImage?.let { image ->
            Image(
                painter = remember(image, context) { image.asPainter(context) },
                contentDescription = contentDescription,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(overlayAlpha),
                contentScale = ContentScale.Crop,
            )
        }
    }
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
