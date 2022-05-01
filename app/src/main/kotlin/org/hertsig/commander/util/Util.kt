package org.hertsig.commander.util

import androidx.compose.runtime.Composable

@Composable
inline fun <T> T.applyIf(condition: Boolean, action: @Composable T.() -> T) = if (condition) action() else this
