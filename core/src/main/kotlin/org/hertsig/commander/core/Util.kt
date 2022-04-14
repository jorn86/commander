package org.hertsig.commander.core

import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView

private const val KB = 1024f
private const val MB = KB * 1024
private const val GB = MB * 1024
private const val TB = GB * 1024

object Util {}
private val log = LoggerFactory.getLogger(Util::class.java)

fun formatSize(fileSize: Long): String {
    val (amount, unit) = when {
        fileSize > 1024L * 1024 * 1024 * 1024 -> fileSize / TB to "TB"
        fileSize > 1024L * 1024 * 1024 -> fileSize / GB to "GB"
        fileSize > 1024L * 1024 -> fileSize / MB to "MB"
        fileSize > 1024L -> fileSize / KB to "kB"
        else -> fileSize.toFloat() to "B"
    }
    return when {
        unit == "B" ||
        amount > 1000 -> String.format("%.0f $unit", amount)
        amount > 100 -> String.format("%.1f $unit", amount)
        else -> String.format("%.2f $unit", amount)
    }
}

fun fileType(path: Path): String? = FileSystemView.getFileSystemView().getSystemTypeDescription(path.toFile())
    ?: Files.probeContentType(path)

fun getFileIcon(path: Path): BufferedImage? {
    val systemIcon = FileSystemView.getFileSystemView().getSystemIcon(path.toFile())
    val icon = systemIcon as? ImageIcon
    val image = icon?.image as? BufferedImage
    if (image == null) {
        log.debug("Could not find icon for $path: $systemIcon (${systemIcon?.javaClass}); ${icon?.image} (${icon?.image?.javaClass})")
    }
    return image
}
