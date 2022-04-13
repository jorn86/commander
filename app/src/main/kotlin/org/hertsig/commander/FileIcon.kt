package org.hertsig.commander

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toPainter
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView

private val log = LoggerFactory.getLogger(FolderUI::class.java)

@Composable
internal fun FileIcon(path: Path) {
    val systemIcon = FileSystemView.getFileSystemView().getSystemIcon(path.toFile())
    val icon = systemIcon as? ImageIcon
    val image = icon?.image as? BufferedImage
    if (image != null) {
        Image(image.toPainter(), "icon")
    } else {
        log.debug("Could not find icon for $path: $systemIcon (${systemIcon?.javaClass}); ${icon?.image} (${icon?.image?.javaClass})")
    }
}
