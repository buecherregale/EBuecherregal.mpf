package dev.buecherregale.ebook_reader.ui.theming

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dev.buecherregale.ebook_reader.core.config.AppThemeSetting
import dev.buecherregale.ebook_reader.dynamicColorSchemeDark
import dev.buecherregale.ebook_reader.dynamicColorSchemeLight
import dev.buecherregale.ebook_reader.supportsDynamicColorScheme

/**
 * Chooses the general AppTheme from the [AppThemeSetting].
 *
 * @param theme     The user configured setting regarding the theme.
 * @param content   The composable content to apply the theme to.
 */
@Composable
fun AppTheme(theme: AppThemeSetting = AppThemeSetting.SYSTEM, content: @Composable () -> Unit) {
    when (theme) {
        AppThemeSetting.LIGHT -> ShellTheme(false, content)
        AppThemeSetting.DARK -> ShellTheme(true, content)
        AppThemeSetting.SYSTEM -> ShellTheme(isSystemInDarkTheme(), content)
        AppThemeSetting.DYNAMIC ->
            when (supportsDynamicColorScheme()) {
                false -> ShellTheme(isSystemInDarkTheme(), content)
                true -> MaterialTheme(
                    colorScheme = if (isSystemInDarkTheme()) dynamicColorSchemeDark() else dynamicColorSchemeLight(),
                    content = content
                )
            }
    }
}

/**
 * Applies a [MaterialTheme] to the content, using [ShellDarkColorScheme] or [ShellLightColorScheme].
 * These themes are inspired by my (desktop) shell.
 *
 * @param isDarkTheme   If the dark or light colors should be used.
 * @param content       The composable content.
 */
@Composable
fun ShellTheme(isDarkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) ShellDarkColorScheme else ShellLightColorScheme,
        content = content
    )
}
