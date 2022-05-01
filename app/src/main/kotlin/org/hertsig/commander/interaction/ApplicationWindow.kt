package org.hertsig.commander.interaction

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import org.hertsig.commander.core.Favorite
import org.hertsig.commander.ui.FolderPanel
import java.nio.file.Paths

@Composable
fun ApplicationScope.ApplicationWindow() {
    Window(
        title = "Hertsig Commander",
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(height = 1024.dp, width = 1280.dp)
    ) {
        MaterialTheme(
            lightColors(
                primary = Color(0xFF0000CD),
                primaryVariant = Color(0xFF00BFFF),
                secondary = Color.LightGray,
                secondaryVariant = Color(0xFFDDDDDD),
            ), Typography(
                FontFamily.SansSerif,
                body1 = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.5.sp),
                body2 = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.25.sp),
                subtitle1 = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 0.25.sp),
            )
        ) {
            val favorites = remember {
                mutableStateListOf<Favorite>(
                )
            }
            Row(Modifier.fillMaxSize()) {
                FolderPanel(FocusDirection.Right, favorites)
                    .Panel(favorites.first().path, Modifier.fillMaxHeight().fillMaxWidth(0.5f))
                Divider(Modifier.fillMaxHeight().width(1.dp), MaterialTheme.colors.onSurface)
                // "max width" meaning the rest of what's left (after the left panel takes half). Yes, very intuitive.
                FolderPanel(FocusDirection.Left, favorites)
                    .Panel(favorites[4].path, modifier = Modifier.fillMaxHeight().fillMaxWidth())
            }
        }
    }
}
