package org.hertsig.commander.ui

import androidx.compose.foundation.*
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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.common.collect.Ordering
import org.hertsig.commander.core.*
import org.hertsig.commander.interaction.*
import org.hertsig.commander.ui.component.SmallButton
import org.hertsig.commander.ui.component.SmallDropdownMenuItem
import org.hertsig.commander.ui.component.TooltipText
import org.hertsig.commander.ui.component.TwoButtonDialog
import org.hertsig.mouse.MouseListener
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.File
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.isRegularFile
import kotlin.streams.toList

@OptIn(ExperimentalFoundationApi::class)
class FolderPanel(
    private val focusDirectionForTab: FocusDirection,
    private val favorites: SnapshotStateList<Favorite>,
    private val showHidden: Boolean = false,
) {
    private val log = LoggerFactory.getLogger(FolderPanel::class.java)

    internal lateinit var other: FolderPanel

    internal lateinit var current: MutableState<Path>
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
            .onKeyEvent(GlobalKeyboardListener(this, LocalFocusManager.current, focusDirectionForTab))
            .mouseClickable(onClick = object : MouseListener() {
                override fun onForward(modifiers: PointerKeyboardModifiers) = forward()
                override fun onBack(modifiers: PointerKeyboardModifiers) = back()
            })
        ) {
            Text(current.value.toString(), modifier = Modifier.padding(6.dp, 0.dp).clickable {
                setClipboard(current.value.toString())
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
            Column(Modifier.padding(6.dp).fillMaxSize().weight(0.1f).verticalScroll(rememberScrollState())) {
                if (!roots.contains(current.value)) {
                    PathLine(this@FolderPanel).FolderLine(current.value.resolve(".."))
                }
                currentFolderContents().forEach {
                    if (it.isDirectory()) {
                        PathLine(this@FolderPanel).FolderLine(it)
                    } else {
                        PathLine(this@FolderPanel).FileLine(it)
                    }
                }
            }
            StatusBar(Modifier)

            showDeleteDialog = remember { mutableStateOf(false) }
            TwoButtonDialog(showDeleteDialog, "Delete ${selection.size} files", "Delete",
                onConfirm = ::confirmDeleteSelection)
        }
    }

    @Composable
    private fun FavoritesButton() {
        val showFavorites = remember { mutableStateOf(false) }
        SmallButton(onClick = { showFavorites.value = true }) {
            Icon(Icons.Outlined.Favorite, "Favorites")
            CursorDropdownMenu(showFavorites.value, onDismissRequest = { showFavorites.value = false }) {
                favorites.forEach {
                    SmallDropdownMenuItem(it.display, showFavorites) {
                        setCurrentFolder(it.path)
                    }
                }
            }
        }
    }

    @Composable
    private fun RootButton(path: Path) {
        val free = formatSize(path.toFile().freeSpace)
        val total = formatSize(path.toFile().totalSpace)
        TooltipText("Free: $free / $total"){
            SmallButton({ setCurrentFolder(path) }) {
                Text(path.toString())
            }
        }
    }

    @Composable
    private fun StatusBar(modifier: Modifier = Modifier) {
        Row(modifier.background(MaterialTheme.colors.secondary).fillMaxWidth().padding(4.dp, 2.dp)) {
            val contents = currentFolderContents()
            Text(statusBarText(contents), color = MaterialTheme.colors.onSecondary)
            if (selection.isNotEmpty()) {
                Text("Selected: ${statusBarText(selection)}", Modifier.fillMaxWidth(), MaterialTheme.colors.onSecondary, textAlign = TextAlign.End)
            }
        }
    }

    private fun statusBarText(contents: List<Path>): String {
        val (folders, files) = contents.countWhere { it.isDirectory() }
        val size = contents.filter { it.isRegularFile() }.sumOf { it.fileSize() }
        val filesText = formatMultiple(files, "file")?.let { "$it (${formatSize(size)})" }
        return listOfNotNull(formatMultiple(folders, "folder"), filesText).joinToString()
    }

    private fun confirmDeleteSelection() {
        selection.forEach {
            if (it.toFile().deleteRecursively()) {
                log.debug("Deleted ${it.normalize()}")
            } else {
                log.warn("Failed to delete ${it.normalize()}")
            }
        }
        selection.clear()
        forceReload()
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
        history.add(path.normalize())
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
            updateHistory(true)
        }
    }

    fun forward() {
        if (indexInHistory.value < history.size - 1) {
            indexInHistory.value += 1
            selection.clear()
            updateHistory(false)
        }
    }

    // TODO extract to proper state management & make tests
    private tailrec fun updateHistory(backwards: Boolean) {
        val it = history[indexInHistory.value]
        if (it.isDirectory()) {
            current.value = it
        } else {
            log.debug("History entry no longer exists: $it")
            history.removeAt(indexInHistory.value)
            indexInHistory.value = indexInHistory.value.coerceIn(0, history.size - 1)
            while (indexInHistory.value > 0 && history[indexInHistory.value] == history[indexInHistory.value - 1]) {
                log.debug("Removing duplicate history entry: ${history[indexInHistory.value]}")
                history.removeAt(indexInHistory.value)
                if (backwards) indexInHistory.value -= 1
            }
            if (backwards) indexInHistory.value -= 1
            indexInHistory.value = indexInHistory.value.coerceIn(0, history.size - 1)
            updateHistory(backwards)
        }
    }

    fun onDelete() {
        showDeleteDialog.value = true
    }

    // TODO use filesystem watcher to auto-trigger reload when needed
    internal fun forceReload() {
        val folder = current.value
        current.value = folder.parent
        current.value = folder
    }

    private fun currentFolderContents() = Files.list(current.value).filter(::show).toList().sortedWith(fileOrder)

    private fun show(path: Path) = try {
        if (path.isHidden()) showHidden else Files.isReadable(path)
    } catch (e: AccessDeniedException) {
        log.debug("$path readable", e)
        false
    }

    companion object {
        internal val roots = File.listRoots().map { it.toPath() }

        private val fileOrder = Ordering.explicit(true, false)
            .onResultOf(Path::isDirectory)
            .thenComparing({ it.fileName.toString() }, String.CASE_INSENSITIVE_ORDER)
    }
}
