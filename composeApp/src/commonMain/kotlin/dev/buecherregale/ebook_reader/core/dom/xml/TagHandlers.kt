package dev.buecherregale.ebook_reader.core.dom.xml

import dev.buecherregale.ebook_reader.core.dom.*

internal class ParagraphHandler : TagHandler {
    override val supportedTags = setOf("p")

    override fun handle(context: ParseContext): List<Node> {
        val node = Paragraph(
            id = nextId(),
            originalLinkAnchor = context.reader.attributes()["id"]
        )
        context.reader.parseChildrenInto(node.children, context)
        return listOf(node)
    }
}

internal class DivisionHandler : TagHandler {
    override val supportedTags = setOf("div", "section", "article", "aside", "main", "header", "footer", "blockquote")

    override fun handle(context: ParseContext): List<Node> {
        val node = Division(id = nextId())
        context.reader.parseChildrenInto(node.children, context)
        return listOf(node)
    }
}

internal class HeadingHandler : TagHandler {
    override val supportedTags = setOf("h1", "h2", "h3", "h4", "h5", "h6")

    override fun handle(context: ParseContext): List<Node> {
        val level = context.reader.localName.removePrefix("h").toIntOrNull() ?: 1
        val node = Heading(
            id = nextId(),
            level = level,
            originalLinkAnchor = context.reader.attributes()["id"]
        )
        context.reader.parseChildrenInto(node.children, context)
        return listOf(node)
    }
}

/**
 * Handles all simple inline formatting tags. Rather than creating a wrapper
 * node, it parses children and applies the relevant [TextStyle] flag to any
 * [Text] leaves it finds (recursively).
 *
 * This keeps the DOM flat: `<p><strong>hello</strong></p>` becomes
 * `Paragraph → Text("hello", style=TextStyle(bold=true))`.
 */
internal class TextFormatHandler : TagHandler {
    override val supportedTags = setOf("strong", "b", "em", "i", "u", "s", "del", "code", "mark", "small", "sub", "sup")

    override fun handle(context: ParseContext): List<Node> {
        val tag = context.reader.localName.lowercase()
        val children = mutableListOf<Node>()
        context.reader.parseChildrenInto(children, context)

        return children.map { applyStyle(it, tag) }
    }

    private fun applyStyle(
        node: Node,
        tag: String
    ): Node = when (node) {
        is Text -> node.copy(style = node.style.withTag(tag))
        is Paragraph -> node.copy(children = node.children.map {
            applyStyle(
                it,
                tag
            )
        }.toMutableList())

        is Link -> node.copy(children = node.children.map {
            applyStyle(
                it,
                tag
            )
        }.toMutableList())

        else -> node
    }

    private fun TextStyle.withTag(tag: String) = when (tag) {
        "strong", "b" -> copy(bold = true)
        "em", "i" -> copy(italic = true)
        "u" -> copy(underline = true)
        "s", "del" -> copy(strikethrough = true)
        "code" -> copy(code = true)
        else -> this
    }
}

internal class SpanHandler : TagHandler {
    override val supportedTags = setOf("span", "time", "abbr", "cite", "q")

    override fun handle(context: ParseContext): List<Node> {
        val children = mutableListOf<Node>()
        context.reader.parseChildrenInto(children, context)
        return children
    }
}

internal class LinkHandler : TagHandler {
    override val supportedTags = setOf("a")

    override fun handle(context: ParseContext): List<Node> {
        val attrs = context.reader.attributes()
        val href = attrs["href"] ?: attrs["src"] ?: "#"
        val node = Link(
            id = nextId(),
            target = href,
            originalLinkAnchor = attrs["id"]
        )
        context.reader.parseChildrenInto(node.children, context)
        return listOf(node)
    }
}

internal class ImageHandler : TagHandler {
    override val supportedTags =
        setOf("img", "figure", "image") // for some fucking reason the titlepage contains an image with `image` tag

    override fun handle(context: ParseContext): List<Node> {
        return when (context.reader.localName.lowercase()) {
            "img" -> listOf(handleImg(context))
            "figure" -> listOf(handleFigure(context))
            "image" -> listOf(handleImage(context))
            else -> emptyList()
        }
    }
    
    private fun handleImage(context: ParseContext): Node {
        val attrs = context.reader.attributes()
        val image = Image(
            id = nextId(),
            src = attrs.entries.find { it.key.startsWith("href") }?.value ?: "",
            alt = attrs["alt"] ?: "",
            originalLinkAnchor = attrs["id"],
        )
        context.reader.skipElement()
        return image
    }

