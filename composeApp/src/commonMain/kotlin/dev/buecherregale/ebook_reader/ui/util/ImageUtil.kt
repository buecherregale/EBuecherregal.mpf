package dev.buecherregale.ebook_reader.ui.util

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.buecherregale.ebook_reader.core.domain.Library
import dev.buecherregale.ebook_reader.core.service.BookService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke

/**
 * Convert an existing bitmap to a byte array with a platform dependent handling.
 *
 * This function is used to save images e.g. created via [generateLibraryImage] to disk.
 * The given `ByteArray` has to be convertible to an `ImageBitmap` via the [ByteArray.decodeToImageBitmap] method.
 *
 * There are no restrictions for compression, etc.
 *
 * @return the bytes of the image
 */
internal expect fun ImageBitmap.encodeToByteArray(): ByteArray

/**
 * Generates a library image by merging 1 - 4 of the book covers.
 *
 * If the library contains no books, no cover can be created so `null` is returned.
 * The image is automatically saved and reread.
 *
 * @param library The library
 * @return the bytes of the image
 *
 * @see [combineBitmaps]
 */
internal suspend fun generateLibraryImage(
    bookService: BookService,
    library: Library,
    imageTargetSize: Int = 600
): ImageBitmap? {
    val coverBytes = library.bookIds
        .mapNotNull { bookId -> bookService.readCoverBytes(bookId) }
        .take(4)
        .map { it.decodeToImageBitmap() }

    if (coverBytes.isEmpty()) {
        return null
    }
    return Dispatchers.Default {
        val combined = combineBitmaps(coverBytes, imageTargetSize) ?: return@Default null
        return@Default combined
    }
}

/**
 * Combines 1 - 4 bitmaps to a single image:
 * - 0 bitmaps:   `null` returned
 * - 1 bitmap:    the bitmap is returned unchanged
 * - 2 bitmaps:   left and right next to each other
 * - 3 bitmaps:   left and right next to each other and one wider below
 * - 4 bitmaps:   each bitmap filling the quadrant
 */
internal fun combineBitmaps(
    bitmaps: List<ImageBitmap>,
    outputSize: Int = 600
):
        ImageBitmap? {
    if (bitmaps.isEmpty()) return null
    if (bitmaps.size == 1) return bitmaps[0].scaledToFit(outputSize, outputSize)

    val half = outputSize / 2

    val combined = ImageBitmap(outputSize, outputSize)
    val canvas = Canvas(combined)

    val rects: List<Rect> = when (bitmaps.size) {
        2 -> listOf(
            Rect(0f, 0f, half.toFloat(), outputSize.toFloat()),
            Rect(half.toFloat(), 0f, outputSize.toFloat(), outputSize.toFloat())
        )

        3 -> listOf(
            Rect(0f, 0f, half.toFloat(), half.toFloat()),
            Rect(half.toFloat(), 0f, outputSize.toFloat(), half.toFloat()),
            Rect(0f, half.toFloat(), outputSize.toFloat(), outputSize.toFloat())
        )

        else -> listOf(
            Rect(0f, 0f, half.toFloat(), half.toFloat()),
            Rect(half.toFloat(), 0f, outputSize.toFloat(), half.toFloat()),
            Rect(0f, half.toFloat(), half.toFloat(), outputSize.toFloat()),
            Rect(half.toFloat(), half.toFloat(), outputSize.toFloat(), outputSize.toFloat())
        )
    }

    bitmaps.zip(rects).forEach { (bitmap, destRect) ->
        val destWidth = (destRect.right - destRect.left).toInt()
        val destHeight = (destRect.bottom - destRect.top).toInt()

        val srcRect = bitmap.centerCropSrcRect(destWidth, destHeight)

        canvas.drawImageRect(
            image = bitmap,
            srcOffset = IntOffset(srcRect.left.toInt(), srcRect.top.toInt()),
            srcSize = IntSize(srcRect.width.toInt(), srcRect.height.toInt()),
            dstOffset = IntOffset(destRect.left.toInt(), destRect.top.toInt()),
            dstSize = IntSize(destWidth, destHeight),
            paint = Paint()
        )
    }

    return combined
}

/**
 * Computes the centered crop source rect from this bitmap
 * that matches the aspect ratio of [destWidth]x[destHeight].
 */
private fun ImageBitmap.centerCropSrcRect(destWidth: Int, destHeight: Int): Rect {
    val srcAspect = width.toFloat() / height.toFloat()
    val dstAspect = destWidth.toFloat() / destHeight.toFloat()

    return if (srcAspect > dstAspect) {
        val srcWidth = (height * dstAspect).toInt()
        val left = (width - srcWidth) / 2f
        Rect(left, 0f, left + srcWidth, height.toFloat())
    } else {
        val srcHeight = (width / dstAspect).toInt()
        val top = (height - srcHeight) / 2f
        Rect(0f, top, width.toFloat(), top + srcHeight)
    }
}

/**
 * Downscales this bitmap to fit within [maxWidth]x[maxHeight],
 * preserving aspect ratio.
 *
 * If the original bitmap is smaller, it is returned unchanged.
 */
private fun ImageBitmap.scaledToFit(maxWidth: Int, maxHeight: Int): ImageBitmap {
    if (width <= maxWidth && height <= maxHeight) return this
    val scale = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()

    val result = ImageBitmap(newWidth, newHeight)
    val canvas = Canvas(result)
    canvas.drawImageRect(
        image = this,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(width, height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(newWidth, newHeight),
        paint = Paint()
    )
    return result
}