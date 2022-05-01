package org.hertsig.commander.interaction

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.*
import org.hertsig.commander.ui.FolderPanel
import org.slf4j.LoggerFactory

@OptIn(ExperimentalComposeUiApi::class)
class GlobalKeyboardListener(
    private val ui: FolderPanel,
    private val focusManager: FocusManager,
    private val focusDirectionForTab: FocusDirection,
) : (KeyEvent) -> Boolean {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun invoke(event: KeyEvent): Boolean {
        if (event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.Backspace -> ui.back()
                Key.Delete -> ui.onDelete()
                Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                Key.DirectionLeft -> focusManager.moveFocus(FocusDirection.Left)
                Key.DirectionRight -> focusManager.moveFocus(FocusDirection.Right)
            }
        }
        return false
    }
}
