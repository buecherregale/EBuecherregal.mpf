package dev.buecherregale.ebook_reader.core.service.filesystem

import kotlinx.io.Sink
import kotlinx.io.Source
import nl.adaptivity.xmlutil.XmlReader

/**
 * A generic interface with different methods for handling files. <br></br>
 * E.g. desktop filesystems or android content management. <br></br>
 * Works in tandem with the right [FileRef] implementation.
 */
interface FileService {
    /**
     * Reads a files whole content as (UTF-8) String.
     *
     * @param file the reference
     * @return the data
     */
    suspend fun read(file: FileRef): String

    /**
     * Reads a files whole content as (UTF-8) String, in a blocking manner.
     *
     * @param file The reference to the file to read.
     * @return The file content as a string.
     * @see [read]
     */
    fun readBlocking(file: FileRef): String

    /**
     * Reads a files' whole content to a byte array.
     *
     * @param file the reference
     * @return the whole file data
     */
    suspend fun readBytes(file: FileRef): ByteArray

    /**
     * Reads a files whole content as (UTF-8) String.
     *
     * @param directory   the app directory type to write in
     * @param relativeRef the ref to a file in this directory
     * @return the data
     */
    suspend fun read(directory: AppDirectory, relativeRef: FileRef): String

    /**
     * Opens an input stream to the file denoted by the ref.
     *
     * @param file the ref
     * @return an open input stream
     */
    fun open(file: FileRef): Source

    /**
     * Opens an output stream to the file denoted by the ref.
     *
     * @param file the ref
     * @return an open output stream
     */
    fun openSink(file: FileRef): Sink

    /**
     * Opens the file as a zip.
     *
     * @param file the ref
     * @return the file interpreted as zip
     */
    suspend fun readZip(file: FileRef): ZipFileRef

    /**
     * Writes the content as UTF-8 to the file, creating it if necessary.
     *
     * @param file the ref
     * @param content the content to write
     */
    suspend fun write(file: FileRef, content: String)

    /**
     * Writes the content bytes to the file, creating it if necessary.
     *
     * @param file the ref
     * @param content the content
     */
    suspend fun write(file: FileRef, content: ByteArray)

    /**
     * Deletes the file if it exists.
     *
     * @param file the ref
     */
    suspend fun delete(file: FileRef)

    /**
     * Copies the values read from the input stream to the file, creating/overwriting it.
     *
     * @param input the input stream
     * @param target the target file reference
     */
    suspend fun copy(input: Source, target: FileRef)

    /**
     * Checks if the given file exists.
     *
     * @param file the target file to check
     * @return if it exists
     */
    suspend fun exists(file: FileRef): Boolean

    /**
     * Checks if the given file exists, in a blocking manner.
     *
     * @param file the file to check
     * @return if the file exists
     * @see [exists]
     */
    fun existsBlocking(file: FileRef): Boolean

    /**
     * Obtains metadata from a file.
     *
     * @param file the ref
     * @return the metadata
     */
    suspend fun getMetadata(file: FileRef): FileMetadata

    /**
     * Gets the actual reference to the directory type. <br></br>
     * An example would be to refer the directory types to the xdg like [AppDirectory.CONFIG] to `XDG_CONFIG_HOME`.
     *
     * @param directory the directory type
     * @return the ref
     */
    fun getAppDirectory(directory: AppDirectory): FileRef

    /**
     * Lists the children of a ref, the files inside a folder.
     *
     * @param fileRef the ref
     * @return empty if none exist (or fileRef is a file)
     */
    suspend fun listChildren(fileRef: FileRef): List<FileRef>

    /**
     * Removes gz compression on the given byte array.
     *
     * @param bytes of a gz-compressed file
     * @return the decompressed bytes
     */
    fun ungzip(bytes: ByteArray): ByteArray

    /**
     * Removes gz compression on the given source.
     *
     * @param source of a gz-compressed file
     * @return the decompressed source
     */
    fun ungzip(source: Source): Source

    /**
     * Stream the given `Source` as XML.
     *
     * @param xmlStream the source to stream containing XML
     * @return the XML reader
     */
    fun streamXml(xmlStream: Source): XmlReader
}