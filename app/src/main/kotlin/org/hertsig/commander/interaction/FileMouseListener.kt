package org.hertsig.commander.interaction

import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import org.hertsig.commander.ui.FolderPanel
import java.awt.Desktop
import java.nio.file.Path

class FileMouseListener(
    ui: FolderPanel,
    file: Path,
    focusRequester: FocusRequester,
    showContextMenu: MutableState<Boolean>
) : PathMouseListener(ui, file, focusRequester, showContextMenu) {

    override fun onDoublePrimary(modifiers: PointerKeyboardModifiers) {
        Desktop.getDesktop().open(path.toFile())
    }
}
