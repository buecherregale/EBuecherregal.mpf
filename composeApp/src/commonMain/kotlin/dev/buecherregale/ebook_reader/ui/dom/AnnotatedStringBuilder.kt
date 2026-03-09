package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import dev.buecherregale.ebook_reader.core.dom.Link
import dev.buecherregale.ebook_reader.core.dom.Node
import dev.buecherregale.ebook_reader.core.dom.Ruby
import dev.buecherregale.ebook_reader.core.dom.Text

internal const val LINK_TAG = "URL"

internal fun AnnotatedString.Builder.appendInlineNode(node: Node, config: RenderingConfig) {
    when (node) {
        is Text -> appendTextNode(node, config)
        is Link -> appendLinkNode(node, config)
        is Ruby -> appendRubyNode(node, config)
        else -> {}
    }
}

internal fun AnnotatedString.Builder.appendTextNode(node: Text, config: RenderingConfig) {
    withStyle(node.style.toSpanStyle(config)) {
        append(node.text)
    }
}

internal fun AnnotatedString.Builder.appendLinkNode(node: Link, config: RenderingConfig) {
    val linkColor = config.linkColor ?: Color.Unspecified
    pushStringAnnotation(tag = LINK_TAG, annotation = node.target)
    withStyle(
        SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.Underline,
        )
    ) {
        node.children.forEach { child -> appendInlineNode(child, config) }
    }
    pop()
}

/**
 * Ruby is rendered as "base(annotation)" in plain text since Compose does not
 * natively support the HTML ruby layout. A proper ruby layout would require a
 * custom [androidx.compose.ui.layout.Layout] — suitable as a future enhancement.
 */
internal fun AnnotatedString.Builder.appendRubyNode(node: Ruby, config: RenderingConfig) {
    val annotationStyle = SpanStyle(fontSize = config.rubyAnnotationScale.em)
    node.baseText.forEachIndexed { index, base ->
        append(base.text)
        val annotation = node.annotationText.getOrNull(index)
        if (annotation != null) {
            withStyle(annotationStyle) {
                append("(${annotation.text})")
            }
        }
    }
}