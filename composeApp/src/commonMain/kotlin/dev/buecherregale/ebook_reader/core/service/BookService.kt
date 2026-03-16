package dev.buecherregale.ebook_reader.core.service

import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.dom.BookParserRegistry
import dev.buecherregale.ebook_reader.core.dom.Document
import dev.buecherregale.ebook_reader.core.dom.ResourceRepository
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.core.repository.BookCoverRepository
import dev.buecherregale.ebook_reader.core.repository.BookFileRepository
import dev.buecherregale.ebook_reader.core.repository.BookRepository
import dev.buecherregale.ebook_reader.core.repository.FileRepository
import dev.buecherregale.ebook_reader.core.service.filesystem.AppDirectory
import dev.buecherregale.ebook_reader.core.service.filesystem.FileRef
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import dev.buecherregale.ebook_reader.core.util.JsonUtil
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * External api to interact with the books with.
 * Handles file storage, book import and opening a book from a library.
 */
class BookService(
    private val fileService: FileService,
    private val jsonUtil: JsonUtil,

    private val repository: BookRepository,
    private val coverRepository: BookCoverRepository,
    private val fileRepository: BookFileRepository,
) {

    /**
     * Import the book into the app system.
     * Converts the book into the internal DOM structure, serializing it to file.
     * Stores the book (meta-)data.
     *
     * This method may run for quite some time, depending on the format and size of the book.
     *
     * @param bookFile the book file(s)
     *
     * @return the book instance with metadata
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun importBook(bookFile: FileRef): Book {
        Logger.i { "importing book from '$bookFile'" }

        val bookId = Uuid.generateV4().toString()

        val parser = BookParserRegistry.applyDefault().get(bookFile, fileService)
        val book = parser.parse(
            file = bookFile,
            fileService = fileService,
            resourceRepository = bookResourceRepository(bookId),
            targetId = bookId
        )

        Logger.d { "successfully parsed ${book.second.id}" }

        repository.save(bookId, book.first)
        fileRepository.save(bookId, jsonUtil.serialize(book.second).encodeToByteArray())

        parser.parseCover(bookFile, fileService)?.let { coverRepository.save(bookId, it) }

        Logger.i("imported book '${book.first.metadata.title}' with id ${book.first.id}")

        return book.first
    }

    /**
     * Reads the serialized DOM used for rendering the book.
     * This file can be quite large, reading can be quite slow, so reading DOM is separated from metadata ([.readData]).
     * This method should be called when the book gets rendered.
     *
     * @param bookId the id of the book to open
     *
     * @return the deserialized DOM
     */
    suspend fun open(bookId: String): Document {
        Logger.i { "opening book with id: $bookId" }
        val bytes = fileRepository.load(bookId)
            ?: throw IllegalArgumentException("book $bookId not found")
        return jsonUtil.deserialize(bytes.decodeToString())
    }

    /**
     * Obtains the repository where the resources for a given book are stored.
     *
     * Resources, like images, are stored as files on disk in [AppDirectory.DATA] based on the book.
     * This method creates the correct repository to manage these files for a single given book.
     *
     * **NOTE:** if the bookId is invalid, meaning no book with that id exists, the (empty) repository describing
     * the resources is still returned.
     *
     * The repository itself is the interface to manage data and does not hold any data itself. Therefore, construction is cheap.
     *
     * @param bookId the id of the book (used in file path)
     *
     * @return the repository managing the resources of a book
     */
    fun bookResourceRepository(bookId: String): ResourceRepository {
        return ResourceRepository(
            FileRepository(
                keyToFilename = { key -> "${key}.resource" },
                storeInDir = fileService.getAppDirectory(AppDirectory.DATA)
                    .resolve("books")
                    .resolve(bookId)
                    .resolve("resources"),
                fileService = fileService
            )
        )
    }

    /**
     * Reads the data file for a book (created by [.saveBookData]).
     *
     * @param bookId the id of the book
     * @return the book instance
     */
    suspend fun readData(bookId: String): Book {
        return repository.load(bookId) ?: throw IllegalArgumentException("book $bookId does not exist")
    }

    /**
     * Updates the book progress. <br></br>
     * <bold>WRITES THE BOOK DATA again</bold> <br></br>
     * Use this method instead of manipulating the [Book] class.
     *
     * @param book the book to update
     * @param newProgress the new progress value
     *
     * @return the updated book
     */
    suspend fun updateProgress(book: Book, newProgress: Double): Book {
        val updated = Book(
            book.id,
            newProgress,
            book.metadata,
        )

        Logger.i { "updating progress for book ${book.id} to ${updated.progress}" }

        saveBookData(updated)
        return updated
    }

    /**
     * Saves the book data to a JSON file. <br></br>
     * This will not only save the [Book.metadata] but the whole book itself, e.g. the progress.
     * The file name will be [.getBookDataFile].
     * <br></br>
     * This method is public to allow for updated books to be saved, e.g. new [Book.progress].
     *
     * @param book the book data to save
     */
    suspend fun saveBookData(book: Book) {
        Logger.d("saving book data for ${book.id}")
        repository.save(book.id, book)
    }

    /**
     * Reads the byte content of the file storing the cover from [.getCoverFile].
     * TODO: possibly give filetype (easy for epub via content-type). For that `readCover()` needs to return the filetype
     *
     * @param bookId the id of the book
     * @return the bytes of the cover image
     */
    suspend fun readCoverBytes(bookId: String): ByteArray? {
        return coverRepository.load(bookId)
    }
}
