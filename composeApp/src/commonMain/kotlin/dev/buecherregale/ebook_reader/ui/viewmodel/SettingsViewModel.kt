package dev.buecherregale.ebook_reader.ui.viewmodel

import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.config.AppThemeSetting
import dev.buecherregale.ebook_reader.core.config.SettingsManager
import dev.buecherregale.ebook_reader.core.domain.DictionaryMetadata
import dev.buecherregale.ebook_reader.core.service.DictionaryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val dictionaryService: DictionaryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val supported = dictionaryService.listSupportedDictionaryNames()
            val downloaded = dictionaryService.listDownloadedDictionaryMetadata()

            val activeIds = settingsManager.state.activeDictionaries.entries.associate {
                it.key to it.value.id
            }

            val fontSize = settingsManager.state.fontSize.value
            val theme = settingsManager.state.theme.value

            _uiState.update {
                it.copy(
                    supportedDictionaries = supported,
                    downloadedDictionaries = downloaded,
                    activeDictionaryIds = activeIds,
                    fontSize = fontSize,
                    theme = theme,
                    isLoading = false
                )
            }
        }
    }

    fun downloadDictionary(name: String, languageTag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                dictionaryService.download(name, Locale(languageTag))
                val downloaded = dictionaryService.listDownloadedDictionaryMetadata()
                _uiState.update { it.copy(downloadedDictionaries = downloaded, isLoading = false) }
            } catch (e: Exception) {
                Logger.e(e) { "failed to download dictionary" }
                _uiState.update { it.copy(error = e.message ?: "Unknown error", isLoading = false) }
            }
        }
    }

    fun setActiveDictionary(originalLanguage: Locale, dictionaryId: Uuid) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                settingsManager.activateDictionary(dictionaryId)
                val activeIds = _uiState.value.activeDictionaryIds.toMutableMap()
                activeIds[originalLanguage] = dictionaryId
                _uiState.update { it.copy(activeDictionaryIds = activeIds) }
            } catch (e: Exception) {
                Logger.e(e) { "failed to activate dictionary" }
                _uiState.update { it.copy(error = e.message ?: "Failed to set dictionary") }
            }
        }
    }

    fun deleteDictionary(dictionaryId: Uuid) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                dictionaryService.delete(dictionaryId)
                val downloaded = dictionaryService.listDownloadedDictionaryMetadata()

                val activeIds = _uiState.value.activeDictionaryIds.toMutableMap()
                val entryToRemove = activeIds.entries.find { it.value == dictionaryId }
                if (entryToRemove != null) {
                    activeIds.remove(entryToRemove.key)
                    settingsManager.deactivateDictionary(entryToRemove.key)
                    // save the settings immediately (the active dictionary cannot be recovered anyway)
                    settingsManager.save()
                }

                _uiState.update {
                    it.copy(
                        downloadedDictionaries = downloaded,
                        activeDictionaryIds = activeIds,
                        message = "Dictionary deleted"
                    )
                }
            } catch (e: Exception) {
                Logger.e(e) { "failed to delete dictionary" }
                _uiState.update { it.copy(error = e.message ?: "Failed to delete dictionary") }
            }
        }
    }

    fun setFontSize(size: Float) {
        _uiState.update { it.copy(fontSize = size) }
    }

    fun setTheme(theme: AppThemeSetting) {
        _uiState.update { it.copy(theme = theme) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, message = null) }
            try {
                settingsManager.setFontSize(_uiState.value.fontSize)
                settingsManager.setTheme(_uiState.value.theme)
                settingsManager.save()
                _uiState.update { it.copy(isSaving = false, message = "Settings saved successfully") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save settings") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

@OptIn(ExperimentalUuidApi::class)
data class SettingsUiState(
    val supportedDictionaries: List<String> = emptyList(),
    val downloadedDictionaries: List<DictionaryMetadata> = emptyList(),
    val activeDictionaryIds: Map<Locale, Uuid> = emptyMap(),
    val fontSize: Float = 20f,
    val theme: AppThemeSetting = AppThemeSetting.SYSTEM,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
