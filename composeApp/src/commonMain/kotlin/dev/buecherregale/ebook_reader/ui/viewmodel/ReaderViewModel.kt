package dev.buecherregale.ebook_reader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.buecherregale.ebook_reader.core.config.SettingsManager
import dev.buecherregale.ebook_reader.core.dom.DomDocument
import dev.buecherregale.ebook_reader.core.dom.LinkTarget
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.core.domain.Dictionary
import dev.buecherregale.ebook_reader.core.service.BookService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ReaderViewModel(
    private val book: Book,
    private val bookService: BookService,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState(book = book, isLoading = true))
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun initState() {
        _uiState.update {
            it.copy(
                title = book.metadata.title,
                progress = if (it.progress < 0) book.progress else it.progress,
            )
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val dom = bookService.open(book.id)
            val cIdx =
                if (_uiState.value.chapterIdx == 0 && book.progress != 0.0)
                    (book.progress * dom.chapter.lastIndex).roundToInt()
                else _uiState.value.chapterIdx

            val language = book.metadata.language
            val dictionary = settingsManager.state.activeDictionaries[language]
            _uiState.update {
                it.copy(
                    chapterIdx = cIdx,
                    isLoading = false,
                    dom = dom,
                    dictionary = dictionary
                )
            }
        }
    }

    fun updateProgress() {
        val newProgress = uiState.value.chapterIdx.toDouble() / uiState.value.dom!!.chapter.lastIndex
        viewModelScope.launch {
            bookService.updateProgress(book, newProgress)
            _uiState.update { it.copy(progress = newProgress) }
        }
    }

    fun nextChapter() {
        _uiState.update { state ->
            state.dom?.let { dom ->
                if (state.chapterIdx < dom.chapter.lastIndex) {
                    state.copy(
                        chapterIdx = state.chapterIdx + 1,
                    )
                } else state
            } ?: state
        }
        updateProgress()
    }

    fun previousChapter() {
        _uiState.update { state ->
            if (state.chapterIdx > 0) {
                state.copy(
                    chapterIdx = state.chapterIdx - 1,
                )
            } else state
        }
        updateProgress()
    }

    fun navigateToLink(target: LinkTarget) {
        if (target is LinkTarget.Internal) {
            val dom = uiState.value.dom ?: return

            if (target.chapterId != null) {
                val chapterIndex = dom.chapter.indexOfFirst { it.id == target.chapterId }
                if (chapterIndex != -1) {
                    _uiState.update { it.copy(chapterIdx = chapterIndex) }
                    updateProgress()
                }
            }
            // TODO: scrolling to specific node within chapter
        }
    }
}

data class ReaderUiState(
    val title: String = "",
    val progress: Double = -1.0, // do not use the book progress as book is immutable and the instance is not updated when progress changes
    val isLoading: Boolean = false,
    val book: Book,
    var dom: DomDocument? = null,
    var chapterIdx: Int = 0,
    val dictionary: Dictionary? = null
)
