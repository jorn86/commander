package org.hertsig.commander

import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import org.hertsig.mouse.MouseListener
import java.nio.file.Path

open class PathMouseListener(
    protected val ui: FolderUI,
    protected val path: Path,
    val focusRequester: FocusRequester,
    val showContextMenu: MutableState<Boolean>,
): MouseListener() {
    override fun onPrimary(modifiers: PointerKeyboardModifiers) {
        if (modifiers.isShiftPressed) {
            ui.selectRange(path)
        } else if (!path.endsWith("..")) {
            ui.select(path.normalize(), modifiers.isCtrlPressed)
        }
        focusRequester.requestFocus()
    }

    override fun onSecondary(modifiers: PointerKeyboardModifiers) {
        if (!ui.isSelected(path) && !path.endsWith("..")) {
            ui.select(path.normalize(), false)
        }
        showContextMenu.value = true
    }

    override fun onForward(modifiers: PointerKeyboardModifiers) = ui.forward()
    override fun onBack(modifiers: PointerKeyboardModifiers) = ui.back()
}