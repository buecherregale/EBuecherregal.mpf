package dev.buecherregale.ebook_reader.ui.dom

import dev.buecherregale.ebook_reader.core.dom.*

/**
 * A single page, represented by the nodes at its root.
 */
data class Page(val roots: List<Node>)

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

// Broken links in this comment are just code snippets. Escaping the squared bracket like "\[" does not work.
/**
 * Paginates the given [Document] by dividing it into fix sized pages.
 *
 * Behavior for node types:
 * - [Leaf] are never split. If they take up more than one whole page, the used height exceeds [pageHeightPx].
 * - [Branch] nodes may be split up, with their `children` placed across multiple pages.
 *   The [Branch] node itself is then displayed for each page it has children on, a clone of the branch is always the child's parent.
 * - [Chapter] nodes always create new pages.
 *
 * @receiver            The document to divide into pages.
 * @param nodeHeights   Mapping [Node.id] to the height the node takes up. For [Branch] this includes the branch itself, but also its children.
 *                      **Contract:** `nodeHeights[branch.id] = branchHeights[branch.id] + branch.children.map { nodeHeights[it.id] }.sum()`
 * @param branchHeights Mapping [Branch.id] to the height the branch itself takes up, as if it had no children.
 * @param pageHeightPx  The height of a page in pixels.
 * @return              A list of pages.
 */
fun Document.paginate(
    nodeHeights: Map<String, Int>,
    branchHeights: Map<String, Int>,
    pageHeightPx: Int,
): List<Page> {
    val builder = PagesBuilder()

    for (child in children) {
        var toWork = child
        if (toWork is Chapter)
            builder.newPage()
        // [Branch.split] might yield additional nodes to iterate over
        // it would be enough to only repeat the branch part tho
        while (true) {
            val height = nodeHeights[toWork.id] ?: 0
            if (toWork is Leaf) {
                if (builder.consumedHeightPx + height < pageHeightPx) {
                    builder.add(child, height)
                } else {
                    builder.newPage()
                    builder.add(child, height)
                }
                break
            }
            toWork as Branch
            when (val split = toWork.split(
                budget = pageHeightPx - builder.consumedHeightPx,
                heightOf = { nodeHeights[it.id] ?: 0 },
                heightOfEmpty = { branchHeights[it.id] ?: 0 })) {
                is SplitResult.All -> {
                    builder.add(toWork, height)
                    break
                }

                is SplitResult.Some -> {
                    builder.add(toWork.cloneWithChildren(split.fittingChildren), height)
                    toWork = toWork.cloneWithChildren(split.remainingChildren)
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
 * @return              The clone of the branch.
 */
internal fun Branch.cloneWithChildren(newChildren: MutableList<Node>): Branch = when (this) {
    is Document -> copy(children = newChildren)
    is Chapter -> copy(children = newChildren)
    is Division -> copy(children = newChildren)
    is Paragraph -> copy(children = newChildren)
    is Heading -> copy(children = newChildren)
    is ListBlock -> copy(children = newChildren)
    is ListItem -> copy(children = newChildren)
    is Link -> copy(children = newChildren)
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
 *
 * ##### Note
 * Although lists for children like [All.children] or [Some.remainingChildren] are generally mutable, callers receiving a [SplitResult] should not mutate the lists.
 * This is more used as a convenience since [Branch.children] is also mutable.
 */
private sealed interface SplitResult {
    val consumedBudget: Int

    /**
     * No children fit into the budget,
     * **or** the empty branch doesn't fit.
     *
     * [consumedBudget] is always `0`.
     */
    class None : SplitResult {
        override val consumedBudget: Int = 0
    }

    /**
     * A subset of children fit into the budget.
     * These children, as well as the remaining might be clones resulting from recursive splits, created via [cloneWithChildren].
     *
     * [consumedBudget] is `heightOfEmpty(branch) + fittingChildren.map { heightOf(it) }.sum()`.
     */
    data class Some(
        override val consumedBudget: Int,
        val fittingChildren: MutableList<Node>,
        val remainingChildren: MutableList<Node>,
    ) : SplitResult

    /**
     * All children fit.
     * [children] contains a copy of the original children.
     *
     * [consumedBudget] should equal to `heightOf(branch)`.
     */
    data class All(
        override val consumedBudget: Int,
        val children: MutableList<Node>,
    ) : SplitResult
}

/**
 * Splits a [Branch] so it fits into the [budget] given.
 * The [SplitResult] returned describes what part of the [Branch] fit into the budget.
 *
 * This method is *recursive*. If a branches child does not fit (and is a branch), [split] is called on it.
 *
 * If no children fit, but the empty branch would (via [heightOfEmpty]), the branch is **NOT** included, [SplitResult.None] is returned.
 *
 * @receiver            The [Branch] to be split. There is no special handling for individual implementations.
 *                      [this]`.children` have to be either [Branch] or [Leaf].
 *                      [cloneWithChildren] needs to be implemented for [this] and the children.
 * @param budget        The height budget to fit the branch into.
 * @param heightOf      Function to obtain the height of any node. For branches this should equal `heightOfEmpty(branch) + children.map { heightOf(it) }.sum()`.
 * @param heightOfEmpty Function to obtain the height of a [Branch] without its children, as if it had none.
 * @return              A [SplitResult] describing the fitting part of the branch.
 */
private fun Branch.split(
    budget: Int,
    heightOf: (Node) -> Int,
    heightOfEmpty: (Branch) -> Int,
): SplitResult {
    val height = heightOf(this)
    if (height < budget)
        return SplitResult.All(height, children)
    val emptyHeight = heightOfEmpty(this)
    if (emptyHeight > height)
        return SplitResult.None()
    // do split
    var toFit = children.toMutableList()
    val fittingChildren = mutableListOf<Node>()
    var remainingBudget = budget
    children.forEach { child ->
        if (child is Leaf) {
            val childHeight = heightOf(child)
            if (childHeight < remainingBudget) {
                fittingChildren += child
                toFit -= child
                remainingBudget -= childHeight
                return@forEach
            } else {
                return if (fittingChildren.isEmpty()) {
                    SplitResult.None()
                } else {
                    SplitResult.Some(budget - remainingBudget, fittingChildren, toFit)
                }
            }
        }
        child as Branch
        when (val result = child.split(remainingBudget, heightOf, heightOfEmpty)) {
            is SplitResult.None -> {
                return if (fittingChildren.isEmpty()) {
                    SplitResult.None()
                } else {
                    SplitResult.Some(budget - remainingBudget, fittingChildren, toFit)
                }
            }

            is SplitResult.Some -> {
                fittingChildren += child.cloneWithChildren(result.fittingChildren)
                toFit -= child // remove original
                toFit = mutableListOf<Node>(child.cloneWithChildren(result.remainingChildren)).apply { addAll(toFit) }
                remainingBudget -= result.consumedBudget

                return SplitResult.Some(budget - remainingBudget, fittingChildren, toFit)
            }

            is SplitResult.All -> {
                fittingChildren.add(child)
                toFit -= child
                remainingBudget -= result.consumedBudget
            }
        }
    }
    return SplitResult.All(budget - remainingBudget, fittingChildren)
}