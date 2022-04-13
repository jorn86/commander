package org.hertsig.commander.core

private const val KB = 1024f
private const val MB = KB * 1024
private const val GB = MB * 1024
private const val TB = GB * 1024

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
