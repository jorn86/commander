package org.hertsig.mouse

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MouseClickScope
import androidx.compose.ui.input.pointer.*
import java.time.Clock
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalFoundationApi::class)
abstract class MouseListener(
    private val doubleClickTimeout: Duration = Duration.ofMillis(500),
    private val clock: Clock = Clock.systemUTC(),
) : (MouseClickScope) -> Unit {
    protected var lastPrimary = Instant.EPOCH; private set
    protected var lastSecondary = Instant.EPOCH; private set

    final override fun invoke(scope: MouseClickScope) {
        when {
            scope.buttons.isPrimaryPressed -> handlePrimary(scope.keyboardModifiers)
            scope.buttons.isSecondaryPressed -> handleSecondary(scope.keyboardModifiers)
            scope.buttons.isTertiaryPressed -> onTertiary(scope.keyboardModifiers)
            scope.buttons.isForwardPressed -> onForward(scope.keyboardModifiers)
            scope.buttons.isBackPressed -> onBack(scope.keyboardModifiers)
        }
    }

    private fun handlePrimary(modifiers: PointerKeyboardModifiers) {
        val now = clock.instant()
        if (now.isBefore(lastPrimary.plus(doubleClickTimeout))) {
            onDoublePrimary(modifiers)
        } else {
            onPrimary(modifiers)
        }
        lastPrimary = now
    }

    private fun handleSecondary(modifiers: PointerKeyboardModifiers) {
        val now = clock.instant()
        if (now.isBefore(lastSecondary.plus(doubleClickTimeout))) {
            onDoubleSecondary(modifiers)
        } else {
            onSecondary(modifiers)
        }
        lastSecondary = now
    }

    open fun onPrimary(modifiers: PointerKeyboardModifiers) = onUnhandled(modifiers)
    open fun onDoublePrimary(modifiers: PointerKeyboardModifiers) = onPrimary(modifiers)

    open fun onSecondary(modifiers: PointerKeyboardModifiers) = onUnhandled(modifiers)
    open fun onDoubleSecondary(modifiers: PointerKeyboardModifiers) = onSecondary(modifiers)

    open fun onTertiary(modifiers: PointerKeyboardModifiers) = onUnhandled(modifiers)

    open fun onForward(modifiers: PointerKeyboardModifiers) = onUnhandled(modifiers)

    open fun onBack(modifiers: PointerKeyboardModifiers) = onUnhandled(modifiers)

    open fun onUnhandled(modifiers: PointerKeyboardModifiers) {}
}
