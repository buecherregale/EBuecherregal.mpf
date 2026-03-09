package dev.buecherregale.ebook_reader.ui.dom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import dev.buecherregale.ebook_reader.core.dom.Image
import dev.buecherregale.ebook_reader.core.dom.ImageBlock
import dev.buecherregale.ebook_reader.core.dom.Text
import dev.buecherregale.ebook_reader.core.domain.Book
import dev.buecherregale.ebook_reader.core.service.BookService
import dev.buecherregale.ebook_reader.ui.components.rememberImageBitmap
import dev.buecherregale.ebook_reader.ui.util.UrlUtil
import org.koin.compose.koinInject

/**
 * Renders a bare [Image] node.
 *
 * The [Image.alt] text is used as a content description for accessibility.
 */
@Composable
fun DomImage(
    book: Book,
    image: Image,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    bookService: BookService = koinInject()
) {
    val bitmap by rememberImageBitmap(image.src) {
        if (UrlUtil.getUrlProtocol(image.src) == UrlUtil.BOOK_RESOURCE_PROTOCOL)
            bookService.bookResourceRepository(book.id).load(UrlUtil.getUrlPath(image.src))
        else
            null
    }
    bitmap?.let { bitmap ->
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = image.alt,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

/**
 * Renders an [ImageBlock] as a [Column] of the image with an optional caption *below*.
 */
@Composable
fun DomImageBlock(
    book: Book,
    block: ImageBlock,
    config: RenderingConfig = RenderingConfig.Default,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        DomImage(book = book, image = block.image, config = config)

        if (block.caption.isNotEmpty()) {
            Spacer(modifier = Modifier.height(config.listItemSpacing))
            val captionConfig = config.copy(baseTextSize = (config.baseTextSize.value * 0.85f).sp)
            InlineContentRenderer(
                nodes = block.caption.filterIsInstance<Text>(),
                config = captionConfig,
            )
        }
    }
}