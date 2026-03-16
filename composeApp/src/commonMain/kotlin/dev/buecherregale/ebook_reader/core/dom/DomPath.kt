package dev.buecherregale.ebook_reader.core.dom

class DomPath(
    private val componentIds: ArrayDeque<String> = ArrayDeque()
) {
    constructor(vararg componentIds: String) : this(ArrayDeque(componentIds.toList()))
    constructor(componentIds: Collection<String>) : this(ArrayDeque(componentIds.toList()))

    fun push(nodeId: String) = componentIds.addLast(nodeId)
    fun pop(): String = componentIds.removeLast()
    fun peek(): String = componentIds.last()

    fun toList(): List<String> = componentIds.toList()

    override fun toString(): String = componentIds.joinToString("/")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DomPath

        return componentIds == other.componentIds
    }

    override fun hashCode(): Int {
        return componentIds.hashCode()
    }
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