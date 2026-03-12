package dev.buecherregale.ebook_reader.core.dom

data class DomPath(
    val nodeIds: List<String>
) {
    override fun toString(): String = nodeIds.joinToString("/")
}

interface DomUrl {

    val path: DomPath
    override fun toString(): String

    data class Link(
        override val path: DomPath,
    ) : DomUrl {
        override fun toString(): String = "$LINK_PROTOCOL://$path"
    }

    data class Resource(
        override val path: DomPath,
    ) : DomUrl {
        override fun toString(): String = "$RESOURCE_PROTOCOL://$path"
    }

    companion object {
        const val RESOURCE_PROTOCOL = "dom_resource"
        const val LINK_PROTOCOL = "dom_link"

        fun parse(url: String): DomUrl? {
            val protocol = url.substringBefore("://")
            return when (protocol) {
                RESOURCE_PROTOCOL -> Resource(DomPath(url.substringAfter("://").split("/")))
                LINK_PROTOCOL -> Link(DomPath(url.substringAfter("://").split("/")))
                else -> null
            }
        }
    }
}