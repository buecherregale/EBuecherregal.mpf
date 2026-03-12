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
) {
    when (node) {
        is Document -> DomDocument(book, node, config, modifier)
        is Chapter -> DomChapter(book, node, config, modifier)
        is Division -> DomDivision(book, node, config, modifier)
        is Paragraph -> DomParagraph(book, node, config, modifier)
        is Heading -> DomHeading(node, config, modifier)
        is ListBlock -> DomListBlock(book, node, config, modifier)
        is ListItem -> DomListItem(book, node, config, modifier, ordinalLabel = null)
        is ImageBlock -> DomImageBlock(book, node, config, modifier)
        is Image -> DomImage(book, node, config, modifier)
        is Link -> DomLink(node, config, modifier)
        is Ruby -> DomRuby(node, config, modifier)
        is Text -> DomText(node, config, modifier)
    }
}