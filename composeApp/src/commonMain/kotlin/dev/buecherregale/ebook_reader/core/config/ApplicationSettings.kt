package dev.buecherregale.ebook_reader.core.config

import androidx.compose.ui.text.intl.Locale
import dev.buecherregale.ebook_reader.core.language.LocaleSerializer
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Class for centralized state management.
 */
@Serializable
@OptIn(ExperimentalUuidApi::class)
class ApplicationSettings {
    internal var activeDictionaryIds: MutableMap<@Serializable(with = LocaleSerializer::class) Locale, Uuid> =
        mutableMapOf()
    var fontSize: Float = 20f
    var theme: AppThemeSetting = AppThemeSetting.SYSTEM
}
