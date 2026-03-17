package dev.buecherregale.ebook_reader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.buecherregale.ebook_reader.core.domain.Library
import dev.buecherregale.ebook_reader.ui.components.LibraryCard
import dev.buecherregale.ebook_reader.ui.dialog.CreateLibraryDialog
import dev.buecherregale.ebook_reader.ui.dialog.EditLibraryDialog
import dev.buecherregale.ebook_reader.ui.navigation.Navigator
import dev.buecherregale.ebook_reader.ui.navigation.Screen
import dev.buecherregale.ebook_reader.ui.viewmodel.LibraryViewModel
import ebuecherregal.composeapp.generated.resources.Res
import ebuecherregal.composeapp.generated.resources.add_24px
import ebuecherregal.composeapp.generated.resources.settings_24px
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.ExperimentalUuidApi

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
fun LibraryScreen(
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<Library?>(null) }
    val navigator = koinInject<Navigator>()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadLibraries()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Libraries") },
                actions = {
                    IconButton(onClick = { navigator.push(Screen.Settings) }) {
                        Icon(painterResource(Res.drawable.settings_24px), contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                val painter = painterResource(Res.drawable.add_24px)
                Icon(painter, contentDescription = "Add Library")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(state.libraries) { library ->
                    LibraryCard(
                        library = library,
                        onRename = { showRenameDialog = it },
                        onDelete = { viewModel.deleteLibrary(it.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateLibraryDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.createLibrary(name)
                showCreateDialog = false
            }
        )
    }

    showRenameDialog?.let { library ->
        EditLibraryDialog(
            library = library,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newName ->
                viewModel.renameLibrary(library.id, newName)
                showRenameDialog = null
            }
        )
    }
}
