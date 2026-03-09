package dev.buecherregale.ebook_reader.ui.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

internal actual fun ImageBitmap.encodeToByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}