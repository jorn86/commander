package org.hertsig.commander

import androidx.compose.ui.window.application
import org.hertsig.commander.interaction.ApplicationWindow
import org.slf4j.LoggerFactory

object App {
    internal val log = LoggerFactory.getLogger(App::class.java)
}

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { _, e -> App.log.error("Uncaught exception", e) }
    application { ApplicationWindow() }
}
