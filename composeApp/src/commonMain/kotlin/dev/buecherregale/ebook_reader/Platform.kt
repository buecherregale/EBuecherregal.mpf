package dev.buecherregale.ebook_reader

import androidx.compose.runtime.Composable
import app.cash.sqldelight.db.SqlDriver
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import ebuecherregal.composeapp.generated.resources.Res
import ebuecherregal.composeapp.generated.resources.icon_colored
import io.ktor.utils.io.*
import kotlinx.io.Source
import org.jetbrains.compose.resources.DrawableResource
import org.koin.core.module.Module

interface Platform {
    val name: String
}

/**
 * Workaround cause I got no clue how to access
 * `Res.drawable` from the desktopApp
 *
 * @return the app icon
 */
fun icon(): DrawableResource {
    return Res.drawable.icon_colored
}

expect fun getPlatform(): Platform
expect fun platformModule(): Module

@Composable
expect fun PickBook(onFilePicked: (PickedFile?) -> Unit)

expect fun createSqlDriver(fileService: FileService, appName: String): SqlDriver

expect fun ByteReadChannel.asSource(): Source

data class PickedFile(
    val path: String
)

data class PickedImage(
    val name: String?,
    val bytes: ByteArray,
    val mimeType: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PickedImage

        if (name != other.name) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        return result
    }
}
