package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import dev.buecherregale.ebook_reader.core.dom.*
import dev.buecherregale.ebook_reader.core.dom.TextStyle

/**
 * Renders a [Text] node as [Text] applying the modifier.
 *
 * TextStyling is based on [Text.style] and [RenderingConfig].
 */
@Composable
fun DomText(
    text: Text,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText) -> Unit) = {},
) {
    Text(
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
 * Renders a [Link] node.@Composable
 * fun DomText(
 *     text: Text,
 *     config: RenderingConfig = RenderingConfig.Default,
 *     modifier: Modifier = Modifier
 * ) {
 *     BasicText(
 *         text = buildAnnotatedString {
 *             withStyle(text.style.toSpanStyle(config)) { append(text.text) }
 *         },
 *         style = androidx.compose.ui.text.TextStyle(
 *             fontSize = config.baseTextSize,
 *             fontFamily = config.bodyFontFamily,
 *             lineHeight = config.baseTextSize * config.lineHeightScale.toDouble(),
 *         ),
 *         modifier = modifier
 *     )
 * }
 *
 * @see [InlineContentRenderer]
 */
@Composable
fun DomLink(
    link: Link,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText) -> Unit) = {},
) {
    InlineContentRenderer(nodes = listOf(link), config = config, modifier = modifier, onTextSelected = onTextSelected)
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
    onTextSelected: ((SelectedText) -> Unit) = {},
) {
    InlineContentRenderer(nodes = listOf(ruby), config = config, modifier = modifier, onTextSelected = onTextSelected)
}

/**
 * Renders inline [Node]s as a single flowing text, handling link taps correctly.
 *
 * Non-inline nodes in [nodes] are ignored.
 * They should be filtered out by the caller to ensure they are rendered.
 *
 * Also takes care of:
 * - Selection via [onTextSelected]
 * - Link resolution if text is tagged with [LINK_TAG], using [LocalUriHandler]`.current`.
 */
@Composable
internal fun InlineContentRenderer(
    nodes: List<Node>,
    config: RenderingConfig,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText) -> Unit) = {},
) {
    val uriHandler = LocalUriHandler.current
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val annotated = buildAnnotatedString {
        nodes.forEach { appendInlineNode(it, config) }
    }

    Text(
        text = annotated,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = config.baseTextSize,
            fontFamily = config.bodyFontFamily,
            lineHeight = config.baseTextSize * config.lineHeightScale.toDouble(),
        ),
        modifier = modifier
            .onGloballyPositioned { coordinates = it }
            .pointerInput(annotated, onTextSelected) {
                detectTapGestures { tapPos ->
                    val lr = layoutResult.value ?: return@detectTapGestures
                    val offset = lr.getOffsetForPosition(tapPos)

                    // links have priority
                    val link = annotated
                        .getStringAnnotations(LINK_TAG, offset, offset)
                        .firstOrNull()
                    if (link != null) {
                        uriHandler.openUri(link.item)
                        return@detectTapGestures
                    }

                    val coords = coordinates ?: return@detectTapGestures
                    resolveSelectedText(
                        tapPos = tapPos,
                        layoutResult = lr,
                        fullText = annotated.text,
                        localToScreen = coords::localToWindow,
                    )?.let(onTextSelected)
                }
            },
        onTextLayout = { layoutResult.value = it },
    )
}

data class SelectedText(
    val index: Int,
    val word: String,
    val wordRange: TextRange,
    val bounds: Rect,
)

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

private fun Rect.union(other: Rect) = Rect(
    left = minOf(left, other.left),
    top = minOf(top, other.top),
    right = maxOf(right, other.right),
    bottom = maxOf(bottom, other.bottom),
)

/**
 * Resolves the word at [tapPos] inside [layoutResult] and returns a
 * [SelectedText] with screen-space bounds, or `null` if the tap didn't
 * land on a non-blank word.
 *
 * @param tapPos           Position of the tap in local (composable) coordinates.
 * @param layoutResult     The [TextLayoutResult] from the [Text] composable.
 * @param fullText         The plain [String] backing the annotated string.
 * @param localToScreen    Converts a local [Offset] to screen coordinates.
 *                         Supply `LayoutCoordinates::localToWindow`.
 */
private fun resolveSelectedText(
    tapPos: Offset,
    layoutResult: TextLayoutResult,
    fullText: String,
    localToScreen: (Offset) -> Offset,
): SelectedText? {
    val charOffset = layoutResult.getOffsetForPosition(tapPos)
    val wordRange = layoutResult.getWordBoundary(charOffset)

    if (wordRange.start >= wordRange.end) return null

    val word = fullText.substring(wordRange.start, wordRange.end)
    if (word.isBlank()) return null

    val localBounds = (wordRange.start until wordRange.end)
        .map { layoutResult.getBoundingBox(it) }
        .reduce(Rect::union)

    val screenTopLeft = localToScreen(Offset(localBounds.left, localBounds.top))
    val screenBottomRight = localToScreen(Offset(localBounds.right, localBounds.bottom))

    return SelectedText(
        index = wordRange.start,
        word = word,
        wordRange = wordRange,
        bounds = Rect(screenTopLeft, screenBottomRight),
    )
}