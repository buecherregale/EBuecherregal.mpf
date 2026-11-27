package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import dev.buecherregale.ebook_reader.core.dom.*
import kotlin.math.roundToInt

/**
 * A single page, represented by the nodes at its root.
 */
data class Page(val roots: List<Node>)

/**
 * Line metrics of the inline text of a node (e.g. [Paragraph] or [Heading]), as laid out during measurement.
 *
 * Used to split inline content at line boundaries, so a single paragraph can flow across multiple pages.
 *
 * @param lineStarts  `lineStarts[i]` is the char offset (inside the annotated string built from the inline children)
 *                    at which line `i` starts.
 * @param lineBottoms `lineBottoms[i]` is the bottom of line `i` in pixels, relative to the top of the text.
 */
data class InlineLayout(
    val lineStarts: List<Int>,
    val lineBottoms: List<Int>,
) {
    val lineCount: Int get() = lineStarts.size

    /** Index of the line containing the char [offset]. */
    fun lineForOffset(offset: Int): Int =
        lineStarts.indexOfLast { it <= offset }.coerceAtLeast(0)

    /** Top of line [line] in pixels. */
    fun lineTop(line: Int): Int = if (line <= 0) 0 else lineBottoms[line - 1]

    companion object {
        fun of(result: TextLayoutResult): InlineLayout = InlineLayout(
            lineStarts = List(result.lineCount) { result.getLineStart(it) },
            lineBottoms = List(result.lineCount) { result.getLineBottom(it).roundToInt() },
        )
    }
}

// --- ids of split pieces ---

/**
 * Separator between the original id and the char offset for nodes created by splitting inline content.
 * Node ids are UUIDs, so this cannot clash.
 */
private const val SPLIT_ID_SEPARATOR = "#split"

/**
 * Creates the id for a piece of the node [baseId], which starts at char [offset] of the original node.
 * Offset `0` is the original node itself.
 */
internal fun splitId(baseId: String, offset: Int): String =
    if (offset == 0) baseId else "$baseId$SPLIT_ID_SEPARATOR$offset"

/**
 * Reverses [splitId].
 *
 * @return The original id and the char offset the piece starts at (`0` for non-split nodes).
 */
internal fun parseSplitId(id: String): Pair<String, Int> {
    val idx = id.lastIndexOf(SPLIT_ID_SEPARATOR)
    if (idx < 0) return id to 0
    val offset = id.substring(idx + SPLIT_ID_SEPARATOR.length).toIntOrNull() ?: return id to 0
    return id.substring(0, idx) to offset
}

/**
 * Helper class for building the page list.
 *
 * Handles the list as well as the current page being built.
 */
private class PagesBuilder(
    val pages: MutableList<Page> = mutableListOf(),
    val current: MutableList<Node> = mutableListOf(),
    var consumedHeightPx: Int = 0,
) {
    /**
     * Adds a [Node] to the current page.
     *
     * The [height] is added to [consumedHeightPx].
     *
     * @param node   The node to add.
     * @param height The height of the added node in pixels.
     */
    fun add(node: Node, height: Int) {
        current += node
        consumedHeightPx += height
    }

    /**
     * Flushes the current page, creating a new one.
     *
     * Resets the [current] page.
     * Resets the [consumedHeightPx].
     *
     * If [current] is empty, **no** empty page will be created.
     */
    fun newPage() {
        if (current.isEmpty())
            return
        pages += Page(current.toMutableList()) //.toMutableList() to copy content
        current.clear()
        consumedHeightPx = 0
    }
}

/**
 * Bundles all measurements needed for splitting.
 */
private class Measurements(
    val nodeHeights: Map<String, Int>,
    val branchHeights: Map<String, Int>,
    val inlineLayouts: Map<String, InlineLayout>,
) {
    /**
     * Height of any node.
     *
     * For pieces of split inline containers (see [splitInline]) the height is derived from the [InlineLayout]
     * of the original node.
     */
    fun heightOf(node: Node): Int {
        nodeHeights[node.id]?.let { return it }
        if (node !is Branch) return 0
        val (baseId, offset) = parseSplitId(node.id)
        val full = nodeHeights[baseId] ?: return 0
        val layout = inlineLayouts[baseId] ?: return full
        return full - layout.lineTop(layout.lineForOffset(offset))
    }

    fun heightOfEmpty(branch: Branch): Int =
        branchHeights[branch.id] ?: branchHeights[parseSplitId(branch.id).first] ?: 0

    /**
     * The [InlineLayout] of the original node of [branch], if [branch] can be split at line boundaries.
     */
    fun inlineLayoutOf(branch: Branch): InlineLayout? {
        if (!branch.isInlineContainer()) return null
        return inlineLayouts[parseSplitId(branch.id).first]
    }
}

