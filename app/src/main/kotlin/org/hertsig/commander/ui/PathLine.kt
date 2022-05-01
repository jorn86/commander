package org.hertsig.commander.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CursorDropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.hertsig.commander.core.fileType
import org.hertsig.commander.core.formatSize
import org.hertsig.commander.core.setClipboard
import org.hertsig.commander.interaction.FileMouseListener
import org.hertsig.commander.interaction.FolderMouseListener
import org.hertsig.commander.interaction.PathKeyListener
import org.hertsig.commander.interaction.PathMouseListener
import org.hertsig.commander.ui.component.FileIcon
import org.hertsig.commander.ui.component.SmallDropdownMenuItem
import org.hertsig.commander.ui.component.TooltipText
import org.hertsig.commander.util.applyIf
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.moveTo

class PathLine(private val parent: FolderPanel) {
    private val log = LoggerFactory.getLogger(FolderPanel::class.java)

    @Composable
    fun FileLine(file: Path) {
        val focusRequester = remember { FocusRequester() }
        val showContextMenu = remember { mutableStateOf(false) }
        val listener = remember { FileMouseListener(parent, file, focusRequester, showContextMenu) }
        PathLine(file, listener) {
            FileIcon(file)
            TooltipText(file.fileName.toString(), Modifier.weight(nameWeight).fillMaxWidth()) {
                Text(file.fileName.toString(), softWrap = false, maxLines = 1, overflow = TextOverflow.Clip)
            }
            Text(formatSize(file.fileSize()), Modifier.weight(sizeWeight).fillMaxWidth(), textAlign = TextAlign.Right)
            val type = fileType(file) ?: ""
            TooltipText(type, Modifier.weight(typeWeight).fillMaxWidth()) {
                Text(type, softWrap = false, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
    }

    @Composable
    fun FolderLine(folder: Path) {
        val focusRequester = remember { FocusRequester() }
        val showContextMenu = remember { mutableStateOf(false) }
        val listener = remember(folder) { FolderMouseListener(parent, folder, focusRequester, showContextMenu) }
        PathLine(folder, listener) {
            FileIcon(folder)
            TooltipText(folder.fileName.toString()) {
                Text(folder.fileName.toString(), softWrap = false, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun PathLine(
        path: Path,
        listener: PathMouseListener,
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit,
    ) {
        val source = remember { MutableInteractionSource() }
        val focused by source.collectIsFocusedAsState()
        val hovered by source.collectIsHoveredAsState()

        Row(modifier.fillMaxWidth()
            .focusRequester(listener.focusRequester)
            .focusable(interactionSource = source)
            .hoverable(source)
            .onKeyEvent(PathKeyListener(parent, path))
            .mouseClickable(onClick = listener)) {

            Row(modifier.fillMaxWidth()
                // TODO dashed border (currently unsupported)
                .applyIf(focused) { border(1.dp, MaterialTheme.colors.secondary) }
                .background(background(parent.isSelected(path), hovered)),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                content = content,
            )
            PathDropdownMenu(listener, path)
        }
    }

    companion object {
        private const val nameWeight = 15f
        private const val sizeWeight = 3f
        private const val typeWeight = 4f
    }

    @Composable
    private fun PathDropdownMenu(listener: PathMouseListener, path: Path) {
        CursorDropdownMenu(listener.showContextMenu.value, { listener.showContextMenu.value = false }) {
            SmallDropdownMenuItem("Copy path", listener.showContextMenu) {
                setClipboard(path.normalize().toString())
            }
            SmallDropdownMenuItem("Move to parent", listener.showContextMenu, path.parent !in FolderPanel.roots) {
                val target = path.parent.parent.resolve(path.fileName)
                log.debug("Moving $path to $target")
                path.moveTo(target)
                parent.forceReload()
            }
            SmallDropdownMenuItem("Delete", listener.showContextMenu, !path.endsWith("..")) {
                if (path.toFile().deleteRecursively()) {
                    log.debug("Deleted ${path.normalize()}")
                } else {
                    log.warn("Failed to delete ${path.normalize()}")
                }
                parent.forceReload()
            }
        }
    }

    @Composable
    private fun background(selected: Boolean, hovered: Boolean) = when {
        selected -> MaterialTheme.colors.secondary
        hovered -> MaterialTheme.colors.secondaryVariant
        else -> MaterialTheme.colors.background
    }
}
