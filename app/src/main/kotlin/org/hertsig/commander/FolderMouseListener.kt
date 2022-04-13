package org.hertsig.commander

import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import java.nio.file.Path

class FolderMouseListener(
    ui: FolderUI,
    folder: Path,
    focusRequester: FocusRequester,
    showContextMenu: MutableState<Boolean>
) : PathMouseListener(ui, folder, focusRequester, showContextMenu) {

    override fun onDoublePrimary(modifiers: PointerKeyboardModifiers) {
        ui.setCurrentFolder(path)
    }
}
