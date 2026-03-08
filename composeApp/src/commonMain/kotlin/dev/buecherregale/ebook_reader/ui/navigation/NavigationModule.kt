package dev.buecherregale.ebook_reader.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import dev.buecherregale.ebook_reader.ui.screens.LibraryDetailScreen
import dev.buecherregale.ebook_reader.ui.screens.LibraryScreen
import dev.buecherregale.ebook_reader.ui.screens.ReaderScreen
import dev.buecherregale.ebook_reader.ui.screens.SettingsScreen
import dev.buecherregale.ebook_reader.ui.viewmodel.LibraryDetailViewModel
import dev.buecherregale.ebook_reader.ui.viewmodel.LibraryViewModel
import dev.buecherregale.ebook_reader.ui.viewmodel.ReaderViewModel
import dev.buecherregale.ebook_reader.ui.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import kotlin.uuid.ExperimentalUuidApi

class Navigator(start: Screen) {
    private val _backStack = mutableStateListOf<Screen>().apply {
        add(start)
    }

    val backStack: SnapshotStateList<Screen> get() = _backStack

    fun push(screen: Screen) = _backStack.add(screen)

    fun pop() {
        if (_backStack.size > 1) _backStack.removeLast()
    }
}

@OptIn(KoinExperimentalAPI::class, ExperimentalUuidApi::class)
val navigationModule = module {
    single { Navigator(Screen.LibraryOverview) }

    viewModelOf(::LibraryViewModel)
    viewModel { params -> LibraryDetailViewModel(library = params.get(), get(), get()) }
    viewModel { params -> ReaderViewModel(book = params.get(), get(), get()) }
    viewModelOf(::SettingsViewModel)

    navigation<Screen.LibraryOverview> { _ ->
        LibraryScreen(viewModel = koinViewModel())
    }
    navigation<Screen.LibraryDetail> { route ->
        LibraryDetailScreen(
            library = route.library,
            viewModel = koinViewModel(
                key = route.library.id.toString(),
            ) {
                parametersOf(route.library)
            }
        )
    }
    navigation<Screen.Reader> { route ->
        val viewModel = koinViewModel<ReaderViewModel>(
            key = route.book.id.toString()
        ) {
            parametersOf(route.book)
        }
        ReaderScreen(
            viewModel = viewModel,
        )
    }
    navigation<Screen.Settings> {
        SettingsScreen(viewModel = koinViewModel())
    }
}