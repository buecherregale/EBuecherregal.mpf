package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
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
import co.touchlab.kermit.Logger
import dev.buecherregale.ebook_reader.core.dom.*
import dev.buecherregale.ebook_reader.core.dom.TextStyle
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.getWordBoundaryAt

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
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
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
    book: Book,
    link: Link,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    InlineContentRenderer(
        book = book,
        nodes = listOf(link),
        config = config,
        modifier = modifier,
        onTextSelected = onTextSelected,
    )
}

/**
 * Renders a [Ruby] node.
 *
 * @see [InlineContentRenderer]
 */
@Composable
fun DomRuby(
    book: Book,
    ruby: Ruby,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    InlineContentRenderer(
        book = book,
        nodes = listOf(ruby),
        config = config,
        modifier = modifier,
        onTextSelected = onTextSelected,
    )
}

/**
 * A function that when called, should dismiss the current highlighted section.
 */
typealias HighlightDismisser = () -> Unit

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
    book: Book,
    nodes: List<Node>,
    config: RenderingConfig,
    modifier: Modifier = Modifier,
    onTextSelected: ((SelectedText, HighlightDismisser) -> Unit) = { _, _ -> },
) {
    val uriHandler = LocalUriHandler.current
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var highlightedRange by remember { mutableStateOf<TextRange?>(null) }
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val dismissHighlight = { highlightedRange = null }

    val annotated = remember(nodes, highlightedRange) {
        buildAnnotatedString {
            nodes.forEach { appendInlineNode(it, config) }
            highlightedRange?.let { range ->
                addStyle(
                    style = SpanStyle(background = highlightColor),
                    start = range.start,
                    end = range.end,
                )
            }
        }
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
                detectTapGestures(
                    onTap = { tapPos ->
                        val lr = layoutResult.value ?: return@detectTapGestures
                        val offset = lr.getOffsetForPosition(tapPos)

                        val link = annotated
                            .getStringAnnotations(LINK_TAG, offset, offset)
                            .firstOrNull()
                        if (link != null) {
                            Logger.d { "opening link: ${link.item}" }
                            highlightedRange = null
                            uriHandler.openUri(link.item)
                            return@detectTapGestures
                        }

                        val coords = coordinates ?: return@detectTapGestures
                        val wordRange =
                            getWordBoundaryAt(annotated.text, offset, book.metadata.language)
                                ?: return@detectTapGestures

                        if (wordRange.start >= wordRange.end
                            || annotated.text.substring(wordRange.start, wordRange.end).isBlank()
                        ) {
                            highlightedRange = null
                            return@detectTapGestures
                        }

                        highlightedRange = if (highlightedRange == TextRange(wordRange.start, wordRange.end)) {
                            null
                        } else {
                            TextRange(wordRange.start, wordRange.end)
                        }

                        resolveSelectedText(
                            layoutResult = lr,
                            fullText = annotated.text,
                            wordRange = wordRange,
                            localToScreen = coords::localToWindow,
                        )?.let {
                            Logger.d { "selected text: $it" }
                            onTextSelected(it, dismissHighlight)
                        }
                    },
                    onPress = {
                        if (tryAwaitRelease()) { /* consumed */
                        }
                    },
                )
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
 * Resolves the word in the [wordRange] inside [layoutResult] and returns a
 * [SelectedText] with screen-space bounds, or `null` if the tap didn't
 * land on a non-blank word.
 *
 * @param layoutResult     The [TextLayoutResult] from the [Text] composable.
 * @param fullText         The plain [String] backing the annotated string.
 * @param wordRange        The [TextRange] containing the word to select.
 * @param localToScreen    Converts a local [Offset] to screen coordinates.
 *                         Supply `LayoutCoordinates::localToWindow`.
 */
private fun resolveSelectedText(
    layoutResult: TextLayoutResult,
    fullText: String,
    wordRange: TextRange,
    localToScreen: (Offset) -> Offset,
): SelectedText? {

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