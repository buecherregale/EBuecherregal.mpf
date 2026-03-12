package dev.buecherregale.ebook_reader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.buecherregale.ebook_reader.core.dom.Document
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.core.service.BookService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookViewModel(
    private val book: Book,
    private val bookService: BookService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookUiState(book))
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
}

data class BookUiState(
    val book: Book,
    var dom: Document? = null,
    var isLoading: Boolean = false
)