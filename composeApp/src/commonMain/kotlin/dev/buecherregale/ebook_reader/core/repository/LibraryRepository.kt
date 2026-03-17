@file:OptIn(ExperimentalUuidApi::class)

package dev.buecherregale.ebook_reader.core.repository

import dev.buecherregale.ebook_reader.core.domain.Library
import dev.buecherregale.ebookreader.sql.Libraries
import dev.buecherregale.ebookreader.sql.LibrariesQueries
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class LibraryImageRepository(
    delegate: FileRepository<Uuid>
) : FileBasedRepository<Uuid> by delegate

interface LibraryRepository : Repository<Uuid, Library> {
    suspend fun loadByName(name: String): Library?
    suspend fun addBook(libraryId: Uuid, bookId: String)
}

class LibrarySqlRepository(
    private val queries: LibrariesQueries
) : LibraryRepository {

    override suspend fun loadAll(): List<Library> {
        val rows = queries.selectLibrariesWithBooks().executeAsList()

        return rows
            .groupBy { it.library_id }
            .map { (libraryId, rowsForLibrary) ->
                Library(
                    id = Uuid.parse(libraryId),
                    name = rowsForLibrary.first().library_name,
                    bookIds = rowsForLibrary
                        .mapNotNull { it.book_id }
                )
            }
    }


    override suspend fun load(key: Uuid): Library? =
        queries.selectLibraryById(key.toString())
            .executeAsOneOrNull()
            ?.toLibrary(loadBookIds(key.toString()))

    override suspend fun loadByName(name: String): Library? =
        queries.selectLibraryByName(name)
            .executeAsOneOrNull().let { libraries ->
                return@loadByName libraries?.toLibrary(loadBookIds(libraries.id))
            }

    override suspend fun addBook(libraryId: Uuid, bookId: String) {
        queries.insertLibraryBook(libraryId.toString(), bookId)
    }

    override suspend fun save(key: Uuid, value: Library): Library {
        val exists = queries.selectLibraryById(key.toString())
            .executeAsOneOrNull() != null

        if (exists) {
            queries.updateLibraryName(
                name = value.name,
                id = key.toString()
            )
        } else {
            queries.insertLibrary(
                id = key.toString(),
                name = value.name
            )
        }

        // Replace books
        queries.deleteLibraryBooks(key.toString())
        value.bookIds.forEach { bookId ->
            queries.insertLibraryBook(
                library_id = key.toString(),
                book_id = bookId
            )
        }
        return load(key)!!
    }

    override suspend fun delete(key: Uuid) {
        queries.deleteLibraryBooks(key.toString())
        queries.deleteLibrary(key.toString())
    }

    private fun loadBookIds(libraryId: String): List<String> =
        queries.selectBookIdsForLibrary(libraryId)
            .executeAsList()
}

private fun Libraries.toLibrary(bookIds: List<String>): Library =
    Library(
        id = Uuid.parse(id),
        name = name,
        bookIds = bookIds.toMutableList() // TODO: make list immutable
    )


