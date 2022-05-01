package org.hertsig.commander.util

import androidx.compose.ui.graphics.Color
import org.slf4j.LoggerFactory

fun Color.darken(amount: Float) = change(-amount)
fun Color.lighten(amount: Float) = change(amount)

private fun Color.change(amount: Float): Color {
    val red = (red + amount).coerceIn(colorSpace.getMinValue(0), colorSpace.getMaxValue(0))
    val green = (green + amount).coerceIn(colorSpace.getMinValue(1), colorSpace.getMaxValue(1))
    val blue = (blue + amount).coerceIn(colorSpace.getMinValue(2), colorSpace.getMaxValue(2))
    return Color(red, green, blue, alpha, colorSpace)
}
