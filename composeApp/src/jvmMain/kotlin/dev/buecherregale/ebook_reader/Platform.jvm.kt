package dev.buecherregale.ebook_reader

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.intl.Locale
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.buecherregale.ebook_reader.core.service.filesystem.AppDirectory
import dev.buecherregale.ebook_reader.core.service.filesystem.FileRef
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import dev.buecherregale.ebook_reader.filesystem.DesktopFileService
import dev.buecherregale.ebook_reader.sql.EBuecherregal
import dev.buecherregale.ebook_reader.ui.pickFile
import io.ktor.utils.io.*
import io.ktor.utils.io.asSource
import kotlinx.io.Source
import kotlinx.io.buffered
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.text.BreakIterator

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun platformModule(): Module {
    return module {
        single { DesktopFileService(APP_NAME) } binds arrayOf(FileService::class)
    }
}

fun FileRef.toPath(): Path {
    return Paths.get(path)
}

@Composable
actual fun PickBook(onFilePicked: (PickedFile?) -> Unit) {
    onFilePicked(
        pickFile(
            "Books",
            listOf("pdf", "epub", "txt", "md")
        ) { file: File? ->
            if (file == null) return@pickFile null
            PickedFile(file.path)
        })
}

actual fun createSqlDriver(fileService: FileService, appName: String): SqlDriver {
    val dbFile = fileService.getAppDirectory(AppDirectory.STATE).resolve("$appName.db")
        .toPath().toFile()

    dbFile.parentFile?.mkdirs()

    return JdbcSqliteDriver(
        url = "jdbc:sqlite:${dbFile.absolutePath}",
        schema = EBuecherregal.Schema
    )
}

actual fun ByteReadChannel.asSource(): Source {
    return this.asSource().buffered()
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
    if (offset !in text.indices) return null
    val iterator = BreakIterator.getWordInstance(locale.platformLocale)
    iterator.setText(text)

    val start = iterator.preceding(offset + 1)
    val end = iterator.following(offset)

    if (start == BreakIterator.DONE || end == BreakIterator.DONE) return null
    return TextRange(start, end)
}
