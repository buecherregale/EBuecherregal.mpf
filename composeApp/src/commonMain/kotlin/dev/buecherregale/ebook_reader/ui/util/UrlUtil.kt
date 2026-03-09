package dev.buecherregale.ebook_reader.ui.util

object UrlUtil {
    const val BOOK_RESOURCE_PROTOCOL = "book_resource"
    const val BOOK_LINK_PROTOCOL = "book_link"

    fun getUrlProtocol(url: String): String {
        return url.substringBefore("://")
    }

    fun getUrlPath(url: String): String {
        return url.substringAfter("://")
            .substringBefore("?")
    }
}