package org.hertsig.commander.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.moveTo

class RenameDialog {
    private val log = LoggerFactory.getLogger(RenameDialog::class.java)

    private lateinit var visible: MutableState<Boolean>
    private lateinit var source: MutableState<Path?>
    private lateinit var target: MutableState<TextFieldValue>

    @Composable
    @OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
    fun RenameDialog() {
        visible = remember { mutableStateOf(false) }
        source = remember { mutableStateOf(null) }
        target = remember { mutableStateOf(TextFieldValue("")) }

        if (visible.value) {
            AlertDialog({ visible.value = false }, {
                Row(Modifier.padding(25.dp, 5.dp), Arrangement.spacedBy(5.dp)) {
                    Button(::confirm) { Text("OK") }
                    Button(onClick = { visible.value = false }) { Text("Cancel") }
                }
            }, Modifier.width(400.dp), {
                Text("Rename")
            }, {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Rename ${source.value?.absolutePathString()} to", Modifier.fillMaxWidth())

                    val focus = remember { FocusRequester() }
                    BasicTextField(target.value, { target.value = it },
                        Modifier.focusRequester(focus)
                            .fillMaxWidth().border(1.dp, MaterialTheme.colors.secondary).padding(4.dp, 2.dp)
                            .onKeyEvent { if (it.key == Key.Enter) confirm(); false },
                        singleLine = true, maxLines = 1,
                    )
                    LaunchedEffect(source) { focus.requestFocus() }
                }
            })
        }
    }

    fun show(path: Path) {
        source.value = path
        val name = path.fileName.toString()
        target.value = TextFieldValue(name, TextRange(0, name.lastIndexOf('.')))
        visible.value = true
    }

    private fun confirm() {
        visible.value = false
        val source = source.value!!
        val newName = target.value.text.trim()
        log.info("Renaming ${source.absolutePathString()} to $newName")
        source.moveTo(source.parent.resolve(newName))
    }
}
