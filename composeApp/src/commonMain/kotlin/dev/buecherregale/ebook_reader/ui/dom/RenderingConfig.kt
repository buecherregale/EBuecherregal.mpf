package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Single configuration point for rendering library nodes.
 * [dev.buecherregale.ebook_reader.core.dom.Node] will pass this config down to every single node rendered.
 *
 * The default values are very basic.
 *
 * @param baseTextSize          Body text size used for [dev.buecherregale.ebook_reader.core.dom.TextStyle] nodes.
 * @param codeTextSize          Font size for inline `<code>` text.
 * @param lineHeightScale       Line height as a multiple of the current font size.
 * @param paragraphSpacing      Vertical space added below each paragraph / block.
 * @param listItemSpacing       Vertical space between list items.
 * @param listIndent            Horizontal indent per nesting level inside lists.
 * @param bodyFontFamily        Font family for body text. Null = system default.
 * @param codeFontFamily        Font family for code text. Null = system monospace.
 * @param headingFontFamily     Font family for headings. Null = same as [bodyFontFamily].
 * @param linkColor             Color used for link text. Null = inherit current color.
 * @param rubyAnnotationScale   Font size of ruby annotation relative to base text.
 */
data class RenderingConfig(
    val baseTextSize: TextUnit = 16.sp,
    val codeTextSize: TextUnit = 14.sp,
    val lineHeightScale: Float = 1.5f,
    val paragraphSpacing: Dp = 12.dp,
    val listItemSpacing: Dp = 4.dp,
    val listIndent: Dp = 16.dp,
    val bodyFontFamily: FontFamily? = null,
    val codeFontFamily: FontFamily = FontFamily.Monospace,
    val headingFontFamily: FontFamily? = bodyFontFamily,
    val linkColor: Color = Color.Blue,
    val rubyAnnotationScale: Float = 0.8f,
) {

    @Composable
    fun headingSize(level: Int): TextStyle {
        return when (level) {
            1 -> MaterialTheme.typography.headlineLarge
            2 -> MaterialTheme.typography.headlineMedium
            3 -> MaterialTheme.typography.headlineSmall
            4 -> MaterialTheme.typography.titleLarge
            5 -> MaterialTheme.typography.titleMedium
            6 -> MaterialTheme.typography.titleSmall
            else -> MaterialTheme.typography.titleSmall

        }
    }

    fun baseAsTextStyle(): TextStyle {
        return TextStyle(
            fontSize = baseTextSize,
            fontFamily = bodyFontFamily,
        )
    }

    companion object {
        val Default = RenderingConfig()
    }
}