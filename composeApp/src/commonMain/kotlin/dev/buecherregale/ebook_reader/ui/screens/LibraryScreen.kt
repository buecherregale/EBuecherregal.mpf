package dev.buecherregale.ebook_reader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.buecherregale.ebook_reader.core.domain.Library
import dev.buecherregale.ebook_reader.ui.components.BookCard
import dev.buecherregale.ebook_reader.ui.dialog.ImportBookDialog
import dev.buecherregale.ebook_reader.ui.navigation.Navigator
import dev.buecherregale.ebook_reader.ui.navigation.Screen
import dev.buecherregale.ebook_reader.ui.viewmodel.LibraryDetailViewModel
import ebuecherregal.composeapp.generated.resources.Res
import ebuecherregal.composeapp.generated.resources.add_24px
import ebuecherregal.composeapp.generated.resources.arrow_back_24px
import ebuecherregal.composeapp.generated.resources.settings_24px
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.uuid.ExperimentalUuidApi

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
fun LibraryDetailScreen(
    library: Library,
    viewModel: LibraryDetailViewModel = currentKoinScope().get(parameters = { parametersOf(library) })
) {
    val state by viewModel.uiState.collectAsState()
    var showImportBookDialog by remember { mutableStateOf(false) }
    val navigator: Navigator = koinInject()

    LaunchedEffect(Unit) {
        viewModel.loadBooks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(library.name) },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(painterResource(Res.drawable.arrow_back_24px), contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navigator.push(Screen.Settings) }) {
                        Icon(painterResource(Res.drawable.settings_24px), contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showImportBookDialog = true }) {
                Icon(painterResource(Res.drawable.add_24px), contentDescription = "Import Book")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(state.books) { book ->
                    BookCard(bookService = koinInject(), book = book)
                }
            }
        }
    }
    if (showImportBookDialog) {
        ImportBookDialog(
            onDismiss = { showImportBookDialog = false },
            onConfirm = { path ->
                viewModel.importBook(path!!)
                showImportBookDialog = false
            }
        )
    }
}