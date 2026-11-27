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
    private val entryByLeafId: Map<String, Entry> = entries.associateBy { it.path.peek() }

    /**
     * Returns the progress fraction for the leaf with the given [leafId].
     *
     * For pieces of split [Text] nodes (see [splitId]), the offset into the text is taken into account.
     */
    fun fractionForLeafId(leafId: String): Double? {
        val (baseId, offset) = parseSplitId(leafId)
        val entry = entryByLeafId[baseId] ?: return null
        val weight = entry.cumulativeWeightBefore + offset.toDouble().coerceAtMost(entry.weight)
        return (weight / totalWeight).coerceIn(0.0, 1.0)
    }

    /**
     * Returns the id of the leaf at the given progress [fraction].
     *
     * Unlike [pathAtFraction], the position inside a [Text] is preserved by returning a [splitId],
     * which can be resolved to the correct page via [pageIndexForLeafId].
     */
    fun leafIdAtFraction(fraction: Double): String? {
        if (entries.isEmpty()) return null
        val target = (fraction * totalWeight).coerceIn(0.0, totalWeight)
        val entry = entries.lastOrNull { it.cumulativeWeightBefore <= target } ?: entries.first()
        val offset = (target - entry.cumulativeWeightBefore).toInt().coerceIn(0, entry.weight.toInt())
        return splitId(entry.path.peek(), offset)
    }

    fun pathAtFraction(fraction: Double): DomPath {
        if (entries.isEmpty()) return DomPath()
        val target = (fraction * totalWeight).coerceIn(0.0, totalWeight)
        return entries
            .lastOrNull { it.cumulativeWeightBefore <= target }
            ?.path
            ?: entries.first().path
    }

    /**
     * Returns the ID of the first leaf whose path starts with [prefix],
     * or `null` if no such leaf exists.
     *
     * Used to navigate to a [DomPath] that points at a [Branch].
     */
    fun firstLeafIdUnder(prefix: DomPath): String? {
        val prefixList = prefix.toList()
        return entries.firstOrNull { entry ->
            entry.path.toList().let { it.size >= prefixList.size && it.take(prefixList.size) == prefixList }
        }?.path?.peek()
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
 * Returns the index of the page that contains the [Leaf] with [leafId],
 * or `-1` if not found.
 *
 * [leafId] may be a [splitId] of a leaf. As the pages might be split differently from when the id was obtained,
 * the page with the last piece starting at or before the offset is returned.
 */
fun List<Page>.pageIndexForLeafId(leafId: String): Int {
    val (baseId, offset) = parseSplitId(leafId)
    fun containsPiece(node: Node): Boolean = when (node) {
        is Leaf -> parseSplitId(node.id).let { (id, start) -> id == baseId && start <= offset }
        is Branch -> node.children.any { containsPiece(it) }
    }
    return indexOfLast { page -> page.roots.any { containsPiece(it) } }
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