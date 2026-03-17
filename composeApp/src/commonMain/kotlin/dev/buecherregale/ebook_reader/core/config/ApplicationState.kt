package dev.buecherregale.ebook_reader.core.config

import androidx.compose.ui.text.intl.Locale
import dev.buecherregale.ebook_reader.core.domain.Dictionary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApplicationState {
    /**
     * Active dictionaries by the language they translate **from**.
     *
     * @see Dictionary.originalLanguage
     */
    var activeDictionaries: MutableMap<Locale, Dictionary> = mutableMapOf()

    private val _fontSize = MutableStateFlow(20f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    fun setFontSize(size: Float) {
        _fontSize.value = size
    }

    private val _theme = MutableStateFlow(AppThemeSetting.SYSTEM)
    val theme: StateFlow<AppThemeSetting> = _theme.asStateFlow()

    fun setTheme(theme: AppThemeSetting) {
        _theme.value = theme
    }
}
