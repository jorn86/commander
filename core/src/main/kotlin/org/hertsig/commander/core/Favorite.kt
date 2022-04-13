package org.hertsig.commander.core

import java.nio.file.Path

data class Favorite(
    val path: Path,
    private val name: String? = null
) {
    val display; get() = name ?: (path.fileName ?: path).toString()
}
