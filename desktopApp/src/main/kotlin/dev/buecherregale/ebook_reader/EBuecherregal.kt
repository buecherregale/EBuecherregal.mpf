package dev.buecherregale.ebook_reader

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import kotlinx.coroutines.DelicateCoroutinesApi
import org.jetbrains.compose.resources.painterResource
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi

@OptIn(DelicateCoroutinesApi::class, ExperimentalUuidApi::class)
fun main() = application {
    Logger.setMinSeverity(Severity.Verbose)
    Logger.setLogWriters(
        Slf4JLogWriter()
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = APP_NAME,
        icon = painterResource(icon())
    ) {
        App()
    }
}

private class Slf4JLogWriter : LogWriter() {
    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?
    ) {
        val logger = LoggerFactory.getLogger(tag)
        when (severity) {
            Severity.Verbose -> logger.trace(message, throwable)
            Severity.Debug -> logger.debug(message, throwable)
            Severity.Info -> logger.info(message, throwable)
            Severity.Warn -> logger.warn(message, throwable)
            Severity.Error -> logger.error(message, throwable)
            else -> logger.trace(message, throwable)
        }
    }

}