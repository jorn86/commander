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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.hertsig.commander.core.formatSize
import org.hertsig.commander.core.setClipboard
import org.hertsig.commander.interaction.FileMouseListener
import org.hertsig.commander.interaction.FolderMouseListener
import org.hertsig.commander.interaction.PathKeyListener
import org.hertsig.commander.interaction.PathMouseListener
import org.hertsig.commander.ui.component.SmallDropdownMenuItem
import org.hertsig.commander.ui.component.TooltipText
import org.hertsig.commander.ui.state.FileState
import org.hertsig.commander.ui.state.Folder
import org.hertsig.commander.ui.state.RegularFile
import org.hertsig.commander.util.applyIf
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.moveTo

class PathLine(private val parent: FolderPanel) {
    private val log = LoggerFactory.getLogger(FolderPanel::class.java)

    @Composable
    fun FileLine(data: RegularFile) {
        val listener = remember(data.path) { FileMouseListener(parent, data.path, data.focusRequester, data.showContextMenu) }

        PathLine(data, listener) {
            data.icon?.let { Image(it.toPainter(), "icon") }
            TooltipText("${data.name} - ${formatDate(data.lastModified)}", Modifier.weight(nameWeight).fillMaxWidth()) {
                Text(data.name, softWrap = false, maxLines = 1, overflow = TextOverflow.Clip)
            }
            Text(formatSize(data.size), Modifier.weight(sizeWeight).fillMaxWidth(), textAlign = TextAlign.Right)
            TooltipText(data.type, Modifier.weight(typeWeight).fillMaxWidth()) {
                Text(data.type, softWrap = false, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
    }

    @Composable
    fun FolderLine(data: Folder) {
        val listener = remember(data.path) { FolderMouseListener(parent, data.path, data.focusRequester, data.showContextMenu) }
        PathLine(data, listener) {
            data.icon?.let { Image(it.toPainter(), "icon") }
            TooltipText(data.name) {
                Text(data.name, softWrap = false, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun PathLine(
        data: FileState,
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
            .onKeyEvent(PathKeyListener(parent, data.path))
            .mouseClickable(onClick = listener)) {

            Row(modifier.fillMaxWidth()
                // TODO dashed border (currently unsupported)
                .applyIf(focused) { border(1.dp, MaterialTheme.colors.secondary) }
                .background(background(parent.isSelected(data.path), hovered)),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                content = content,
            )
            PathDropdownMenu(data.showContextMenu, data.path)
        }
    }

    @Composable
    private fun PathDropdownMenu(showContextMenu: MutableState<Boolean>, path: Path) {
        CursorDropdownMenu(showContextMenu.value, { showContextMenu.value = false }) {
            SmallDropdownMenuItem("Copy path", showContextMenu) {
                setClipboard(path.normalize().toString())
            }
            SmallDropdownMenuItem("Move to parent", showContextMenu, path.parent !in parent.roots.roots) {
                val target = path.parent.parent.resolve(path.fileName)
                log.debug("Moving $path to $target")
                path.moveTo(target)
            }
            SmallDropdownMenuItem("Move to other panel", showContextMenu, path.parent !in parent.roots.roots) {
                val target = parent.other.current.value.resolve(path.fileName)
                log.debug("Moving $path to $target")
                path.moveTo(target)
            }
            SmallDropdownMenuItem("Delete", showContextMenu, !path.endsWith("..")) {
                if (path.toFile().deleteRecursively()) {
                    log.debug("Deleted ${path.normalize()}")
                } else {
                    log.warn("Failed to delete ${path.normalize()}")
                }
            }
        }
    }

    @Composable
    private fun background(selected: Boolean, hovered: Boolean) = when {
        selected -> MaterialTheme.colors.secondary
        hovered -> MaterialTheme.colors.secondaryVariant
        else -> MaterialTheme.colors.background
    }

    private fun formatDate(date: Instant) = date.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))

    companion object {
        private const val nameWeight = 15f
        private const val sizeWeight = 3f
        private const val typeWeight = 4f
    }
}
