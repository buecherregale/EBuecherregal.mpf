package dev.buecherregale.ebook_reader.ui.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint

internal fun combineBitmaps(bitmaps: List<ImageBitmap>): ImageBitmap {
    if (bitmaps.isEmpty()) return ImageBitmap(1, 1)
    if (bitmaps.size == 1) return bitmaps[0]

    val bitWidth = bitmaps[0].width
    val bitHeight = bitmaps[0].height

    val (totalWidth, totalHeight) = when (bitmaps.size) {
        2 -> Pair(bitWidth * 2, bitHeight)
        3 -> Pair(bitWidth * 2, bitHeight * 2)
        else -> Pair(bitWidth * 2, bitHeight * 2)
    }

    val combined = ImageBitmap(totalWidth, totalHeight)
    val canvas = Canvas(combined)
    val paint = Paint()

    when (bitmaps.size) {
        2 -> {
            canvas.drawImage(bitmaps[0], Offset(0f, 0f), paint)
            canvas.drawImage(bitmaps[1], Offset(bitWidth * 1f, 0f), paint)
        }

        3 -> {
            canvas.drawImage(bitmaps[0], Offset(0f, 0f), paint)
            canvas.drawImage(bitmaps[1], Offset(bitWidth * 1f, 0f), paint)
            canvas.drawImage(bitmaps[2], Offset(bitWidth / 2f, bitHeight * 1f), paint)
        }

        4 -> {
            canvas.drawImage(bitmaps[0], Offset(0f, 0f), paint)
            canvas.drawImage(bitmaps[1], Offset(bitWidth * 1f, 0f), paint)
            canvas.drawImage(bitmaps[2], Offset(0f, bitHeight * 1f), paint)
            canvas.drawImage(bitmaps[3], Offset(bitWidth * 1f, bitHeight * 1f), paint)
        }
    }

    return combined
}