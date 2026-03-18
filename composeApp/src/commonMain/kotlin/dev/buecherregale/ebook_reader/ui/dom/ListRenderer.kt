package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.buecherregale.ebook_reader.core.dom.Inline
import dev.buecherregale.ebook_reader.core.dom.ListBlock
import dev.buecherregale.ebook_reader.core.dom.ListItem
import dev.buecherregale.ebook_reader.core.domain.Book

/**
 * Renders a [ListBlock] as a vertical [Column] of [ListItem]s.
 *
 * Ordered lists use a numeric label ("1.", "2.", …).
 * Unordered lists use a bullet character ("•").
 *
 * Nested lists are supported.
 */
@Composable
fun DomListBlock(
    book: Book,
    list: ListBlock,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    Column(modifier = modifier) {
        list.children.forEachIndexed { index, child ->
            val label = if (list.ordered) "${index + 1}." else "•"
            when (child) {
                is ListItem -> DomListItem(
                    book = book,
                    item = child,
                    config = config,
                    ordinalLabel = label,
                    onTextSelected = onTextSelected,
                )

                else -> {
                    DomNode(book = book, node = child, config = config, onTextSelected = onTextSelected)
                }
            }
            Spacer(modifier = Modifier.height(config.listItemSpacing))
        }
    }
}

/**
 * Renders a single [ListItem] as a [Row] of [ordinalLabel] + content.
 *
 * [ordinalLabel] is supplied by the parent [DomListBlock]; when called
 * directly via [DomNode] it defaults to null and no bullet is shown.
 */
@Composable
fun DomListItem(
    book: Book,
    item: ListItem,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    ordinalLabel: String?,
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    Row(modifier = modifier.padding(start = config.listIndent)) {
        if (ordinalLabel != null) {
            Text(
                text = ordinalLabel,
                style = config.baseAsTextStyle(),
            )
            Spacer(modifier = Modifier.width(config.listIndent / 2))
        }

        val (inlineNodes, blockNodes) = item.children.partition { it is Inline }
        Column(modifier = Modifier.weight(1f)) {
            if (inlineNodes.isNotEmpty()) {
                InlineContentRenderer(
                    book = book,
                    nodes = inlineNodes,
                    config = config,
                    onTextSelected = onTextSelected,
                )
            }
            blockNodes.forEach { child ->
                DomNode(book = book, node = child, config = config, onTextSelected = onTextSelected)
            }
        }
    }
}