package dev.buecherregale.ebook_reader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.buecherregale.ebook_reader.core.config.SettingsManager
import dev.buecherregale.ebook_reader.core.dom.Document
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.core.domain.Dictionary
import dev.buecherregale.ebook_reader.core.service.BookService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookViewModel(
    private val book: Book,
    private val bookService: BookService,
    settingsManager: SettingsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BookUiState(
            book = book,
            progress = book.progress,
            dictionary = settingsManager.state.activeDictionaries[book.metadata.language],
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadDom()
    }

    fun loadDom() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val dom = bookService.open(book.id)
            _uiState.value = _uiState.value.copy(dom = dom, isLoading = false)
        }
    }

    fun updateProgress(progress: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(progress = progress, book = book.copy(progress = progress))
            bookService.updateProgress(book, progress)
        }
    }
}

data class BookUiState(
    val book: Book,
    var dom: Document? = null,
    var dictionary: Dictionary? = null,
    var isLoading: Boolean = false,
    var progress: Double = 0.0,
)