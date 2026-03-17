package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.buecherregale.ebook_reader.core.dom.*
import dev.buecherregale.ebook_reader.core.domain.Book

/**
 * Renders any [Node] from the DOM by determining the type and choosing the appropriate rendering function.
 *
 * Unsupported node types are *ignored*.
 *
 * ```kotlin
 * // Render a full document
 * NodeRenderer(document, config = DomRenderConfig(baseTextSize = 18.sp))
 *
 * // Render just one paragraph
 * NodeRenderer(paragraph)
 * ```
 *
 * @param book     The book that is being rendered. Needed to load resources.
 * @param node     The DOM [Node] to render.
 * @param config   Visual configuration. Defaults to [RenderingConfig.Default].
 * @param modifier Applied to the outermost layout of the rendered node.
 */
@Composable
fun DomNode(
    book: Book,
    node: Node,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    when (node) {
        is Document -> DomDocument(book, node, config, modifier, onTextSelected = onTextSelected)
        is Chapter -> DomChapter(book, node, config, modifier, onTextSelected = onTextSelected)
        is Division -> DomDivision(book, node, config, modifier, onTextSelected = onTextSelected)
        is Paragraph -> DomParagraph(book, node, config, modifier, onTextSelected = onTextSelected)
        is Heading -> DomHeading(node, config, modifier, onTextSelected = onTextSelected)
        is ListBlock -> DomListBlock(book, node, config, modifier, onTextSelected = onTextSelected)
        is ListItem -> DomListItem(book, node, config, modifier, ordinalLabel = null, onTextSelected = onTextSelected)
        is ImageBlock -> DomImageBlock(book, node, config, modifier, onTextSelected = onTextSelected)
        is Image -> DomImage(book, node, config, modifier)
        is Link -> DomLink(node, config, modifier, onTextSelected = onTextSelected)
        is Ruby -> DomRuby(node, config, modifier, onTextSelected = onTextSelected)
        is RubyAnnotation -> DomText(node.annotationText, config, modifier, onTextSelected = onTextSelected)
        is Text -> DomText(node, config, modifier, onTextSelected = onTextSelected)
    }
}