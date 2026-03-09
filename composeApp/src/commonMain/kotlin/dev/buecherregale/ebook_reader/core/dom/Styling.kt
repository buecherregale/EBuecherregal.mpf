package dev.buecherregale.ebook_reader.core.dom

import kotlinx.serialization.Serializable

/** Class representing different supported TextStyles */
@Serializable
data class TextStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val code: Boolean = false,
) {
    companion object {
        val Default = TextStyle()
        val Bold = TextStyle(bold = true)
        val Italic = TextStyle(italic = true)
        val Code = TextStyle(code = true)
    }
}