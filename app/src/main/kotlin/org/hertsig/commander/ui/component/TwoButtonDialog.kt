package org.hertsig.commander.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TwoButtonDialog(visible: MutableState<Boolean>, text: String, confirm: String = "OK", cancel: String = "Cancel", onConfirm: () -> Unit) {
    if (visible.value) {
        AlertDialog(onDismissRequest = { visible.value = false }, {
            Row(Modifier.padding(5.dp), Arrangement.spacedBy(5.dp)) {
                Button({
                    visible.value = false
                    onConfirm()
                }) {
                    Text(confirm)
                }
                Button(onClick = { visible.value = false }) {
                    Text(cancel)
                }
            }
        }, title = { Text(text) })
    }
}
