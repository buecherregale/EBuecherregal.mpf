package dev.buecherregale.ebook_reader.core.dom.xml

import dev.buecherregale.ebook_reader.core.dom.CURRENT_DOM_VERSION
import dev.buecherregale.ebook_reader.core.dom.Document
import dev.buecherregale.ebook_reader.core.dom.Node
import dev.buecherregale.ebook_reader.core.dom.Text
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Generates a unique, stable node ID. Replace with UUID if persistence matters. */
@OptIn(ExperimentalUuidApi::class)
internal fun nextId(): String = Uuid.generateV4().toString()

/**
 * Carries mutable state that tag handlers may need during a parse run.
 *
 * @param registry  The [TagHandlerRegistry] used to look up child handlers.
 * @param reader    The underlying [XmlReader] (position is advanced by handlers).
 */
data class ParseContext(
    val registry: TagHandlerRegistry,
    val reader: XmlReader,
)

/**
 * A [TagHandler] knows how to turn a single XML element (and its subtree)
 * into one or more [Node]s.
 *
 * Contract:
 * - When [handle] is called, the reader is positioned ON the START_ELEMENT
 *   event for the tag this handler owns.
 * - The handler MUST consume everything up to and including the matching
 *   END_ELEMENT before returning.
 * - Returns a (possibly empty) list of nodes to be added to the parent.
 */
interface TagHandler {
    /** The local XML tag names this handler claims, e.g. `setOf("p", "para")`. */
    val supportedTags: Set<String>

    fun handle(context: ParseContext): List<Node>
}

class TagHandlerRegistry {

    private val handlers = mutableMapOf<String, TagHandler>()

    /**
     * Register the given handler for all [TagHandler.supportedTags].
     * This overwrites existing handlers for the same tag.
     *
     * @param handler the tag handler to use
     */
    fun register(handler: TagHandler) {
        handler.supportedTags.forEach { tag ->
            handlers[tag.lowercase()] = handler
        }
    }

    fun handlerFor(tag: String): TagHandler? = handlers[tag.lowercase()]

    /** Returns a registry pre-populated with all built-in handlers. */
    companion object {
        fun default(): TagHandlerRegistry = TagHandlerRegistry().apply {
            register(ParagraphHandler())
            register(DivisionHandler())
            register(HeadingHandler())
            register(TextFormatHandler())
            register(LinkHandler())
            register(ImageHandler())
            register(ListHandler())
            register(ListItemHandler())
            register(RubyHandler())
            register(SpanHandler())
        }
    }
}

object XmlDomParser {

    /**
     * Parses an XML/HTML file from a reader to a [.Document].
     *
     * The root element of the XML is treated as the document container; its tag
     * name is ignored. If the root is `<html>`, only `<body>` children are
     * processed (others are skipped silently).
     */
    fun parse(
        reader: XmlReader,
        registry: TagHandlerRegistry = TagHandlerRegistry.default(),
        documentId: String = nextId(),
    ): Document {
        val document = Document(
            id = documentId,
            version = CURRENT_DOM_VERSION
        )
        val context = ParseContext(registry, reader)

        reader.skipToFirstStartElement()

        val rootTag = reader.localName.lowercase()

        if (rootTag == "html") {
            reader.parseChildren { tag ->
                if (tag == "body") {
                    reader.parseChildrenInto(document.children, context)
                } else {
                    reader.skipElement()
                }
            }
        } else {
            reader.parseChildrenInto(document.children, context)
        }

        return document
    }

    /**
     * Parses an XML/HTML string into a [.Document].
     *
     * The root element of [xml] is treated as the document container; its tag
     * name is ignored. If the root is `<html>`, only `<body>` children are
     * processed (others are skipped silently).
     */
    fun parse(
        xml: String,
        registry: TagHandlerRegistry = TagHandlerRegistry.default(),
        documentId: String = nextId(),
    ): Document {
        val reader = xmlStreaming.newReader(xml)
        return parse(reader, registry, documentId)
    }
}

/** Advances until the first START_ELEMENT, ignoring everything before it. */
internal fun XmlReader.skipToFirstStartElement() {
    while (hasNext()) {
        val type = next()
        if (type == EventType.START_ELEMENT) return
    }
}

/**
 * Calls [onTag] for every direct child START_ELEMENT.
 * Must be called when the reader is already inside the parent element
 * (meaning the parent's START_ELEMENT has been consumed).
 * Exits after consuming the parent's END_ELEMENT.
 *
 * CONTRACT: [onTag] MUST consume the element it is called for, including
 * its END_ELEMENT (matching the same contract as [TagHandler.handle]).
 * The loop only ever sees START_ELEMENT and END_ELEMENT events at the
 * direct-child level — it never double-counts events consumed by [onTag].
 */
internal fun XmlReader.parseChildren(onTag: XmlReader.(localName: String) -> Unit) {
    while (hasNext()) {
        when (next()) {
            EventType.START_ELEMENT -> onTag(localName)
            EventType.END_ELEMENT -> return
            else -> {}
        }
    }
}

/**
 * Parses children of the current element and appends resulting nodes to [target].
 *
 * CONTRACT: The reader must be positioned just after the parent's START_ELEMENT
 * (i.e. the START_ELEMENT itself has already been consumed).
 * Returns after consuming the parent's END_ELEMENT.
 *
 * Each child handler owns its END_ELEMENT — this function never double-counts
 * events that a handler has already consumed. There is no depth counter here
 * for exactly that reason: depth tracking and handler-owned END_ELEMENTs are
 * mutually exclusive strategies and must not be mixed.
 */
internal fun XmlReader.parseChildrenInto(
    target: MutableList<Node>,
    context: ParseContext,
) {
    while (hasNext()) {
        when (next()) {
            EventType.START_ELEMENT -> {
                val tag = localName.lowercase()
                val handler = context.registry.handlerFor(tag)
                if (handler != null) {
                    target.addAll(handler.handle(context))
                } else {
                    parseChildrenInto(target, context)
                }
            }

            EventType.TEXT, EventType.CDSECT -> {
                val text = text
                if (text.isNotEmpty()) {
                    target.add(
                        Text(
                            id = nextId(),
                            text = text
                        )
                    )
                }
            }

            EventType.END_ELEMENT -> return
            else -> {}
        }
    }
}

/**
 * Skips the current element entirely (consumes until its END_ELEMENT).
 * Must be called while positioned ON the START_ELEMENT.
 */
internal fun XmlReader.skipElement() {
    var depth = 1
    while (hasNext() && depth > 0) {
        when (next()) {
            EventType.START_ELEMENT -> depth++
            EventType.END_ELEMENT -> depth--
            else -> {}
        }
    }
}

/** Reads all attribute values as a map of localName → value. */
internal fun XmlReader.attributes(): Map<String, String> =
    (0 until attributeCount).associate {
        getAttributeLocalName(it) to getAttributeValue(it)
    }