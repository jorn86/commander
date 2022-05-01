package org.hertsig.commander.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TooltipText(text: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    TooltipArea({
        Text(text, Modifier
            .background(MaterialTheme.colors.background)
            .border(1.dp, MaterialTheme.colors.onBackground)
            .padding(3.dp))
    }, modifier, content = content)
}
