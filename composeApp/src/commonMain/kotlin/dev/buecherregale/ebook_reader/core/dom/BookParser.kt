package dev.buecherregale.ebook_reader.core.dom

import dev.buecherregale.ebook_reader.core.dom.epub.EPubDomParser
import dev.buecherregale.ebook_reader.core.dom.md.MarkdownDomParser
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.core.service.filesystem.FileRef
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import kotlin.uuid.ExperimentalUuidApi

/**
 * Parses a book to the internal DOM structure, collecting metadata in the process.
 *
 * Parsers should be registered and obtained via the [BookParserRegistry].
 */
interface BookParser {

    /**
     * Low cost check, if the parser can parse the given file.
     * This is used to determine the parser by [BookParserRegistry].
     *
     * @param file        the book file
     * @param fileService the system file service to use
     * @return if this parser can handle the given file
     */
    suspend fun canParse(
        file: FileRef,
        fileService: FileService
    ): Boolean

    /**
     * Parses the file into the internal DOM structure.
     * Additionally, metadata is collected into the [Book] structure.
     *
     * **Implementation notes:**
     * - The resulting book should have the `targetId`.
     * - Local resources should be copied/written to the `resourceRepository`, to keep access later
     * - Nodes need to now the `resourceId` for the repository
     *
     * @param file                  the book file to parse
     * @param fileService           the system file service to use
     * @param resourceRepository    the repository for local resources
     * @param targetId              the id the book should have (needed to provide the correct repository)
     * @return the parsed book information and the resulting DOM structure
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun parse(
        file: FileRef,
        fileService: FileService,
        resourceRepository: ResourceRepository,
        targetId: String
    ): Pair<Book, Document>

    /**
     * Attempts to find a cover image for the book, deserializing it as a [ByteArray].
     *
     * @param file          the book file
     * @param fileService   the system file service
     * @return              the bytes for the cover image or `null` if non found or not supported
     */
    suspend fun parseCover(
        file: FileRef,
        fileService: FileService
    ): ByteArray?
}

/**
 * Registry for obtaining [BookParser].
 */
object BookParserRegistry {
    private val parser = mutableListOf<BookParser>()

    fun applyDefault(): BookParserRegistry {
        apply {
            register(EPubDomParser)
            register(MarkdownDomParser())
        }
        return this
    }

    /**
     * Registers the given book parser
     *
     * @param parser the parser to register
     */
    fun register(parser: BookParser) {
        this.parser += parser
    }

    /**
     * Finds a registered parser that can parse the given file using [BookParser.canParse].
     *
     * @param file                      the book file
     * @param fileService               the system file service
     * @return                          the appropriate book parser
     * @throws NoSuchElementException   if no parser can parse the file
     */
    suspend fun get(file: FileRef, fileService: FileService): BookParser {
        return parser.first { it.canParse(file, fileService) }
    }
}