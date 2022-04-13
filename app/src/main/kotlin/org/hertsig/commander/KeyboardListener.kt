package org.hertsig.commander

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.*
import org.slf4j.LoggerFactory


@OptIn(ExperimentalComposeUiApi::class)
class KeyboardListener(
    private val ui: FolderUI,
    private val focusManager: FocusManager,
    private val focusDirectionForTab: FocusDirection,
) : (KeyEvent) -> Boolean {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun invoke(event: KeyEvent): Boolean {
        if (event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.Backspace -> ui.back()
                Key.Delete -> ui.onDelete()
                Key.DirectionUp -> move(FocusDirection.Up, event.isShiftPressed)
                Key.DirectionDown -> move(FocusDirection.Down, event.isShiftPressed)
                Key.DirectionLeft -> focusManager.moveFocus(FocusDirection.Left)
                Key.DirectionRight -> focusManager.moveFocus(FocusDirection.Right)
                Key.Tab -> onTab()
                Key.Spacebar -> selectCurrentFocused()
            }
        }
        return false
    }

    private fun onTab() {
        focusManager.moveFocus(focusDirectionForTab)
        focusManager.moveFocus(FocusDirection.Down)
    }

    private fun move(direction: FocusDirection, select: Boolean) {
        focusManager.moveFocus(direction)
        if (select) selectCurrentFocused(true)
    }

    private fun selectCurrentFocused(addOnly: Boolean = false) {
        log.debug("Want to select focused line but don't know how")
    }
}
