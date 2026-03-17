package dev.buecherregale.ebook_reader.core.dom.md

import dev.buecherregale.ebook_reader.core.dom.Inline
import dev.buecherregale.ebook_reader.core.dom.Link
import dev.buecherregale.ebook_reader.core.dom.Text
import dev.buecherregale.ebook_reader.core.dom.TextStyle

interface InlineHandler {
    val priority: Int get() = 0

    fun findMatch(text: String): InlineMatch?

    fun parse(text: String, match: InlineMatch, context: ParseContext): List<Inline>
}

data class InlineMatch(
    val start: Int,
    val end: Int,               // exclusive
    val groups: List<String>,
)

internal fun Regex.firstInlineMatch(text: String): InlineMatch? {
    val m = find(text) ?: return null
    return InlineMatch(m.range.first, m.range.last + 1, m.groupValues)
}

class BoldItalicInlineHandler : InlineHandler {
    override val priority = 30
    private val regex = Regex("""\*{3}(.+?)\*{3}""")

    override fun findMatch(text: String) = regex.firstInlineMatch(text)

    override fun parse(text: String, match: InlineMatch, context: ParseContext) = listOf(
        Text(id = nextId(), text = match.groups[1], style = TextStyle(bold = true, italic = true)),
    )
}

class BoldInlineHandler : InlineHandler {
    override val priority = 20
    private val regex = Regex("""\*\*(.+?)\*\*|__(.+?)__""")

    override fun findMatch(text: String) = regex.firstInlineMatch(text)

    override fun parse(text: String, match: InlineMatch, context: ParseContext): List<Inline> {
        val content = match.groups[1].ifEmpty { match.groups[2] }
        return listOf(Text(id = nextId(), text = content, style = TextStyle(bold = true)))
    }
}

class ItalicInlineHandler : InlineHandler {
    override val priority = 19

    private val regex = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)|(?<!_)_(?!_)(.+?)(?<!_)_(?!_)""")

    override fun findMatch(text: String) = regex.firstInlineMatch(text)

    override fun parse(text: String, match: InlineMatch, context: ParseContext): List<Inline> {
        val content = match.groups[1].ifEmpty { match.groups[2] }
        return listOf(Text(id = nextId(), text = content, style = TextStyle(italic = true)))
    }
}

class StrikethroughInlineHandler : InlineHandler {
    override val priority = 18
    private val regex = Regex("""~~(.+?)~~""")

    override fun findMatch(text: String) = regex.firstInlineMatch(text)

    override fun parse(text: String, match: InlineMatch, context: ParseContext) = listOf(
        Text(id = nextId(), text = match.groups[1], style = TextStyle(strikethrough = true)),
    )
}

open class InlineCodeHandler : InlineHandler {
    override val priority = 25
    private val regex = Regex("""`(.+?)`""")

    override fun findMatch(text: String) = regex.firstInlineMatch(text)

    override fun parse(text: String, match: InlineMatch, context: ParseContext) =
        listOf(Text(id = nextId(), text = match.groups[1]))
}

open class InlineImageHandler : InlineHandler {
    override val priority = 31
    private val regex = Regex("""!\[([^]]*)]\(([^)]+)\)""")

    override fun findMatch(text: String) = regex.firstInlineMatch(text)

    override fun parse(text: String, match: InlineMatch, context: ParseContext): List<Inline> =
        emptyList()
}

class LinkInlineHandler : InlineHandler {
    override val priority = 29
    private val regex = Regex("""(?<!!)\[([^]]+)]\(([^)]+)\)""")

    override fun findMatch(text: String) = regex.firstInlineMatch(text)

    override fun parse(text: String, match: InlineMatch, context: ParseContext): List<Inline> {
        val label = match.groups[1]
        val href = match.groups[2]
        return listOf(
            Link(
                id = nextId(),
                target = href,
                children = mutableListOf(Text(id = nextId(), text = label)),
            ),
        )
    }
}

class InlineHandlerRegistry {
    private val handlers = mutableListOf<InlineHandler>()

    fun register(handler: InlineHandler) {
        handlers += handler
    }

    fun registerAll(handlers: List<InlineHandler>) {
        this.handlers.addAll(handlers)
    }

    fun collect(): List<InlineHandler> {
        return handlers.sortedByDescending { it.priority }
    }

    companion object {
        fun default(): InlineHandlerRegistry = InlineHandlerRegistry().apply {
            registerAll(
                listOf(
                    InlineImageHandler(),   // before link
                    BoldItalicInlineHandler(),
                    InlineCodeHandler(),    // before bold/italic
                    BoldInlineHandler(),
                    ItalicInlineHandler(),
                    StrikethroughInlineHandler(),
                    LinkInlineHandler(),
                )
            )
        }
    }
}