package dev.buecherregale.ebook_reader.core.service

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.domain.Library
import dev.buecherregale.ebook_reader.core.repository.LibraryImageRepository
import dev.buecherregale.ebook_reader.core.repository.LibraryRepository
import dev.buecherregale.ebook_reader.core.service.filesystem.AppDirectory
import dev.buecherregale.ebook_reader.core.service.filesystem.FileRef
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Service-API to handle [Library] instances. As the domain classes are DTOs, use service classes to manipulate them.
 */
@OptIn(ExperimentalUuidApi::class)
class LibraryService(
    private val fileService: FileService,
    private val bookService: BookService,
    private val repository: LibraryRepository,
    private val imageRepository: LibraryImageRepository
) {
    private val libDir: FileRef = fileService.getAppDirectory(AppDirectory.STATE).resolve("libraries")

    /**
     * Adds a book to the library by adding the book id. <br></br>
     * Will also save and <bold>WRITE</bold> the library to disk. <br></br>
     * Use this instead of manipulating the [Library.bookIds] directly.
     *
     * @param libraryId the id of the library
     * @param bookId the book to add
     */
    suspend fun addBook(libraryId: Uuid, bookId: Uuid) {
        repository.addBook(libraryId, bookId)
    }

    /**
     * Create a library based on the name and saves it to disk.
     *
     * @param name the desired name
     * @param imageBytes the bytes for the cover image
     * @return the created library
     */
    suspend fun createLibrary(name: String, imageBytes: ByteArray?): Library {
        Logger.i("creating library '$name'")
        val l = Library(Uuid.random(), name)
        if (imageBytes != null) {
            imageRepository.save(l.id, imageBytes)
        }
        repository.save(l.id, l)
        return l
    }

    /**
     * Loads a library from the repository by its name.
     *
     * @param id the id of the library
     * @return the deserialized library instance.
     */
    suspend fun loadLibrary(id: Uuid): Library {
        return repository.load(id) ?: throw IllegalArgumentException("library $id does not exist")
    }

    /**
     * Loads all libraries in the [.libDir].
     *
     * @return the list of libraries (mutable)
     */
    suspend fun loadLibraries(): List<Library> {
        Logger.d("loading libraries from '$libDir'")

        val libraries = repository.loadAll()

        Logger.i("loaded ${libraries.size} libraries")
        return libraries
    }

    /**
     * Obtains the bytes of the library image.
     * This opens the file and reads the bytes from the source
     *
     * @param library the library
     * @return the bytes of the image file or null if image is not set
     */
    suspend fun imageBytes(library: Library): ByteArray? {
        return imageRepository.load(library.id)
    }

    /**
     * Renames a library.
     *
     * @param libraryId the id of the library
     * @param newName the new name
     */
    suspend fun renameLibrary(libraryId: Uuid, newName: String) {
        val library = loadLibrary(libraryId)
        repository.save(libraryId, library.copy(name = newName))
    }

    /**
     * Deletes a library.
     *
     * @param libraryId the id of the library
     */
    suspend fun deleteLibrary(libraryId: Uuid) {
        repository.delete(libraryId)
        val imageFile = imageRepository.getFile(libraryId)
        if (fileService.exists(imageFile)) {
            imageRepository.delete(libraryId)
        }
    }

    /**
     * Generates a library image by merging 1 - 4 of the book covers.
     *
     * If the library contains no books, no cover can be created so `null` is returned.
     * The image is automatically saved and reread.
     *
     * @param library The library
     * @return the bytes of the image
     */
    suspend fun generateLibraryImage(library: Library): ImageBitmap? {
        val coverBytes = library.bookIds
            .mapNotNull { bookId -> bookService.readCoverBytes(bookId) }
            .take(4)
            .map { it.decodeToImageBitmap() }

        if (coverBytes.isEmpty()) {
            return null
        }
        return Dispatchers.Default {
            val combined = combineBitmaps(coverBytes)
            return@Default combined
        }
    }


}
