package dev.buecherregale.ebook_reader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.buecherregale.ebook_reader.core.config.AppThemeSetting
import dev.buecherregale.ebook_reader.supportsDynamicColorScheme
import dev.buecherregale.ebook_reader.ui.navigation.Navigator
import dev.buecherregale.ebook_reader.ui.viewmodel.SettingsViewModel
import ebuecherregal.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.ExperimentalUuidApi

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val navigator = koinInject<Navigator>()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            try {
                snackbarHostState.showSnackbar(it)
            } finally {
                viewModel.clearError()
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            try {
                snackbarHostState.showSnackbar(it)
            } finally {
                viewModel.clearMessage()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(painterResource(Res.drawable.arrow_back_24px), contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveSettings() },
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = LocalContentColor.current
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font Size")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { viewModel.setFontSize(state.fontSize - 1) }) {
                                Icon(
                                    painter = painterResource(Res.drawable.remove_24px),
                                    contentDescription = "Decrease Font Size"
                                )
                            }

                            OutlinedTextField(
                                value = state.fontSize.toInt().toString(),
                                onValueChange = {
                                    val newSize = it.toFloatOrNull()
                                    if (newSize != null) {
                                        viewModel.setFontSize(newSize)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(50.dp),
                                singleLine = true
                            )

                            IconButton(onClick = { viewModel.setFontSize(state.fontSize + 1) }) {
                                Icon(
                                    painter = painterResource(Res.drawable.add_24px),
                                    contentDescription = "Increase Font Size"
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("App Theme")

                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                    .width(150.dp),
                                readOnly = true,
                                value = when (state.theme) {
                                    AppThemeSetting.LIGHT -> "Light"
                                    AppThemeSetting.DARK -> "Dark"
                                    AppThemeSetting.SYSTEM -> "System"
                                    AppThemeSetting.DYNAMIC -> "Dynamic"
                                },
                                onValueChange = {},
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                AppThemeSetting.entries.filter { it.available() }.forEach { theme ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                when (theme) {
                                                    AppThemeSetting.LIGHT -> "Light"
                                                    AppThemeSetting.DARK -> "Dark"
                                                    AppThemeSetting.SYSTEM -> "System"
                                                    AppThemeSetting.DYNAMIC -> "Dynamic"
                                                }
                                            )
                                        },
                                        onClick = {
                                            viewModel.setTheme(theme)
                                            expanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Download Dictionary", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    DownloadDictionarySection(
                        supportedDictionaries = state.supportedDictionaries,
                        onDownload = { name, lang -> viewModel.downloadDictionary(name, lang) }
                    )
                }

                item {
                    Text("Active Dictionaries", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                }

                val groupedDictionaries = state.downloadedDictionaries.groupBy { it.originalLanguage }

                items(groupedDictionaries.keys.toList()) { lang ->
                    val dicts = groupedDictionaries[lang] ?: emptyList()
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Language: ${lang.toLanguageTag()}", style = MaterialTheme.typography.titleSmall)
                            dicts.forEach { dict ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = state.activeDictionaryIds[lang] == dict.id,
                                        onClick = { viewModel.setActiveDictionary(lang, dict.id) }
                                    )
                                    Text(
                                        text = "${dict.name} (${dict.targetLanguage})",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.deleteDictionary(dict.id) }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.delete_24px),
                                            contentDescription = "Delete Dictionary"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DownloadDictionarySection(
    supportedDictionaries: List<String>,
    onDownload: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedDictionary by remember { mutableStateOf(if (supportedDictionaries.isNotEmpty()) supportedDictionaries[0] else "") }
    var languageTag by remember { mutableStateOf("") }

    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth(),
                readOnly = true,
                value = selectedDictionary,
                onValueChange = {},
                label = { Text("Dictionary Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                supportedDictionaries.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            selectedDictionary = selectionOption
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = languageTag,
            onValueChange = { languageTag = it },
            label = { Text("Language Tag (e.g. en, de)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (selectedDictionary.isNotEmpty() && languageTag.isNotEmpty()) {
                    onDownload(selectedDictionary, languageTag)
                }
            },
            enabled = selectedDictionary.isNotEmpty() && languageTag.isNotEmpty()
        ) {
            Text("Download")
        }
    }
}

private fun AppThemeSetting.available(): Boolean {
    return when (this) {
        AppThemeSetting.DYNAMIC -> supportsDynamicColorScheme()
        else -> true
    }
}