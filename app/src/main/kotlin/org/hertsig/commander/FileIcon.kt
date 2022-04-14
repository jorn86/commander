package org.hertsig.commander

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toPainter
import org.hertsig.commander.core.getFileIcon
import java.nio.file.Path

@Composable
internal fun FileIcon(path: Path) {
    getFileIcon(path)?.let { Image(it.toPainter(), "icon") }
}
