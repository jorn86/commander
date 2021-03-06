package org.hertsig.commander.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.mouseClickable
import androidx.compose.material.CursorDropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import kotlinx.coroutines.delay
import org.hertsig.commander.core.*
import org.hertsig.commander.interaction.GlobalKeyboardListener
import org.hertsig.commander.ui.component.SmallButton
import org.hertsig.commander.ui.component.SmallDropdownMenuItem
import org.hertsig.commander.ui.component.TooltipText
import org.hertsig.commander.ui.component.TwoButtonDialog
import org.hertsig.commander.ui.state.*
import org.hertsig.mouse.MouseListener
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.isRegularFile
import kotlin.streams.toList

@OptIn(ExperimentalFoundationApi::class)
class FolderPanel(
    private val focusDirectionForTab: FocusDirection,
    internal val roots: RootsState,
    private val favorites: SnapshotStateList<Favorite>,
    private val showHidden: Boolean = false,
) {
    private val log = LoggerFactory.getLogger(FolderPanel::class.java)

    private val watcher = FileSystems.getDefault().newWatchService()
    private var lastKey: WatchKey? = null
    private lateinit var updateHack: MutableState<Int>

    internal lateinit var other: FolderPanel
    private val renameDialog = RenameDialog()

    internal lateinit var current: MutableState<Path>
    private lateinit var contents: State<List<Path>>
    private lateinit var selection: SnapshotStateList<Path>
    private lateinit var history: SnapshotStateList<Path>
    private lateinit var indexInHistory: MutableState<Int>
    private lateinit var showDeleteDialog: MutableState<Boolean>

    @Composable
    fun Panel(initialPath: Path = roots.roots.first(), modifier: Modifier = Modifier) {
        current = remember { mutableStateOf(initialPath.parent) }
        selection = remember { mutableStateListOf() }
        history = remember { mutableStateListOf() }
        indexInHistory = remember { mutableStateOf(-1) }
        updateHack = remember { mutableStateOf(0) }
        contents = remember(updateHack.value) { derivedStateOf {
            log.debug("Listing contents for ${current.value} (${updateHack.value})")
            try {
                Files.list(current.value).filter(::show).toList().sortedWith(fileOrder)
            } catch (e: IOException) {
                log.error("Failed to list contents of ${current.value}", e)
                listOf()
            }
        }}
        LaunchedEffect(Unit) {
            setCurrentFolder(initialPath)
            while (true) {
                lastKey?.pollEvents()?.forEach {
                    log.debug("Got ${it.kind()} ${it.context()} ${it.count()}")
                    updateHack.value += 1
                }
                roots.reload()
                delay(100)
            }
        }

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
                roots.roots.forEach { RootButton(it) }
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    SmallButton(onClick = { updateHack.value += 1 }) {
                        Icon(Icons.Outlined.Refresh, "Refresh")
                    }

                    FavoritesButton()

                    SmallButton(onClick = { Desktop.getDesktop().open(current.value.toFile()) }) {
                        Icon(Icons.Outlined.FolderOpen, "Open in Explorer")
                    }
                }
            }
            LazyColumn(Modifier.padding(6.dp).fillMaxSize().weight(0.1f)) {
                if (!roots.roots.contains(current.value)) {
                    item {
                        PathLine(current.value.resolve(".."))
                    }
                }

                items(contents.value) {
                    PathLine(it)
                }
            }
            StatusBar(Modifier)

            showDeleteDialog = remember { mutableStateOf(false) }
            TwoButtonDialog(showDeleteDialog, "Delete ${selection.size} files", "Delete",
                onConfirm = ::confirmDeleteSelection)

            renameDialog.RenameDialog()
        }
    }

    @Composable
    private fun PathLine(path: Path) {
        when (val data = rememberFileData(path, updateHack.value)) {
            is Folder -> PathLine(this@FolderPanel).FolderLine(data)
            is RegularFile -> PathLine(this@FolderPanel).FileLine(data)
            Failed -> {}
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
        TooltipText("Free: $free / $total") {
            SmallButton({ setCurrentFolder(path) }) {
                Text(path.toString())
            }
        }
    }

    @Composable
    private fun StatusBar(modifier: Modifier = Modifier) {
        Row(modifier.background(MaterialTheme.colors.secondary).fillMaxWidth().padding(4.dp, 2.dp)) {
            Text(statusBarText(contents.value), color = MaterialTheme.colors.onSecondary)
            if (selection.isNotEmpty()) {
                Text("Selected: ${statusBarText(selection)}", Modifier.fillMaxWidth(), MaterialTheme.colors.onSecondary, textAlign = TextAlign.End)
            }
        }
    }

    private fun statusBarText(contents: List<Path>) = try {
        val (folders, files) = contents.countWhere { it.isDirectory() }
        val size = contents.filter { it.isRegularFile() }.sumOf { it.fileSize() }
        val filesText = formatMultiple(files, "file")?.let { "$it (${formatSize(size)})" }
        listOfNotNull(formatMultiple(folders, "folder"), filesText).joinToString()
    } catch (e: IOException) {
        log.error("Failed to get status bar text", e)
        e.message.orEmpty()
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
    }

    fun setCurrentFolder(path: Path) {
        try {
            Files.list(path)
        } catch (e: IOException) {
            log.debug("Listing current folder", e)
            updateHack.value += 1
            return
        }
        if (path != current.value) {
            selection.clear()
            current.value = path.normalize()
        }
        indexInHistory.value += 1
        history.removeRange(indexInHistory.value, history.size)
        history.add(path.normalize())

        updateWatcher()
    }

    private fun updateWatcher() {
        lastKey?.cancel()
        lastKey = current.value.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
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
        val fromIndex = contents.value.indexOf(from)
        val toIndex = contents.value.indexOf(to)
        if (toIndex > fromIndex) {
            contents.value.subList(fromIndex, toIndex + 1).filterTo(selection) { !selection.contains(it) }
        } else if (fromIndex > toIndex) {
            contents.value.subList(toIndex, fromIndex).filterTo(selection) { !selection.contains(it) }
        }
    }

    fun rename(path: Path) = renameDialog.show(path)

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
            updateWatcher()
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

    private fun show(path: Path) = if (path.isHidden()) showHidden else Files.isReadable(path)

    companion object {
        private val fileOrder = Ordering.explicit(true, false)
            .onResultOf(Path::isDirectory)
            .thenComparing({ it.fileName.toString() }, String.CASE_INSENSITIVE_ORDER)
    }
}
