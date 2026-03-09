package dev.buecherregale.ebook_reader.core.service

import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.domain.Library
import dev.buecherregale.ebook_reader.core.repository.LibraryImageRepository
import dev.buecherregale.ebook_reader.core.repository.LibraryRepository
import dev.buecherregale.ebook_reader.core.service.filesystem.AppDirectory
import dev.buecherregale.ebook_reader.core.service.filesystem.FileRef
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Service-API to handle [Library] instances. As the domain classes are DTOs, use service classes to manipulate them.
 */
@OptIn(ExperimentalUuidApi::class)
class LibraryService(
    private val fileService: FileService,
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
    suspend fun addBook(libraryId: Uuid, bookId: String) {
        repository.addBook(libraryId, bookId)
    }

    /**
     * Create a library based on the name and saves it to disk.
     *
     * @param name the desired name
     * @return the created library
     */
    suspend fun createLibrary(name: String): Library {
        Logger.i("creating library '$name'")
        val l = Library(Uuid.random(), name)
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
        Logger.i("loading libraries from '${libDir.path}'")

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
     * Updates the library image written on disk.
     *
     * @param libraryId the id of the library to update the image for
     * @param imageBytes the byte array of the image
     */
    suspend fun updateImage(libraryId: Uuid, imageBytes: ByteArray) {
        Logger.i { "updating image for library $libraryId" }
        imageRepository.save(libraryId, imageBytes)
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
        Logger.i { "deleting library $libraryId" }
        repository.delete(libraryId)
        val imageFile = imageRepository.getFile(libraryId)
        if (fileService.exists(imageFile)) {
            Logger.i { "deleting cover image" }
            imageRepository.delete(libraryId)
        }
    }
}
