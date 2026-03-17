package dev.buecherregale.ebook_reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.ui.NavDisplay
import dev.buecherregale.ebook_reader.core.config.SettingsManager
import dev.buecherregale.ebook_reader.core.language.dictionaries.DictionaryImporterFactory
import dev.buecherregale.ebook_reader.core.language.dictionaries.jmdict.JMDictImporter
import dev.buecherregale.ebook_reader.ui.navigation.Navigator
import dev.buecherregale.ebook_reader.ui.navigation.navigationModule
import dev.buecherregale.ebook_reader.ui.theming.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

const val APP_NAME = "EBuecherregal"

@Composable
@OptIn(KoinExperimentalAPI::class)
fun App() {
    KoinApplication(
        configuration = koinConfiguration {
            modules(navigationModule, commonModule, platformModule())
        }
    ) {
        registerImplsInFactory()

        val settingsManager = koinInject<SettingsManager>()

        settingsManager.loadOrCreateBlocking()

        LaunchedEffect(Unit) {
            settingsManager.loadDictionaries()
        }

        val theme by settingsManager.theme.collectAsState()

        AppTheme(theme) {
            NavDisplay(
                backStack = koinInject<Navigator>().backStack,
                entryProvider = koinEntryProvider()
            )
        }
    }
}

fun registerImplsInFactory() {
    DictionaryImporterFactory.register("JmDict", ::JMDictImporter)
}
