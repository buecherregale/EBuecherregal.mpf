package dev.buecherregale.ebook_reader.ui.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

internal actual fun ImageBitmap.encodeToByteArray(): ByteArray {
    return Image.makeFromBitmap(this.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)!!.bytes
}