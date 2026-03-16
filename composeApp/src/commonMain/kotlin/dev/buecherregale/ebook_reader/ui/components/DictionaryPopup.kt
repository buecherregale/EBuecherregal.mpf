package dev.buecherregale.ebook_reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.buecherregale.ebook_reader.core.domain.Dictionary
import dev.buecherregale.ebook_reader.core.service.DictionaryService
import dev.buecherregale.ebook_reader.ui.dom.SelectedText
import org.koin.compose.koinInject


@Stable
class PopupState {
    var text by mutableStateOf<String?>(null)
    var bounds by mutableStateOf<Rect?>(null)
    var selectedRange by mutableStateOf<TextRange?>(null)

    fun show(selectedText: SelectedText) {
        text = selectedText.word
        bounds = selectedText.bounds
        selectedRange = selectedText.wordRange
    }

    fun dismiss() {
        text = null
        bounds = null
        selectedRange = null
    }

    val isVisible: Boolean
        get() = text != null
}

@Composable
fun rememberPopupState(): PopupState =
    remember { PopupState() }

@Composable
fun DictionaryPopup(
    state: PopupState,
    dictionary: Dictionary,
    dictionaryService: DictionaryService = koinInject()
) {
    if (!state.isVisible || state.bounds == null) return

    val popupPositionProvider = remember(state.bounds) {
        val widgetBounds = state.bounds!!
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                var x = widgetBounds.left.toInt()
                var y = (widgetBounds.top - popupContentSize.height).toInt()

                if (y < 0) {
                    y = widgetBounds.bottom.toInt()
                }

                if (x + popupContentSize.width > windowSize.width) {
                    x = windowSize.width - popupContentSize.width
                }
                if (x < 0) {
                    x = 0
                }

                return IntOffset(x, y)
            }
        }
    }

    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = { state.dismiss() },
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            val entry = dictionaryService.lookup(dictionary, state.text!!).firstOrNull()
            if (entry == null) {
                Text(
                    text = "No definition found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = entry.reading,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = entry.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.partsOfSpeech.isNotEmpty()) {
                    Text(
                        text = entry.partsOfSpeech.joinToString(separator = " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
