package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import dev.buecherregale.ebook_reader.core.dom.*

/**
 * Renders a [Text] node as [BasicText] applying the modifier.
 *
 * TextStyling is based on [Text.style] and [RenderingConfig].
 */
@Composable
fun DomText(
    text: Text,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier
) {
    BasicText(
        text = buildAnnotatedString {
            withStyle(text.style.toSpanStyle(config)) { append(text.text) }
        },
        style = androidx.compose.ui.text.TextStyle(
            fontSize = config.baseTextSize,
            fontFamily = config.bodyFontFamily,
            lineHeight = config.baseTextSize * config.lineHeightScale.toDouble(),
        ),
        modifier = modifier
    )
}

/**
 * Renders a [Link] node.
 *
 * @see [InlineContentRenderer]
 */
@Composable
fun DomLink(
    link: Link,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
) {
    InlineContentRenderer(nodes = listOf(link), config = config, modifier = modifier)
}

/**
 * Renders a [Ruby] node.
 *
 * @see [InlineContentRenderer]
 */
@Composable
fun DomRuby(
    ruby: Ruby,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
) {
    InlineContentRenderer(nodes = listOf(ruby), config = config, modifier = modifier)
}

/**
 * Renders inline [Node]s as a single flowing text, handling link taps correctly.
 *
 * Non-inline nodes in [nodes] are ignored.
 * They should be filtered out by the caller to ensure they are rendered.
 */
@Composable
internal fun InlineContentRenderer(
    nodes: List<Node>,
    config: RenderingConfig,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotated = buildAnnotatedString {
        nodes.forEach { appendInlineNode(it, config) }
    }

    BasicText(
        text = annotated,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = config.baseTextSize,
            fontFamily = config.bodyFontFamily,
            lineHeight = config.baseTextSize * config.lineHeightScale.toDouble(),
        ),
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { pos ->
                layoutResult.value?.let { layoutResult ->
                    val offset = layoutResult.getOffsetForPosition(pos)
                    annotated.getStringAnnotations(tag = LINK_TAG, start = offset, end = offset)
                        .firstOrNull()
                        ?.let { uriHandler.openUri(it.item) }
                }
            }
        },
        onTextLayout = { layoutResult.value = it }
    )
}

internal fun TextStyle.toSpanStyle(config: RenderingConfig): SpanStyle {
    var decoration = TextDecoration.None
    if (underline) decoration += TextDecoration.Underline
    if (strikethrough) decoration += TextDecoration.LineThrough

    return SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
        textDecoration = decoration.takeIf { it != TextDecoration.None },
        fontFamily = if (code) config.codeFontFamily else config.bodyFontFamily,
        fontSize = if (code) config.codeTextSize else config.baseTextSize,
        background = if (code) Color(0x22000000) else Color.Unspecified,
    )
}
