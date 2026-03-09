package dev.buecherregale.ebook_reader.core.dom

/**
 * Basic Node with a unique id
 * For later processing, the link anchor from the original document is preserved.
 *
 * For XML this could be the `id` attribute.
 * For Markdown this could be a heading id `(#heading-ids)`
 */
interface Node {
    val id: String
    val originalLinkAnchor: String?

    fun visit(visitor: (Node) -> Unit) {
        visitor(this)
    }
}

/** A node with no children (text, image, …). */
interface Leaf : Node

/** A node that owns child nodes. */
interface Branch : Node {
    val children: MutableList<Node>

    override fun visit(visitor: (Node) -> Unit) {
        visitor(this)
        children.forEach { it.visit(visitor) }
    }
}

/**
 * Marker for nodes that flow inline inside a paragraph
 * (e.g. Text, Link, Ruby) as opposed to block-level nodes.
 */
interface Inline : Node