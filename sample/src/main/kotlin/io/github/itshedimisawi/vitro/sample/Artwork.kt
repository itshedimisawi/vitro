package io.github.itshedimisawi.vitro.sample

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * The photos at half size, decoded once and shared by every row and card that shows one. Loading
 * each use at full size would keep more pictures alive than the graphics memory can hold.
 */
@Composable
fun rememberArtwork(): List<ImageBitmap> {
    val resources = LocalContext.current.resources
    return remember(resources) {
        val half = BitmapFactory.Options().apply { inSampleSize = 2 }
        Photos.map { BitmapFactory.decodeResource(resources, it, half).asImageBitmap() }
    }
}
