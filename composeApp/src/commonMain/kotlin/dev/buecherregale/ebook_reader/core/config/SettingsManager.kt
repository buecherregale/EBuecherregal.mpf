package dev.buecherregale.ebook_reader.core.config

import androidx.compose.ui.text.intl.Locale
import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.service.DictionaryService
import dev.buecherregale.ebook_reader.core.service.filesystem.AppDirectory
import dev.buecherregale.ebook_reader.core.service.filesystem.FileRef
import dev.buecherregale.ebook_reader.core.service.filesystem.FileService
import dev.buecherregale.ebook_reader.core.util.JsonUtil
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Manager class for the application settings and state. <br></br>
 * Loads and saves settings, creates states from them.
 */
@OptIn(ExperimentalUuidApi::class)
class SettingsManager(
    private val fileService: FileService,
    private val jsonUtil: JsonUtil,
    private val dictionaryService: DictionaryService
) {

    private var settings: ApplicationSettings = ApplicationSettings()

    private var _state: ApplicationState = ApplicationState()
    val state: ApplicationState
        get() = _state

    /**
     * Loads the config file at [.configFile]. Will fail if the config file is not present. <br></br>
     * Also builds the initial state via [.buildState].
     */
    suspend fun load() {
        val json = fileService.read(configFile())
        settings = jsonUtil.deserialize(json)

        val loadedState = buildState()

        _state.setTheme(settings.theme)
        _state.setFontSize(settings.fontSize)

        _state.activeDictionaries.clear()
        _state.activeDictionaries.putAll(loadedState.activeDictionaries)

        Logger.i("loaded application settings:\n$json")
    }

    /**
     * Loads the config file at [.configFile] in a blocking manner. <br></br>
     * Only loads visual properties like theme and font size immediately.
     * Use [loadDictionaries] to load the rest asynchronously.
     */
    fun loadBlocking() {
        if (!fileService.existsBlocking(configFile())) {
            Logger.i("no existing settings found (blocking), using blank...")
            return
        }

        Logger.i { "loading existing settings (blocking)..." }
        val json = fileService.readBlocking(configFile())
        settings = jsonUtil.deserialize(json)

        _state.setTheme(settings.theme)
        _state.setFontSize(settings.fontSize)

        Logger.i("loaded application settings (blocking):\n$json")
    }

    /**
     * Loads dictionaries asynchronously. Should be called after [loadBlocking].
     */
    suspend fun loadDictionaries() {
        for ((lang, dictId) in settings.activeDictionaryIds) {
            try {
                dictionaryService.open(dictId).let {
                    _state.activeDictionaries[lang] = it
                }
            } catch (e: Exception) {
                Logger.w("Failed to load dictionary $dictId for language $lang", e)
            }
        }
    }

    /**
     * Checks if the config file exists. If not creates **BUT DOES NOT SAVE** a blank config and state.
     */
    suspend fun loadOrCreate() {
        if (!fileService.exists(configFile())) {
            Logger.i("no existing settings found, using blank...")
        } else {
            Logger.i { "loading existing settings..." }
            load()
        }
    }

    /**
     * Blocking version of [loadOrCreate].
     * Only loads basic settings. Call [loadDictionaries] afterward.
     */
    fun loadOrCreateBlocking() {
        if (!fileService.existsBlocking(configFile())) {
            Logger.i("no existing settings found, using blank...")
        } else {
            loadBlocking()
        }
    }

    /**
     * The reference to the config file.
     * Even if the file does not exist, the valid ref will be returned.
     *
     * @return the (theoretical) location of the config
     */
    fun configFile(): FileRef {
        return fileService.getAppDirectory(AppDirectory.CONFIG).resolve(CONFIG_FILENAME)
    }

    /**
     * Saves the config as JSON to the file at [.configFile].
     */
    suspend fun save() {
        Logger.d("saving settings at: ${configFile()}")
        val json: String = jsonUtil.serialize(settings)
        fileService.write(configFile(), json)
    }

    /**
     * Builds the initial state the config describes, OVERWRITING the existent state. <br></br>
     * Fails if no config is loaded.
     *
     * @return the initial state
     */
    private suspend fun buildState(): ApplicationState {
        val newState = ApplicationState()
        for ((lang, dictId) in settings.activeDictionaryIds) {
            try {
                dictionaryService.open(dictId).let {
                    newState.activeDictionaries[lang] = it
                }
            } catch (e: Exception) {
                Logger.w("Failed to load dictionary $dictId for language $lang", e)
            }
        }
        newState.setFontSize(settings.fontSize)
        newState.setTheme(settings.theme)

        return newState
    }

    /**
     * Set the active dictionary for its [dev.buecherregale.ebook_reader.core.domain.Dictionary.originalLanguage], updating the state as well.
     *
     * @param dictionaryId the id of the new dictionary
     */
    suspend fun activateDictionary(dictionaryId: Uuid) {
        val dictionary = dictionaryService.open(dictionaryId)
        _state.activeDictionaries[dictionary.originalLanguage] = dictionary
        settings.activeDictionaryIds[dictionary.originalLanguage] = dictionaryId
    }

    /**
     * Deactivates the active dictionary for the given language by removing it from the state and settings.
     * This is needed when the last dictionary for a language is deactivated.
     * Replacing an active dictionary can just be done by calling [activateDictionary].
     *
     * @param language the language for which to deactivate the dictionary
     */
    fun deactivateDictionary(language: Locale) {
        _state.activeDictionaries.remove(language)
        settings.activeDictionaryIds.remove(language)
    }

    fun setFontSize(size: Float) {
        _state.setFontSize(size)
        settings.fontSize = size
    }

    fun setTheme(theme: AppThemeSetting) {
        _state.setTheme(theme)
        settings.theme = theme
    }

    val theme: StateFlow<AppThemeSetting>
        get() = _state.theme

    companion object {
        const val CONFIG_FILENAME: String = "settings.json"
    }
}
