package dev.buecherregale.ebook_reader.ui.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.buecherregale.ebook_reader.core.domain.Library

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditLibraryDialog(
    library: Library,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(library.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Library") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Library Name") }
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
