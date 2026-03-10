package dev.buecherregale.ebook_reader.core.dom

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Not an updated DOM, but a brand-new DOM. Better, nicer, cooler */
const val CURRENT_DOM_VERSION = 1

interface Migration {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(document: Document)
}

@Serializable
data class Document(
    override val id: String,
    override val children: MutableList<Node> = mutableListOf(),
    @Transient
    override val originalLinkAnchor: String? = null,
    val version: Int = CURRENT_DOM_VERSION,
) : Branch

/**
 * A Chapter is just a simple branching node.
 * This class is adapted to EPubs, keeping some of the original information.
 *
 * A Chapter should lead to a new Page displayed.
 *
 * @param id                    the new id for the node
 * @param children              the child nodes
 * @param originalZipPath       the path to the chapters xhtml in the zip
 * @param originalLinkAnchor    identifier for a link anchor (targeted by things like `<a>`) = XML id
 */
@Serializable
data class Chapter(
    override val id: String,
    override val children: MutableList<Node> = mutableListOf(),
    @Transient
    override val originalLinkAnchor: String? = null,
    @Transient
    val originalZipPath: String = "",
) : Branch


@Serializable
data class Division(
    override val id: String,
    override val children: MutableList<Node> = mutableListOf(),
    @Transient
    override val originalLinkAnchor: String? = null,
) : Branch

@Serializable
data class Paragraph(
    override val id: String,
    override val children: MutableList<Node> = mutableListOf(),
    @Transient
    override val originalLinkAnchor: String? = null,
) : Branch

/**
 * Heading level 1 to 6. Children are inline nodes so that formatted
 * headings like `<h1><em>Title</em></h1>` are representable.
 */
@Serializable
data class Heading(
    override val id: String,
    override val children: MutableList<Node> = mutableListOf(),
    @Transient
    override val originalLinkAnchor: String? = null,
    val level: Int,
) : Branch

/**
 * Ordered or unordered list. Each direct child should be a [ListItem].
 * Renamed from `List` to avoid clashing with kotlin.collections.List.
 */
@Serializable
data class ListBlock(
    override val id: String,
    override val children: MutableList<Node> = mutableListOf(),
    @Transient
    override val originalLinkAnchor: String? = null,
    val ordered: Boolean = false,
) : Branch

@Serializable
data class ListItem(
    override val id: String,
    override val children: MutableList<Node> = mutableListOf(),
    @Transient
    override val originalLinkAnchor: String? = null,
) : Branch

@Serializable
data class ImageBlock(
    override val id: String,
    @Transient
    override val originalLinkAnchor: String? = null,
    val image: Image,
    /** Caption can contain inline formatting, so it's a list of nodes. */
    val caption: MutableList<Node> = mutableListOf(),
) : Leaf

@Serializable
data class Text(
    override val id: String,
    @Transient
    override val originalLinkAnchor: String? = null,
    val text: String,
    val style: TextStyle = TextStyle.Default
) : Leaf, Inline

/**
 * A hyperlink. Children are inline nodes (formatted link text is valid).
 */
@Serializable
data class Link(
    override val id: String,
    override val children: MutableList<Node> = mutableListOf(),
    @Transient
    override val originalLinkAnchor: String? = null,
    var target: String,
) : Branch, Inline

@Serializable
data class Image(
    override val id: String,
    @Transient
    override val originalLinkAnchor: String? = null,
    var src: String,
    val alt: String = "",
) : Leaf

/**
 * Ruby annotation. Both base and annotation can have styled text runs.
 */
@Serializable
data class Ruby(
    override val id: String,
    @Transient
    override val originalLinkAnchor: String? = null,
    val baseText: MutableList<Text> = mutableListOf(),
    val annotationText: MutableList<Text> = mutableListOf(),
) : Leaf, Inline