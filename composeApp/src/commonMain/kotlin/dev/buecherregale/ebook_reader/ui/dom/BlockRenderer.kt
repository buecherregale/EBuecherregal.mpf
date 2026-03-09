package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.buecherregale.ebook_reader.core.dom.*
import dev.buecherregale.ebook_reader.core.domain.Book

/**
 * Renderer for a [Document] node.
 * Renders the document children as [Column], applying the [modifier].
 *
 * Children are spaced with the height of [RenderingConfig.paragraphSpacing].
 */
@Composable
fun DomDocument(
    book: Book,
    document: Document,
    config: RenderingConfig,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        document.children.forEach {
            DomNode(book = book, node = it, config)
            Spacer(modifier = Modifier.height(config.paragraphSpacing))
        }
    }
}

/**
 * Renderer for a [Division] node.
 * Renders the children as [Column], applying the [modifier].
 *
 * Children are spaced with the height of [RenderingConfig.paragraphSpacing].
 */
@Composable
fun DomDivision(
    book: Book,
    division: Division,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        division.children.forEach { child ->
            DomNode(book = book, node = child, config = config)
            Spacer(modifier = Modifier.height(config.paragraphSpacing))
        }
    }
}

/**
 * Renderer for a [Paragraph] node.
 * Renders the children as [Column], applying the [modifier].
 *
 * Children are spaced with the height of [RenderingConfig.paragraphSpacing].
 */
@Composable
fun DomParagraph(
    book: Book,
    paragraph: Paragraph,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
) {
    val (inlineNodes, blockNodes) = paragraph.children.partition { it is Inline }

    Column(modifier = modifier) {
        if (inlineNodes.isNotEmpty()) {
            InlineContentRenderer(nodes = inlineNodes, config = config)
        }
        blockNodes.forEach { child ->
            DomNode(book = book, node = child, config = config)
        }
    }
}

/**
 * Renders a [Heading] at the appropriate size from [RenderingConfig.headingSize].
 * Children are rendered as a [Column], applying the [modifier] to it.
 *
 * Heading children are merged into a single text run so that inline formatting within headings (`<h1><em>Title</em></h1>`) works
 * correctly.
 */
@Composable
fun DomHeading(
    heading: Heading,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
) {
    val headingConfig = config.copy(
        baseTextSize = config.headingSize(heading.level).fontSize,
        bodyFontFamily = config.headingFontFamily ?: config.bodyFontFamily,
    )

    Column(modifier = modifier.padding(bottom = config.paragraphSpacing)) {
        InlineContentRenderer(nodes = heading.children, config = headingConfig)
    }
}