/**
 * Whether the branch renders all of its children as a single flowing text and can therefore be split at line boundaries.
 *
 * [ListItem] is excluded, as each piece would be rendered with its own bullet.
 */
private fun Branch.isInlineContainer(): Boolean =
    (this is Paragraph || this is Heading) && children.isNotEmpty() && children.all { it is Inline }

// Broken links in this comment are just code snippets. Escaping the squared bracket like "\[" does not work.
/**
 * Paginates the given [Document] by dividing it into fix sized pages.
 *
 * Behavior for node types:
 * - [Leaf] are never split. If they take up more than one whole page, the used height exceeds [pageHeightPx].
 * - [Branch] nodes may be split up, with their `children` placed across multiple pages.
 *   The [Branch] node itself is then displayed for each page it has children on, a clone of the branch is always the child's parent.
 * - [Paragraph] and [Heading] with only [Inline] children are split at line boundaries, if an [InlineLayout] is given.
 *   [Text] nodes are split in the process. The resulting pieces get ids created with [splitId].
 * - [Chapter] nodes always create new pages.
 *
 * @receiver            The document to divide into pages.
 * @param nodeHeights   Mapping [Node.id] to the height the node takes up. For [Branch] this includes the branch itself, but also its children.
 *                      **Contract:** `nodeHeights[ branch.id ] = branchHeights[ branch.id ] + branch.children.map { nodeHeights[ it.id ] }.sum()`
 * @param branchHeights Mapping [Branch.id] to the height the branch itself takes up, as if it had no children.
 * @param pageHeightPx  The height of a page in pixels.
 * @param inlineLayouts Mapping [Branch.id] of inline containers to their [InlineLayout].
 *                      Measured with the same width and style as [nodeHeights].
 *                      Inline containers without a layout are split by their children instead.
 * @return              A list of pages.
 */
fun Document.paginate(
    nodeHeights: Map<String, Int>,
    branchHeights: Map<String, Int>,
    pageHeightPx: Int,
    inlineLayouts: Map<String, InlineLayout> = emptyMap(),
): List<Page> {
    val builder = PagesBuilder()
    val measurements = Measurements(nodeHeights, branchHeights, inlineLayouts)

    for (child in children) {
        var toWork = child
        if (toWork is Chapter)
            builder.newPage()
        // [Branch.split] might yield additional nodes to iterate over
        // it would be enough to only repeat the branch part tho
        while (true) {
            if (toWork is Leaf) {
                val height = measurements.heightOf(toWork)
                if (builder.consumedHeightPx + height >= pageHeightPx)
                    builder.newPage()
                builder.add(toWork, height)
                break
            }
            toWork as Branch
            when (val split = toWork.split(
                budget = pageHeightPx - builder.consumedHeightPx,
                measurements = measurements,
                // on an empty page, an oversized leaf has to be placed anyway or we loop forever
                forceFirstLeaf = builder.current.isEmpty())) {
                is SplitResult.All -> {
                    builder.add(toWork, split.consumedBudget)
                    break
                }

                is SplitResult.Some -> {
                    builder.add(split.fitting, split.consumedBudget)
                    toWork = split.remaining
                    builder.newPage()
                    continue
                }

                is SplitResult.None -> {
                    builder.newPage()   // repeat with same element
                    continue
                }
            }
        }
    }
    builder.newPage()
    return builder.pages
}

/**
 * Clones the given [Branch] preserving all data, except replacing [Branch.children].
 *
 * @receiver            The branch to clone.
 * @param newChildren   The children of the clone.
 * @param newId         The id of the clone.
 * @return              The clone of the branch.
 */
