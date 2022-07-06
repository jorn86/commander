package org.hertsig.commander

import androidx.compose.ui.window.application
import org.hertsig.commander.interaction.ApplicationWindow
import org.slf4j.LoggerFactory

object App {
    private val log = LoggerFactory.getLogger(App::class.java)

    fun run() {
        Thread.setDefaultUncaughtExceptionHandler { _, e -> log.error("Uncaught exception", e) }
        application { ApplicationWindow() }
    }
}

fun main() = App.run()
