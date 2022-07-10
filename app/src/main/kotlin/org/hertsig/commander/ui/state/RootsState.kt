package org.hertsig.commander.ui.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import java.io.File
import java.nio.file.Path

class RootsState {
    val roots: SnapshotStateList<Path> = mutableStateListOf()
    init { reload() }

    fun reload() {
        roots.clear()
        roots.addAll(File.listRoots().map { it.toPath() })
    }
}
