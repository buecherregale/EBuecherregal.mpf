@file:OptIn(ExperimentalUuidApi::class)

package dev.buecherregale.ebook_reader.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import dev.buecherregale.ebook_reader.core.config.SettingsManager
import dev.buecherregale.ebook_reader.core.dom.Chapter
import dev.buecherregale.ebook_reader.core.dom.LinkTarget
import dev.buecherregale.ebook_reader.ui.components.BlockRenderer
import dev.buecherregale.ebook_reader.ui.components.SelectedText
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun ChapterView(
    bookId: Uuid,
    chapter: Chapter,
    scrollState: ScrollState,
    selectedRange: TextRange? = null,
    selectedBlockId: String? = null,
    onSelected: (SelectedText, String) -> Unit = { _, _ -> },
    onLinkClick: (LinkTarget) -> Unit = {},
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
) {
    var swipeDistance by remember { mutableStateOf(0f) }
    val settingsManager = koinInject<SettingsManager>()
    val fontSize by settingsManager.state.fontSize.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            swipeDistance > 200 -> onSwipeRight()
                            swipeDistance < -200 -> onSwipeLeft()
                        }
                        swipeDistance = 0f
                    },
                    onDragCancel = {
                        swipeDistance = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeDistance += dragAmount
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        chapter.blocks.forEach { block ->
            BlockRenderer(bookId, block, fontSize, selectedRange, selectedBlockId, onSelected, onLinkClick)
        }
    }
}
