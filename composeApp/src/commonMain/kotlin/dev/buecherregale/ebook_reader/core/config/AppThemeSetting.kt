package dev.buecherregale.ebook_reader.core.config

import kotlinx.serialization.Serializable

@Serializable
enum class AppThemeSetting {
    LIGHT,
    DARK,
    SYSTEM,
    DYNAMIC, // android only
}
