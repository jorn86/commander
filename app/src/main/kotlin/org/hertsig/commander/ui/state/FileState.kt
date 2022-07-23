package org.hertsig.commander.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import org.hertsig.commander.core.getFileIcon
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import javax.swing.filechooser.FileSystemView
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.name

private val log = LoggerFactory.getLogger(FileState::class.java)

sealed interface FileState {
    val path: Path
    val name: String
    val showContextMenu: MutableState<Boolean>
}

object Failed: FileState {
    override val name = "Failed"
    override val path get() = throw UnsupportedOperationException()
    override val showContextMenu get() = throw UnsupportedOperationException()
}

data class RegularFile(
    override val path: Path,
    override val name: String,
    val icon: BufferedImage?,
    val type: String,
    val lastModified: Instant,
    val size: Long,
    val focusRequester: FocusRequester,
    override val showContextMenu: MutableState<Boolean>,
): FileState

data class Folder(
    override val path: Path,
    override val name: String,
    val icon: BufferedImage?,
    val focusRequester: FocusRequester,
    override val showContextMenu: MutableState<Boolean>,
): FileState

@Composable
fun rememberFileData(file: Path, key2: Any?): FileState {
    val focusRequester = remember { FocusRequester() }
    val showContextMenu = remember { mutableStateOf(false) }

    return remember(file, key2) {
        try {
            if (file.isDirectory()) {
                Folder(file, file.fileName.name, getFileIcon(file), focusRequester, showContextMenu)
            } else {
                RegularFile(
                    file,
                    file.fileName.name,
                    getFileIcon(file),
                    fileType(file) ?: "",
                    file.getLastModifiedTime().toInstant(),
                    file.fileSize(),
                    focusRequester,
                    showContextMenu,
                )
            }
        } catch (e: IOException) {
            log.error("Error resolving data for $file", e)
            Failed
        }
    }
}

private fun fileType(path: Path): String? =
    FileSystemView.getFileSystemView().getSystemTypeDescription(path.toFile())
    ?: Files.probeContentType(path)
