package com.greenrou.kanata.features.random.content

import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun RandomImagePage(
    imageUrl: String?,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    bottomPadding: Dp,
    isImmersive: Boolean = false,
    onImmersiveChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSettingWallpaper by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            error != null -> PageError(message = error, onRetry = onRefresh, modifier = Modifier.align(Alignment.Center))

            imageUrl != null -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Random Anime Wallpaper",
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(imageUrl) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                val longPressed = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                    waitForUpOrCancellation()
                                } == null
                                if (longPressed) {
                                    onImmersiveChange(true)
                                    waitForUpOrCancellation()
                                    onImmersiveChange(false)
                                }
                            }
                        },
                    contentScale = ContentScale.Crop,
                )

                AnimatedVisibility(
                    visible = !isImmersive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                                )
                            ),
                    )
                }

                AnimatedVisibility(
                    visible = !isImmersive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                )
                            ),
                    )
                }

                AnimatedVisibility(
                    visible = !isImmersive,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = bottomPadding + 16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                downloadImage(context, imageUrl)
                                Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = "Download")
                        }

                        FilledTonalIconButton(
                            onClick = {
                                if (!isSettingWallpaper) {
                                    isSettingWallpaper = true
                                    scope.launch {
                                        try {
                                            setAsWallpaper(context, imageUrl)
                                            Toast.makeText(context, "Wallpaper set!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to set wallpaper", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isSettingWallpaper = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            enabled = !isSettingWallpaper,
                        ) {
                            if (isSettingWallpaper) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            } else {
                                Icon(Icons.Rounded.Wallpaper, contentDescription = "Set as wallpaper")
                            }
                        }

                        Button(
                            onClick = onRefresh,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                            Text("New", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private fun downloadImage(context: Context, url: String) {
    val filename = "anime_wallpaper_${System.currentTimeMillis()}.jpg"
    val request = DownloadManager.Request(Uri.parse(url)).apply {
        setTitle("Anime Wallpaper")
        setDescription("Saving to Pictures")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Kanata/$filename")
        setMimeType("image/jpeg")
    }
    context.getSystemService(DownloadManager::class.java)?.enqueue(request)
}

private suspend fun setAsWallpaper(context: Context, url: String) {
    val request = ImageRequest.Builder(context)
        .data(url)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request)
    val bitmap = (result as? SuccessResult)?.drawable?.toBitmap()
        ?: error("Could not load image")
    withContext(Dispatchers.IO) {
        WallpaperManager.getInstance(context).setBitmap(bitmap)
    }
}
