package dev.buecherregale.ebook_reader

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.intl.Locale
import app.cash.sqldelight.db.SqlDriver
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import io.ktor.utils.io.*
import kotlinx.io.Source
import org.koin.core.module.Module

actual fun platformModule(): Module {
    TODO("Not yet implemented")
}

actual fun createSqlDriver(fileService: FileService, appName: String): SqlDriver {
    TODO("Not yet implemented")
}

@Composable
actual fun PickBook(onFilePicked: (PickedFile?) -> Unit) {
}

actual fun ByteReadChannel.asSource(): Source {
    TODO("Not yet implemented")
}

@Composable
actual fun dynamicColorSchemeLight(): ColorScheme {
    throw UnsupportedOperationException("only available on android")
}

@Composable
actual fun dynamicColorSchemeDark(): ColorScheme {
    throw UnsupportedOperationException("only available on android")
}

actual fun supportsDynamicColorScheme(): Boolean {
    return false
}

actual fun getWordBoundaryAt(
    text: String,
    offset: Int,
    locale: Locale
): TextRange? {
    TODO("Not yet implemented")
}