internal fun Branch.cloneWithChildren(newChildren: MutableList<Node>, newId: String = id): Branch = when (this) {
    is Document -> copy(id = newId, children = newChildren)
    is Chapter -> copy(id = newId, children = newChildren)
    is Division -> copy(id = newId, children = newChildren)
    is Paragraph -> copy(id = newId, children = newChildren)
    is Heading -> copy(id = newId, children = newChildren)
    is ListBlock -> copy(id = newId, children = newChildren)
    is ListItem -> copy(id = newId, children = newChildren)
    is Link -> copy(id = newId, children = newChildren)
}

/**
 * Describes the way in which a [Branch] can be split using [split].
 * Precisely, it describes the way child nodes fit.
 *
 * Additionally, the cost of the Branch is returned.
 * This cost does include the height of the Branch itself.
 *
 * - [None]: The whole branch does not fit
 * - [Some]: A part of the children fits. Another part remains out of budget.
 * - [All]: The whole branch with all children fits
 */
private sealed interface SplitResult {
    val consumedBudget: Int

    /**
     * No children fit into the budget,
     * **or** the empty branch doesn't fit.
     *
     * [consumedBudget] is always `0`.
     */
    object None : SplitResult {
        override val consumedBudget: Int = 0
    }

    /**
     * A part of the branch fits into the budget.
     * [fitting] and [remaining] are clones of the split branch, created via [cloneWithChildren].
     * Their children might be clones resulting from recursive splits as well.
     *
     * [consumedBudget] is the height of [fitting].
     */
    data class Some(
        override val consumedBudget: Int,
        val fitting: Branch,
        val remaining: Branch,
    ) : SplitResult

    /**
     * All children fit.
     *
     * [consumedBudget] should equal to `heightOf(branch)`.
     */
    data class All(
        override val consumedBudget: Int,
    ) : SplitResult
}

/**
 * Splits a [Branch] so it fits into the [budget] given.
 * The [SplitResult] returned describes what part of the [Branch] fit into the budget.
 *
 * This method is *recursive*. If a branches child does not fit (and is a branch), [split] is called on it.
 * Inline containers with a known [InlineLayout] are split at line boundaries via [splitInline].
 *
 * If no children fit, but the empty branch would, the branch is **NOT** included, [SplitResult.None] is returned.
 *
 * @receiver            The [Branch] to be split. There is no special handling for individual implementations.
 *                      [this]`.children` have to be either [Branch] or [Leaf].
 *                      [cloneWithChildren] needs to be implemented for [this] and the children.
 * @param budget        The height budget to fit the branch into.
 * @param measurements  The heights and layouts of the nodes.
 * @param forceFirstLeaf If `true`, the first [Leaf] (or first line of text) is always included, even if it exceeds the [budget].
 *                      Required when splitting onto an empty page, otherwise an oversized leaf could never be placed.
 * @return              A [SplitResult] describing the fitting part of the branch.
 */
private fun Branch.split(
    budget: Int,
    measurements: Measurements,
    forceFirstLeaf: Boolean = false,
): SplitResult {
    val height = measurements.heightOf(this)
    if (height < budget)
        return SplitResult.All(height)
    measurements.inlineLayoutOf(this)?.let { layout ->
        return splitInline(layout, budget, height, forceFirstLeaf)
    }
    // do split
    var toFit = children.toMutableList()
    val fittingChildren = mutableListOf<Node>()
    var remainingBudget = budget

    fun partial(): SplitResult =
        if (fittingChildren.isEmpty()) SplitResult.None
        else SplitResult.Some(
            consumedBudget = budget - remainingBudget,
            fitting = cloneWithChildren(fittingChildren),
            remaining = cloneWithChildren(toFit),
        )

    for (child in children) {
        val force = forceFirstLeaf && fittingChildren.isEmpty()
        if (child is Leaf) {
            val childHeight = measurements.heightOf(child)
            if (childHeight < remainingBudget || force) {
                fittingChildren += child
                toFit.removeAt(0)
                remainingBudget -= childHeight
                continue
            }
            return partial()
        }
        child as Branch
        when (val result = child.split(remainingBudget, measurements, force)) {
            is SplitResult.None -> return partial()

            is SplitResult.Some -> {
                fittingChildren += result.fitting
                toFit[0] = result.remaining
                remainingBudget -= result.consumedBudget
                return partial()
            }

            is SplitResult.All -> {
                fittingChildren += child
                toFit.removeAt(0)
                remainingBudget -= result.consumedBudget
            }
        }
    }
    return SplitResult.All(budget - remainingBudget)
}

