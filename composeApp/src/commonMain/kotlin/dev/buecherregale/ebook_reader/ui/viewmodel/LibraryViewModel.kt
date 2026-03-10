package dev.buecherregale.ebook_reader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.core.domain.Library
import dev.buecherregale.ebook_reader.core.service.BookService
import dev.buecherregale.ebook_reader.core.service.LibraryService
import dev.buecherregale.ebook_reader.core.service.filesystem.FileRef
import dev.buecherregale.ebook_reader.ui.util.encodeToByteArray
import dev.buecherregale.ebook_reader.ui.util.generateLibraryImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class LibraryDetailViewModel(
    library: Library,
    private val libraryService: LibraryService,
    private val bookService: BookService
) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryDetailUiState(library))
    val uiState = _uiState.asStateFlow()

    fun loadBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val books = _uiState.value.library.bookIds.map { id ->
                bookService.readData(id)
            }

            _uiState.update { it.copy(books = books, isLoading = false) }
        }
    }

    fun importBook(path: String) {
        val ref = FileRef(path)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val book = bookService.importBook(ref)
            libraryService.addBook(_uiState.value.library.id, book.id)

            _uiState.update {
                it.copy(
                    books = _uiState.value.books.plus(book),
                    library = _uiState.value.library.copy(bookIds = _uiState.value.library.bookIds.plus(book.id)),
                    isLoading = false
                )
            }

            // separately update image
            if (_uiState.value.library.bookIds.size <= 4) {
                Logger.i { "library now contains ${_uiState.value.library.bookIds.size} books. Generating new cover image..." }
                // optimally pass the card image size or local pixel density here
                val imageBytes = generateLibraryImage(bookService, _uiState.value.library)
                if (imageBytes != null) {
                    libraryService.updateImage(_uiState.value.library.id, imageBytes.encodeToByteArray())
                }
            }
        }
    }
}

data class LibraryDetailUiState(
    var library: Library,
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false
)