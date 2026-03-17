package dev.buecherregale.ebook_reader.filesystem

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dev.buecherregale.ebook_reader.core.service.filesystem.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.*
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.newReader
import nl.adaptivity.xmlutil.xmlStreaming
import java.io.*
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

class AndroidFileService(val context: Context) : FileService {

    private fun FileRef.toFile(): File = File(path)

    override suspend fun read(file: FileRef): String {
        return withContext(Dispatchers.IO) {
            file.toFile().readText(StandardCharsets.UTF_8)
        }
    }

    override fun readBlocking(file: FileRef): String {
        return file.toFile().readText(StandardCharsets.UTF_8)
    }

    override suspend fun readBytes(file: FileRef): ByteArray {
        return withContext(Dispatchers.IO) {
            file.toFile().readBytes()
        }
    }

    override suspend fun read(
        directory: AppDirectory,
        relativeRef: FileRef
    ): String {
        return withContext(Dispatchers.IO) {
            val dir = getAppDirectory(directory).toFile()
            File(dir, relativeRef.path).readText(StandardCharsets.UTF_8)
        }
    }

    override fun open(file: FileRef): Source {
        return file.toFile().inputStream().asSource().buffered()
    }

    override fun openSink(file: FileRef): Sink {
        val targetFile = file.toFile()
        targetFile.parentFile?.mkdirs()
        return targetFile.outputStream().asSink().buffered()
    }

    override suspend fun readZip(file: FileRef): ZipFileRef {
        return AndroidZipFileRef(withContext(Dispatchers.IO) {
            ZipFile(file.toFile())
        })
    }

    override suspend fun write(
        file: FileRef,
        content: String
    ) {
        write(file, content.toByteArray(StandardCharsets.UTF_8))
    }

    override suspend fun write(
        file: FileRef,
        content: ByteArray
    ) {
        val targetFile = file.toFile()
        withContext(Dispatchers.IO) {
            targetFile.parentFile?.mkdirs()
            targetFile.writeBytes(content)
        }
    }

    override suspend fun delete(file: FileRef) {
        withContext(Dispatchers.IO) {
            file.toFile().delete()
        }
    }

    override suspend fun copy(
        input: Source,
        target: FileRef
    ) {
        val targetFile = target.toFile()
        withContext(Dispatchers.IO) {
            targetFile.parentFile?.mkdirs()
            targetFile.outputStream().use { output ->
                input.asInputStream().copyTo(output)
            }
        }
    }

    override suspend fun exists(file: FileRef): Boolean {
        return withContext(Dispatchers.IO) {
            file.toFile().exists()
        }
    }

    override fun existsBlocking(file: FileRef): Boolean {
        return file.toFile().exists()
    }

    override suspend fun getMetadata(file: FileRef): FileMetadata {
        val targetFile = file.toFile()
        if (!withContext(Dispatchers.IO) { targetFile.exists() }) {
            throw FileNotFoundException("file $file does not exist")
        }
        val fileName = targetFile.name
        val firstDot = fileName.indexOf('.')
        if (firstDot == -1) {
            return FileMetadata(
                fileName,
                extension = null,
                size = if (targetFile.isDirectory) 0 else targetFile.length(),
                isDirectory = targetFile.isDirectory
            )
        }
        return FileMetadata(
            name = fileName.take(firstDot),
            extension = fileName.substring(firstDot),
            size = if (targetFile.isDirectory) 0 else targetFile.length(),
            isDirectory = targetFile.isDirectory
        )
    }

    override fun getAppDirectory(directory: AppDirectory): FileRef {
        val dir = when (directory) {
            AppDirectory.CONFIG -> File(context.filesDir, "config")
            AppDirectory.DATA -> File(context.filesDir, "data")
            AppDirectory.STATE -> File(context.filesDir, "state")
        }
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw IOException("Could not create directory $dir")
            }
        }
        return FileRef(dir.absolutePath)
    }

    override suspend fun listChildren(fileRef: FileRef): List<FileRef> {
        val targetFile = fileRef.toFile()
        if (!targetFile.isDirectory) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            targetFile.listFiles()?.map { FileRef(it.absolutePath) } ?: emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun ungzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(bytes)).use { gzis ->
            gzis.transferTo(out)
        }
        return out.toByteArray()
    }

    override fun ungzip(source: Source): Source {
        return GZIPInputStream(source.asInputStream(), 8192).asSource().buffered()
    }

    override fun streamXml(xmlStream: Source): XmlReader {
        return xmlStreaming.newReader(xmlStream.asInputStream())
    }
}
