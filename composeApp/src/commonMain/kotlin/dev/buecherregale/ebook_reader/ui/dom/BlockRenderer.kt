package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
 * Spaced with the height of [RenderingConfig.paragraphSpacing] if empty.
 */
@Composable
fun DomDocument(
    book: Book,
    document: Document,
    config: RenderingConfig,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    Column(modifier = modifier) {
        if (document.children.isEmpty())
            Box(modifier = Modifier.height(config.paragraphSpacing))
        document.children.forEach {
            DomNode(book = book, node = it, config = config, onTextSelected = onTextSelected)
        }
    }
}

/**
 * Renderer for a [Division] node.
 * Renders the children as [Column], applying the [modifier].
 *
 * Spaced with the height of [RenderingConfig.paragraphSpacing] if empty.
 */
@Composable
fun DomDivision(
    book: Book,
    division: Division,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    Column(modifier = modifier) {
        if (division.children.isEmpty())
            Box(modifier = Modifier.height(config.paragraphSpacing))
        division.children.forEach { child ->
            DomNode(book = book, node = child, config = config, onTextSelected = onTextSelected)
        }
    }
}

/**
 * Renderer for a [Chapter] node.
 * Renders the children as [Column], applying the [modifier].
 *
 * Spaced with the height of [RenderingConfig.paragraphSpacing] if empty.
 */
@Composable
fun DomChapter(
    book: Book,
    chapter: Chapter,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    Column(modifier = modifier) {
        if (chapter.children.isEmpty())
            Box(modifier = Modifier.height(config.paragraphSpacing))
        chapter.children.forEach { child ->
            DomNode(book = book, node = child, config = config, onTextSelected = onTextSelected)
        }
    }
}

/**
 * Renderer for a [Paragraph] node.
 * Renders the children as [Column], applying the [modifier].
 *
 * Spaced with the height of [RenderingConfig.paragraphSpacing] if empty.
 */
@Composable
fun DomParagraph(
    book: Book,
    paragraph: Paragraph,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    val (inlineNodes, blockNodes) = paragraph.children.partition { it is Inline }

    Column(modifier = modifier) {
        if (inlineNodes.isNotEmpty()) {
            InlineContentRenderer(nodes = inlineNodes, config = config, onTextSelected = onTextSelected)
        }
        if (blockNodes.isEmpty())
            Box(modifier = Modifier.height(config.paragraphSpacing))
        blockNodes.forEach { child ->
            DomNode(book = book, node = child, config = config, onTextSelected = onTextSelected)
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
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    val headingConfig = config.copy(
        baseTextSize = config.headingSize(heading.level).fontSize,
        bodyFontFamily = config.headingFontFamily ?: config.bodyFontFamily,
    )

    Column(modifier = modifier.padding(bottom = config.paragraphSpacing)) {
        InlineContentRenderer(nodes = heading.children, config = headingConfig, onTextSelected = onTextSelected)
    }
}