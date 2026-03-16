package dev.buecherregale.ebook_reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.intl.Locale
import app.cash.sqldelight.db.SqlDriver
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import dev.buecherregale.ebook_reader.ui.components.SelectedText
import io.ktor.utils.io.*
import kotlinx.io.Source
import org.koin.core.module.Module

actual fun platformModule(): Module {
    TODO("Not yet implemented")
}

actual fun createSqlDriver(fileService: FileService, appName: String): SqlDriver {
    TODO("Not yet implemented")
}

actual fun findWordInSelection(selection: SelectedText, locale: Locale): TextRange? {
    TODO("Not yet implemented")
}

@Composable
actual fun PickBook(onFilePicked: (PickedFile?) -> Unit) {
}

actual fun ByteReadChannel.asSource(): Source {
    TODO("Not yet implemented")
}