    private fun handleImg(context: ParseContext): Node {
        val attrs = context.reader.attributes()
        val image = Image(
            id = nextId(),
            src = attrs["src"] ?: "",
            alt = attrs["alt"] ?: "",
            originalLinkAnchor = attrs["id"],
        )
        context.reader.skipElement()
        return image
    }

    private fun handleFigure(context: ParseContext): Node {
        var image: Image? = null
        val caption = mutableListOf<Node>()

        context.reader.parseChildren { tag ->
            when (tag) {
                "img" -> {
                    val attrs = context.reader.attributes()
                    image = Image(
                        id = nextId(),
                        src = attrs["src"] ?: "",
                        alt = attrs["alt"] ?: "",
                        originalLinkAnchor = attrs["id"],
                    )
                    skipElement()
                }

                "figcaption" -> {
                    parseChildrenInto(caption, context)
                }

                else -> skipElement()
            }
        }

        return if (image != null) {
            ImageBlock(
                id = nextId(),
                image = image,
                caption = caption,
                originalLinkAnchor = context.reader.attributes()["id"]
            )
        } else {
            // Degenerate figure with no image, wrap in Division
            Division(
                id = nextId(),
                children = caption,
                originalLinkAnchor = context.reader.attributes()["id"]
            )
        }
    }
}

internal class ListHandler : TagHandler {
    override val supportedTags = setOf("ul", "ol")

    override fun handle(context: ParseContext): List<Node> {
        val ordered = context.reader.localName.lowercase() == "ol"
        val node = ListBlock(
            id = nextId(),
            ordered = ordered,
            originalLinkAnchor = context.reader.attributes()["id"]
        )
        context.reader.parseChildrenInto(node.children, context)
        return listOf(node)
    }
}

internal class ListItemHandler : TagHandler {
    override val supportedTags = setOf("li")

    override fun handle(context: ParseContext): List<Node> {
        val node = ListItem(
            id = nextId(),
            originalLinkAnchor = context.reader.attributes()["id"]
        )
        context.reader.parseChildrenInto(node.children, context)
        return listOf(node)
    }
}

internal class RubyHandler : TagHandler {

    override val supportedTags: Set<String> = setOf("ruby")

    override fun handle(context: ParseContext): List<Node> {
        val reader = context.reader

        val ruby = Ruby(
            id = nextId()
        )

        var pendingRpOpen: Text? = null
        var pendingRpClose: Text? = null

        reader.parseChildren { tag ->
            when (tag) {

                "rb" -> {
                    val texts: MutableList<Node> = mutableListOf()
                    reader.parseChildrenInto(texts, context)
                    ruby.baseText.addAll(texts.filterIsInstance<Text>())
                }

                "rt" -> {
                    val texts = mutableListOf<Node>()
                    reader.parseChildrenInto(texts, context)

                    val annotationText = texts
                        .filterIsInstance<Text>()
                        .joinToString("") { it.text }

                    ruby.annotationText.add(
                        RubyAnnotation(
                            id = nextId(),
                            annotationText = Text(
                                id = nextId(),
                                text = annotationText
                            ),
                            parenthesisOpen = pendingRpOpen,
                            parenthesisClosed = pendingRpClose
                        )
                    )

                    pendingRpOpen = null
                    pendingRpClose = null
                }

                "rp" -> {
                    val texts = mutableListOf<Node>()
                    reader.parseChildrenInto(texts, context)

                    val text = texts
                        .filterIsInstance<Text>()
                        .joinToString("") { it.text }

                    val rpText = Text(
                        id = nextId(),
                        text = text
                    )

                    // Assign as open or close depending on state
                    if (pendingRpOpen == null) {
                        pendingRpOpen = rpText
                    } else {
                        pendingRpClose = rpText
                    }
                }

                else -> {
                    val handler = context.registry.handlerFor(tag)
                    if (handler != null) {
                        val nodes = handler.handle(context)
                        ruby.baseText.addAll(nodes.filterIsInstance<Text>())
                    } else {
                        val texts = mutableListOf<Node>()
                        reader.parseChildrenInto(texts, context)
                        ruby.baseText.addAll(texts.filterIsInstance<Text>())
                    }
                }
            }
        }

        return listOf(ruby)
    }
}