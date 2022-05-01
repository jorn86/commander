package org.hertsig.commander.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.hertsig.commander.util.lighten

@Composable
fun SmallDropdownMenuItem(text: String, visible: MutableState<Boolean>, enabled: Boolean = true, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val hovered = source.collectIsHoveredAsState()
    var modifier = Modifier.fillMaxWidth()
    if (enabled) {
        modifier = modifier.clickable(source, LocalIndication.current) {
            onClick()
            visible.value = false
        }
    }
    Text(text, modifier.background(if (hovered.value) MaterialTheme.colors.secondary else MaterialTheme.colors.surface).padding(8.dp, 4.dp),
        if (enabled) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.lighten(0.5f))
}
