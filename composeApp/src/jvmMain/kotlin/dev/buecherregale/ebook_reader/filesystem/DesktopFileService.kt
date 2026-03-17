package dev.buecherregale.ebook_reader.filesystem

import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.service.filesystem.*
import dev.buecherregale.ebook_reader.toPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.*
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.newReader
import nl.adaptivity.xmlutil.xmlStreaming
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

class DesktopFileService(appName: String) : FileService {

    private val configDir: Path = configDir(appName)
    private val stateDir: Path = stateDir(appName)
    private val dataDir: Path = dataDir(appName)

    private fun configDir(appName: String): Path {
        val xdg = System.getenv("XDG_CONFIG_HOME")
        if (xdg != null)
            return Path.of(xdg, appName)
        val home = System.getProperty("user.home")
        if (home != null)
            return Path.of(System.getProperty("user.home"), ".$appName", "config")
        val fallback = Path.of(appName).toAbsolutePath()
        Logger.w { "XDG_CONFIG_HOME and 'user.home' not not set, fallback to $fallback" }
        return fallback
    }

    private fun stateDir(appName: String): Path {
        val xdg = System.getenv("XDG_STATE_HOME")
        if (xdg != null)
            return Path.of(xdg, appName)
        val home = System.getProperty("user.home")
        if (home != null)
            return Path.of(System.getProperty("user.home"), ".$appName", "state")
        val fallback = Path.of(appName).toAbsolutePath()
        Logger.w { "XDG_STATE_HOME and 'user.home' not not set, fallback to $fallback" }
        return fallback
    }

    private fun dataDir(appName: String): Path {
        val xdg = System.getenv("XDG_DATA_HOME")
        if (xdg != null)
            return Path.of(xdg, appName)
        val home = System.getProperty("user.home")
        if (home != null)
            return Path.of(System.getProperty("user.home"), ".$appName", "data")
        val fallback = Path.of(appName).toAbsolutePath()
        Logger.w { "XDG_STATE_HOME and 'user.home' not not set, fallback to $fallback" }
        return fallback
    }

    override suspend fun read(file: FileRef): String {
        return withContext(Dispatchers.IO) {
            Files.readString(file.toPath())
        }
    }

    override fun readBlocking(file: FileRef): String {
        return Files.readString(file.toPath())
    }

    override suspend fun readBytes(file: FileRef): ByteArray {
        return withContext(Dispatchers.IO) {
            Files.readAllBytes(file.toPath())
        }
    }

    override suspend fun read(
        directory: AppDirectory,
        relativeRef: FileRef
    ): String {
        return withContext(Dispatchers.IO) {
            Files.readString(getAppDirectory(directory).resolve(relativeRef).toPath())
        }
    }

    override fun open(file: FileRef): Source {
        return Files.newInputStream(file.toPath(), StandardOpenOption.READ)
            .asSource()
            .buffered()
    }

    override fun openSink(file: FileRef): Sink {
        val path = file.toPath()
        Files.createDirectories(path.parent)
        return Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            .asSink()
            .buffered()
    }

    override suspend fun readZip(file: FileRef): ZipFileRef {
        return DesktopZipFileRef(withContext(Dispatchers.IO) {
            ZipFile(file.toPath().toFile())
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
        val path = file.toPath()
        withContext(Dispatchers.IO) {
            Files.createDirectories(path.parent)
        }
        withContext(Dispatchers.IO) {
            Files.write(
                path,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        }
    }

    override suspend fun delete(file: FileRef) {
        withContext(Dispatchers.IO) {
            Files.delete(file.toPath())
        }
    }

    override suspend fun copy(
        input: Source,
        target: FileRef
    ) {
        val path = target.toPath()
        withContext(Dispatchers.IO) {
            Files.createDirectories(path.parent)
        }
        input.use {
            Files.copy(input.asInputStream(), path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override suspend fun exists(file: FileRef): Boolean {
        return Files.exists(file.toPath())
    }

    override fun existsBlocking(file: FileRef): Boolean {
        return Files.exists(file.toPath())
    }

    override suspend fun getMetadata(file: FileRef): FileMetadata {
        val path = file.toPath()
        if (!Files.exists(path)) {
            throw java.io.FileNotFoundException("file $file does not exist")
        }
        val fileName = path.fileName.toString()
        val firstDot = fileName.indexOf('.')
        if (firstDot == -1) {
            return FileMetadata(
                fileName,
                extension = null,
                size = if (Files.isDirectory(path)) 0 else withContext(Dispatchers.IO) {
                    Files.size(path)
                },
                isDirectory = Files.isDirectory(path)
            )
        }
        return FileMetadata(
            name = fileName.take(firstDot),
            extension = fileName.substring(firstDot),
            size = if (Files.isDirectory(path)) 0 else withContext(Dispatchers.IO) {
                Files.size(path)
            },
            isDirectory = Files.isDirectory(path)
        )
    }

    override fun getAppDirectory(directory: AppDirectory): FileRef {
        val dir = when (directory) {
            AppDirectory.CONFIG -> configDir
            AppDirectory.DATA -> dataDir
            AppDirectory.STATE -> stateDir
        }
        try {
            Files.createDirectories(dir)
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
        return FileRef(dir.toString())
    }

    override suspend fun listChildren(fileRef: FileRef): List<FileRef> {
        val path = fileRef.toPath()
        if (!Files.isDirectory(path)) {
            return kotlin.collections.mutableListOf()
        }

        try {
            withContext(Dispatchers.IO) {
                Files.newDirectoryStream(path)
            }.use { stream ->
                val result: MutableList<FileRef> = ArrayList()
                for (p in stream) {
                    result.add(FileRef(p.toString()))
                }
                return result
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    override fun ungzip(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(bytes)).use { gzis ->
            gzis.transferTo(out)
        }
        return out.toByteArray()
    }

    override fun ungzip(source: Source): Source {
        return GZIPInputStream(source.asInputStream()).asSource().buffered()
    }

    override fun streamXml(xmlStream: Source): XmlReader {
        return xmlStreaming.newReader(xmlStream.asInputStream())
    }
}