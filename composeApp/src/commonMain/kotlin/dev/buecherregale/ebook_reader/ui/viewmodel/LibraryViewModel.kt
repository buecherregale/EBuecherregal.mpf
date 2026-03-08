package dev.buecherregale.ebook_reader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.buecherregale.ebook_reader.core.domain.Library
import dev.buecherregale.ebook_reader.core.service.LibraryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class LibraryViewModel(
    private val libraryService: LibraryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadLibraries()
    }

    fun loadLibraries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val libraries = libraryService.loadLibraries()
            _uiState.update { it.copy(libraries = libraries.toMutableList(), isLoading = false) }
        }
    }

    fun createLibrary(name: String, imageBytes: ByteArray?) {
        viewModelScope.launch {
            libraryService.createLibrary(name, imageBytes)
            loadLibraries()
        }
    }

    fun renameLibrary(libraryId: Uuid, newName: String) {
        viewModelScope.launch {
            libraryService.renameLibrary(libraryId, newName)
            loadLibraries()
        }
    }

    fun deleteLibrary(libraryId: Uuid) {
        viewModelScope.launch {
            libraryService.deleteLibrary(libraryId)
            loadLibraries()
        }
    }
}

data class LibraryUiState(
    val libraries: MutableList<Library> = mutableListOf(),
    val isLoading: Boolean = false
)
