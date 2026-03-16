package dev.buecherregale.ebook_reader

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import dev.buecherregale.ebook_reader.filesystem.AndroidFileService
import dev.buecherregale.ebook_reader.sql.EBuecherregal
import io.ktor.utils.io.*
import io.ktor.utils.io.asSource
import kotlinx.io.Source
import kotlinx.io.buffered
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module
import java.io.File
import java.io.FileOutputStream

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
actual fun platformModule(): Module {
    return module {
        single { AndroidFileService(get()) } binds arrayOf(FileService::class)
    }
}

@Composable
actual fun PickBook(onFilePicked: (PickedFile?) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            onFilePicked(null)
            return@rememberLauncherForActivityResult
        }

        val contentResolver = context.contentResolver
        val fileName = getFileName(context, uri) ?: "temp_book"
        val cacheFile = File(context.cacheDir, fileName)

        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            onFilePicked(PickedFile(cacheFile.absolutePath))
        } catch (e: Exception) {
            e.printStackTrace()
            onFilePicked(null)
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("application/epub+zip", "application/pdf"))
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor.use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1 && cut != null) {
            result = result.substring(cut + 1)
        }
    }
    return result
}


actual fun createSqlDriver(fileService: FileService, appName: String): SqlDriver {
    if (fileService is AndroidFileService) {
        return AndroidSqliteDriver(EBuecherregal.Schema, fileService.context, "$appName.db")
    }
    throw IllegalArgumentException("FileService must be AndroidFileService on Android")
}

actual fun ByteReadChannel.asSource(): Source {
    return this.asSource().buffered()
}