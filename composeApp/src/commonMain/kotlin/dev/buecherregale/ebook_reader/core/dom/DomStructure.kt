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

    /**
     * Traverse the graph this Node is a part of.
     * Calls [visitor] on every node.
     *
     * @see [Branch.traverse]
     */
    fun traverse(visitor: (Node) -> Unit) {
        visitor(this)
    }
}

/** A node with no children (text, image, …). */
interface Leaf : Node

/** A node that owns child nodes. */
interface Branch : Node {
    val children: MutableList<Node>

    /**
     * Traverse the graph this Node is a part of.
     *
     * Calls [visitor] on every node.
     *
     * @see [Node.traverse]
     */
    override fun traverse(visitor: (Node) -> Unit) {
        visitor(this)
        children.forEach { it.traverse(visitor) }
    }
}

/**
 * Marker for nodes that flow inline inside a paragraph
 * (e.g. Text, Link, Ruby) as opposed to block-level nodes.
 */
interface Inline : Node