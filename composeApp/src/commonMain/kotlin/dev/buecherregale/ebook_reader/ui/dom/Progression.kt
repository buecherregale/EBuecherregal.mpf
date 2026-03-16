package dev.buecherregale.ebook_reader.ui.dom

import dev.buecherregale.ebook_reader.core.dom.*

/**
 * Precomputed index of all leaves in document order, used to convert
 * between [DomPath] and a [0.0, 1.0] progress fraction.
 *
 * Build once via [Document.buildContentIndex].
 */
class ContentIndex internal constructor(
    private val entries: List<Entry>,
    private val totalWeight: Double,
) {
    data class Entry(val path: DomPath, val weight: Double, val cumulativeWeightBefore: Double)

    // Precomputed for O(1) lookup when a page settles
    private val fractionByLeafId: Map<String, Double> = entries.associate { entry ->
        entry.path.peek() to (entry.cumulativeWeightBefore / totalWeight).coerceIn(0.0, 1.0)
    }

    fun fractionAt(path: DomPath): Double {
        if (totalWeight == 0.0) return 0.0
        val entry = entries.firstOrNull { it.path == path } ?: return 0.0
        return (entry.cumulativeWeightBefore / totalWeight).coerceIn(0.0, 1.0)
    }

    /** Returns the progress fraction for the leaf with the given [leafId]. */
    fun fractionForLeafId(leafId: String): Double? = fractionByLeafId[leafId]

    fun pathAtFraction(fraction: Double): DomPath {
        if (entries.isEmpty()) return DomPath()
        val target = (fraction * totalWeight).coerceIn(0.0, totalWeight)
        return entries
            .lastOrNull { it.cumulativeWeightBefore <= target }
            ?.path
            ?: entries.first().path
    }
}

fun Page.firstLeafId(): String? {
    fun findLeaf(node: Node): String? = when (node) {
        is Leaf -> node.id
        is Branch -> node.children.firstNotNullOfOrNull { findLeaf(it) }
    }
    return roots.firstNotNullOfOrNull { findLeaf(it) }
}

/**
 * Returns the index of the first page that contains a [Leaf] with [leafId],
 * or `-1` if not found.
 */
fun List<Page>.pageIndexForLeafId(leafId: String): Int {
    fun containsLeaf(node: Node): Boolean = when (node) {
        is Leaf -> node.id == leafId
        is Branch -> node.children.any { containsLeaf(it) }
    }
    return indexOfFirst { page -> page.roots.any { containsLeaf(it) } }
}

/**
 * Builds a [ContentIndex] by traversing the document in order and
 * assigning a weight to every [Leaf].
 *
 * The path for each leaf includes the document itself as root, e.g.
 * `documentId/chapterId/paragraphId/textId`.
 */
fun Document.buildContentIndex(): ContentIndex {
    val entries = mutableListOf<ContentIndex.Entry>()
    var cumulativeWeight = 0.0
    val path = DomPath()

    fun visit(node: Node) {
        path.push(node.id)
        when (node) {
            is Leaf -> {
                val weight = node.contentWeight()
                entries += ContentIndex.Entry(
                    path = DomPath(path.toList()),
                    weight = weight,
                    cumulativeWeightBefore = cumulativeWeight,
                )
                cumulativeWeight += weight
            }

            is Branch -> node.children.forEach { visit(it) }
        }
        path.pop()
    }

    visit(this)
    return ContentIndex(entries, cumulativeWeight)
}

/**
 * Heuristic content weight for a leaf.
 * Behavior for node type:
 * - Text based on length.
 * - Image fixed value as the size is not yet known.
 * - Remainder a fixed value.
 */
private fun Leaf.contentWeight(): Double = when (this) {
    is Text -> text.length.toDouble()
    is Image -> 2000.0
    else -> 100.0
}