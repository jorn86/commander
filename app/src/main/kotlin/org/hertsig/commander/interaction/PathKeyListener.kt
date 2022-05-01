package org.hertsig.commander.interaction

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import org.hertsig.commander.ui.FolderPanel
import java.awt.Desktop
import java.nio.file.Path

@OptIn(ExperimentalComposeUiApi::class)
class PathKeyListener(
    private val ui: FolderPanel,
    private val path: Path,
) : (KeyEvent) -> Boolean {
    override fun invoke(event: KeyEvent): Boolean {
        if (event.type == KeyEventType.KeyDown) {
            when (event.key) {
                Key.Enter -> Desktop.getDesktop().open(path.toFile())
                Key.Spacebar -> ui.select(path, event.isCtrlPressed)
            }
        }
        else if (event.type == KeyEventType.KeyUp && !event.isCtrlPressed) {
            // Key up event happens after focus change
            when (event.key) {
                Key.DirectionUp,
                Key.DirectionDown -> ui.select(path, event.isShiftPressed)
            }
        }
        return false
    }
}