/**
 * Splits an inline container (see [isInlineContainer]) at the last line boundary fitting into the [budget].
 *
 * The [layout] belongs to the original node. [this] might already be a piece of it, starting at the char offset
 * encoded in its id (see [splitId]).
 * The remaining piece gets a new id via [splitId], so its height can be derived by [Measurements.heightOf].
 *
 * The spacing below the text (e.g. paragraph spacing) is not counted for the fitting piece,
 * as it is only whitespace at the bottom of the page.
 *
 * @param height     The height of [this], including spacing.
 * @param forceFirstLine If `true`, at least one line is included, even if it exceeds the [budget].
 */
private fun Branch.splitInline(
    layout: InlineLayout,
    budget: Int,
    height: Int,
    forceFirstLine: Boolean,
): SplitResult {
    val (baseId, pieceStart) = parseSplitId(id)
    val startLine = layout.lineForOffset(pieceStart)
    val top = layout.lineTop(startLine)

    var endLine = startLine // exclusive
    while (endLine < layout.lineCount && layout.lineBottoms[endLine] - top < budget)
        endLine++
    if (endLine == startLine) {
        if (!forceFirstLine) return SplitResult.None
        endLine = startLine + 1
    }
    // all lines fit, only the spacing below did not
    if (endLine >= layout.lineCount)
        return SplitResult.All(minOf(height, budget))

    val (head, tail, tailOffset) = splitInlines(children, layout.lineStarts[endLine] - pieceStart)
    if (head.isEmpty() || tail.isEmpty())
        return if (head.isEmpty()) SplitResult.None else SplitResult.All(minOf(height, budget))
    return SplitResult.Some(
        consumedBudget = layout.lineBottoms[endLine - 1] - top,
        fitting = cloneWithChildren(head),
        remaining = cloneWithChildren(tail, splitId(baseId, pieceStart + tailOffset)),
    )
}

/**
 * Splits a list of inline nodes at the char [offset] of the annotated string built from them (see [appendInlineNode]).
 *
 * [Text] nodes are split in two, the second piece getting a new id via [splitId].
 * [Link] nodes are split recursively, both pieces keeping the id.
 * Other inline nodes ([Ruby]) are atomic: if [offset] lies inside them, they are moved to the tail.
 *
 * @return The head, the tail and the actual offset at which the tail starts (`<=` [offset]).
 */
private fun splitInlines(nodes: List<Node>, offset: Int): Triple<MutableList<Node>, MutableList<Node>, Int> {
    val head = mutableListOf<Node>()
    val tail = mutableListOf<Node>()
    var tailStart = -1
    var pos = 0
    for (node in nodes) {
        val length = inlineLength(node)
        when {
            tailStart >= 0 || pos >= offset -> {
                if (tailStart < 0) tailStart = pos
                tail += node
            }

            pos + length <= offset -> head += node

            node is Text -> {
                val cut = offset - pos
                val (baseId, textStart) = parseSplitId(node.id)
                head += node.copy(text = node.text.substring(0, cut))
                tail += node.copy(id = splitId(baseId, textStart + cut), text = node.text.substring(cut))
                tailStart = offset
            }

            node is Link -> {
                val (linkHead, linkTail, linkTailStart) = splitInlines(node.children, offset - pos)
                if (linkHead.isNotEmpty()) head += node.copy(children = linkHead)
                if (linkTail.isNotEmpty()) tail += node.copy(children = linkTail)
                tailStart = pos + linkTailStart
            }

            else -> {
                tailStart = pos
                tail += node
            }
        }
        pos += length
    }
    return Triple(head, tail, if (tailStart < 0) pos else tailStart)
}

/**
 * The number of chars the inline [node] contributes to the rendered annotated string.
 */
private fun inlineLength(node: Node): Int = when (node) {
    is Text -> node.text.length
    is Link -> node.children.sumOf { inlineLength(it) }
    else -> buildAnnotatedString { appendInlineNode(node, RenderingConfig.Default) }.length
}
