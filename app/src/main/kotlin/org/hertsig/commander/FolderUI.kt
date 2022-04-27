package org.hertsig.commander

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.common.collect.Ordering
import org.hertsig.commander.core.*
import org.hertsig.mouse.MouseListener
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.moveTo
import kotlin.streams.toList

@OptIn(ExperimentalFoundationApi::class)
class FolderUI(
    private val focusDirectionForTab: FocusDirection,
    private val favorites: SnapshotStateList<Favorite>,
    private val showHidden: Boolean = false
) {
    private val log = LoggerFactory.getLogger(FolderUI::class.java)

    private lateinit var current: MutableState<Path>
    private lateinit var selection: SnapshotStateList<Path>
    private lateinit var history: SnapshotStateList<Path>
    private lateinit var indexInHistory: MutableState<Int>
    private lateinit var showDeleteDialog: MutableState<Boolean>

    @Composable
    fun Panel(initialPath: Path = roots.first(), modifier: Modifier = Modifier) {
        current = remember { mutableStateOf(initialPath) }
        selection = remember { mutableStateListOf() }
        history = remember { mutableStateListOf(initialPath) }
        indexInHistory = remember { mutableStateOf(0) }

        Column(modifier
            .onKeyEvent(KeyboardListener(this, LocalFocusManager.current, focusDirectionForTab))
            .mouseClickable(onClick = object : MouseListener() {
                override fun onForward(modifiers: PointerKeyboardModifiers) = forward()
                override fun onBack(modifiers: PointerKeyboardModifiers) = back()
            })
        ) {
            Text(current.value.toString(), modifier = Modifier.clickable {
                val selection = StringSelection(current.value.toString())
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            })
            Row(Modifier.fillMaxWidth()) {
                roots.forEach { RootButton(it) }
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    SmallButton(onClick = { forceReload() }) {
                        Icon(Icons.Outlined.Refresh, "Refresh")
                    }

                    FavoritesButton()

                    SmallButton(onClick = { Desktop.getDesktop().open(current.value.toFile()) }) {
                        Icon(Icons.Outlined.FolderOpen, "Open in Explorer")
                    }
                }
            }
            Column(Modifier.fillMaxSize().weight(0.1f).verticalScroll(rememberScrollState())) {
                if (!roots.contains(current.value)) {
                    FolderLine(current.value.resolve(".."))
                }
                currentFolderContents().forEach {
                    if (it.isDirectory()) {
                        FolderLine(it)
                    } else {
                        FileLine(it)
                    }
                }
            }
            StatusBar(Modifier)

            DeleteDialog()
        }
    }

    @Composable
    private fun StatusBar(modifier: Modifier = Modifier) {
        Row(modifier.background(MaterialTheme.colors.secondary).fillMaxWidth().padding(4.dp, 2.dp)) {
            val contents = currentFolderContents()
            val (folders, files) = contents.countWhere { it.isDirectory() }
            val size = contents.filter { !it.isDirectory() }.sumOf { it.fileSize() }
            val filesText = formatMultiple(files, "file")?.let { "$it (${formatSize(size)})" }
            val text = listOfNotNull(formatMultiple(folders, "folder"), filesText)
                .joinToString()
            Text(text, color = MaterialTheme.colors.onSecondary)
        }
    }

    @Composable
    private fun FavoritesButton() {
        val showFavorites = remember { mutableStateOf(false) }
        SmallButton(onClick = { showFavorites.value = true }) {
            Icon(Icons.Outlined.Favorite, "Favorites")
            DropdownMenu(showFavorites.value, onDismissRequest = { showFavorites.value = false }) {
                favorites.forEach {
                    SmallDropdownMenuItem(it.display, showFavorites) {
                        setCurrentFolder(it.path)
                    }
                }
            }
        }
    }

    @Composable
    private fun SmallDropdownMenuItem(text: String, visible: MutableState<Boolean>, onClick: () -> Unit) {
        Text(text, Modifier.fillMaxWidth().clickable {
            onClick()
            visible.value = false
        }.padding(8.dp, 4.dp))
    }

    private fun show(path: Path) = try {
        if (path.isHidden()) showHidden else Files.isReadable(path)
    } catch (e: AccessDeniedException) {
        log.debug("$path readable", e)
        false
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun DeleteDialog() {
        showDeleteDialog = remember { mutableStateOf(false) }
        if (showDeleteDialog.value) {
            AlertDialog(onDismissRequest = { showDeleteDialog.value = false }, {
                Row(Modifier.padding(5.dp), Arrangement.spacedBy(5.dp)) {
                    Button(onClick = {
                        selection.forEach {
                            showDeleteDialog.value = false
                            if (it.toFile().deleteRecursively()) {
                                log.debug("Deleted ${it.normalize()}")
                            } else {
                                log.warn("Failed to delete ${it.normalize()}")
                            }
                        }
                        selection.clear()
                        forceReload()
                    }) {
                        Text("Delete")
                    }
                    Button(onClick = { showDeleteDialog.value = false }) {
                        Text("Cancel")
                    }
                }
            }, title = { Text("Delete ${selection.size} files") })
        }
    }

    @Composable
    private fun RootButton(path: Path) {
        TooltipArea({
            val free = formatSize(path.toFile().freeSpace)
            val total = formatSize(path.toFile().totalSpace)
            Text("Free: $free / $total", Modifier
                .background(MaterialTheme.colors.background)
                .border(1.dp, MaterialTheme.colors.primaryVariant)
                .padding(5.dp))
        }) {
            Button(
                onClick = { setCurrentFolder(path) },
                modifier = Modifier.sizeIn(minWidth = 1.dp, minHeight = 1.dp),
                contentPadding = PaddingValues(8.dp, 4.dp),
            ) {
                Text(path.toString())
            }
        }
    }

    @Composable
    private fun FileLine(it: Path) {
        val focusRequester = remember { FocusRequester() }
        val showContextMenu = remember { mutableStateOf(false) }
        PathLine(it, FileMouseListener(this@FolderUI, it, focusRequester, showContextMenu)) {
            FileIcon(it)
            TextCell(it.fileName.toString(), nameWeight)
            TextCell(formatSize(it.fileSize()), sizeWeight, TextAlign.Right)
            TextCell(fileType(it) ?: "", typeWeight)
        }
    }

    @Composable
    private fun FolderLine(folder: Path) {
        val focusRequester = remember { FocusRequester() }
        val showContextMenu = remember { mutableStateOf(false) }
        PathLine(folder, FolderMouseListener(this@FolderUI, folder, focusRequester, showContextMenu)) {
            FileIcon(folder)
            Text(folder.fileName.toString(), softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }

    @Composable
    private fun PathLine(
        path: Path,
        listener: PathMouseListener,
        modifier: Modifier = Modifier,
        content: @Composable RowScope.() -> Unit
    ) {
        val hoverSource = remember { MutableInteractionSource() }
        val interactionSource = remember { MutableInteractionSource() }
        val focused by interactionSource.collectIsFocusedAsState()

        Row(modifier.fillMaxWidth()
            .focusRequester(listener.focusRequester)
            .focusable(interactionSource = interactionSource)
            // TODO dashed border (currently unsupported)
            .run { if (focused) border(1.dp, MaterialTheme.colors.secondary) else this }
            .background(background(isSelected(path), hoverSource.collectIsHoveredAsState().value))
            .hoverable(hoverSource)
            .mouseClickable(onClick = remember(path) { listener }),
            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            content()
            PathDropdownMenu(listener, path)
        }
    }

    @Composable
    private fun PathDropdownMenu(listener: PathMouseListener, path: Path) {
        DropdownMenu(listener.showContextMenu.value, { listener.showContextMenu.value = false }) {
            if (path.parent !in roots) {
                SmallDropdownMenuItem("Move to parent", listener.showContextMenu) {
                    val target = path.parent.parent.resolve(path.fileName)
                    log.debug("Moving $path to $target")
                    path.moveTo(target)
                    forceReload()
                }
                Divider()
            }
            SmallDropdownMenuItem("Delete", listener.showContextMenu) {
                if (path.toFile().deleteRecursively()) {
                    log.debug("Deleted ${path.normalize()}")
                } else {
                    log.warn("Failed to delete ${path.normalize()}")
                }
                forceReload()
            }
        }
    }

    @Composable
    private fun background(selected: Boolean, hovered: Boolean) = when {
        selected -> MaterialTheme.colors.secondary
        hovered -> MaterialTheme.colors.secondaryVariant
        else -> MaterialTheme.colors.background
    }

    @Composable
    private fun RowScope.TextCell(text: String, weight: Float, align: TextAlign = TextAlign.Left) {
        Text(text, textAlign = align, softWrap = false, maxLines = 1, overflow = TextOverflow.Clip,
            modifier = Modifier.weight(weight).fillMaxWidth())
    }

    fun setCurrentFolder(path: Path) {
        try {
            Files.list(path)
        } catch (e: AccessDeniedException) {
            log.debug("Listing current folder", e)
            return
        }
        if (path != current.value) {
            selection.clear()
            current.value = path.normalize()
        }
        indexInHistory.value += 1
        history.removeRange(indexInHistory.value, history.size)
        history.add(path)
    }

    fun isSelected(path: Path) = path in selection

    fun select(path: Path, add: Boolean) {
        if (!add) selection.clear()
        if (path in selection) {
            selection.remove(path)
        } else {
            selection.add(path)
        }
    }

    fun selectRange(to: Path) {
        val from = selection.lastOrNull() ?: return select(to, false)
        val contents = currentFolderContents()
        val fromIndex = contents.indexOf(from)
        val toIndex = contents.indexOf(to)
        if (toIndex > fromIndex) {
            contents.subList(fromIndex, toIndex + 1).filterTo(selection) { !selection.contains(it) }
        } else if (fromIndex > toIndex) {
            contents.subList(toIndex, fromIndex).filterTo(selection) { !selection.contains(it) }
        }
    }

    fun back() {
        if (indexInHistory.value > 0) {
            indexInHistory.value -= 1
            selection.clear()
            // DO NOT use setCurrentFolder here!
            current.value = history[indexInHistory.value]
        }
    }

    fun forward() {
        if (indexInHistory.value < history.size - 1) {
            indexInHistory.value += 1
            selection.clear()
            // DO NOT use setCurrentFolder here!
            current.value = history[indexInHistory.value]
        }
    }

    fun onDelete() {
        showDeleteDialog.value = true
    }

    private fun forceReload() {
        val folder = current.value
        current.value = folder.resolve("..")
        current.value = folder
    }

    private fun currentFolderContents() = Files.list(current.value).filter(::show).toList().sortedWith(fileOrder)

    companion object {
        private val roots = File.listRoots().map { it.toPath() }
        private const val nameWeight = 15f
        private const val sizeWeight = 3f
        private const val typeWeight = 4f

        private val fileOrder = Ordering.explicit(true, false)
            .onResultOf(Path::isDirectory)
            .thenComparing({ it.fileName.toString() }, String.CASE_INSENSITIVE_ORDER)
    }
}
