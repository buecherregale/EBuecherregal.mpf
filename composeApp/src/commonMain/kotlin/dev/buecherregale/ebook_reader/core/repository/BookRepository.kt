@file:OptIn(ExperimentalUuidApi::class)

package dev.buecherregale.ebook_reader.core.repository

import androidx.compose.ui.text.intl.Locale
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.core.domain.BookMetadata
import dev.buecherregale.ebookreader.sql.Books
import dev.buecherregale.ebookreader.sql.BooksQueries
import kotlin.uuid.ExperimentalUuidApi

/** repository for the book files themselves (e.g. epub) */
class BookFileRepository(
    delegate: FileRepository<String>
) : FileBasedRepository<String> by delegate

class BookCoverRepository(
    delegate: FileRepository<String>
) : FileBasedRepository<String> by delegate

interface BookRepository : Repository<String, Book>

class BookSqlRepository(
    private val queries: BooksQueries
) : BookRepository {
    override suspend fun loadAll(): List<Book> {
        return queries.selectAll().executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun load(key: String): Book? {
        return queries.selectById(key)
            .executeAsOneOrNull()
            .let { it?.toDomain() }
    }

    override suspend fun save(
        key: String,
        value: Book
    ): Book {
        queries.upsert(
            id = key,
            progress = value.progress,

            title = value.metadata.title,
            author = value.metadata.author,
            isbn = value.metadata.isbn,
            language = value.metadata.language.toLanguageTag()
        )
        return value
    }

    override suspend fun delete(key: String) {
        queries.deleteById(key)
    }

}

fun Books.toDomain(): Book =
    Book(
        id = id,
        progress = progress,
        metadata = BookMetadata(
            title = title,
            author = author,
            isbn = isbn,
            language = Locale(language)
        )
    )
