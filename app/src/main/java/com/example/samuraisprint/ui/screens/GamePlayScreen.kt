package com.example.samuraisprint.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.example.samuraisprint.engine.GameEngine
import com.example.samuraisprint.ui.components.GameCanvas
import com.example.samuraisprint.ui.components.GameHud
import kotlinx.coroutines.isActive

@Composable
fun GamePlayScreen(
    engine: GameEngine,
    onNavigateHome: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    BackHandler {
        if (!engine.isPaused.value && !engine.isGameOver.value) {
            engine.togglePause()
        } else {
            engine.isRunning.value = false
            onNavigateHome()
        }
    }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}

        var lastTime = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (lastTime != 0L) {
                    val dt = (frameTimeNanos - lastTime) / 1_000_000_000.0f
                    engine.update(dt)
                }
                lastTime = frameTimeNanos
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101017))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionUp, Key.W, Key.Spacebar -> {
                            engine.jump()
                            true
                        }
                        Key.DirectionDown, Key.S -> {
                            engine.slide()
                            true
                        }
                        Key.DirectionRight, Key.D, Key.F, Key.J, Key.K, Key.Enter -> {
                            engine.slash()
                            true
                        }
                        Key.E, Key.ShiftLeft, Key.ShiftRight -> {
                            engine.triggerDragonSprint()
                            true
                        }
                        Key.Escape, Key.P -> {
                            engine.togglePause()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        GameCanvas(
            engine = engine,
            modifier = Modifier.fillMaxSize()
        )

        GameHud(
            engine = engine,
            onNavigateHome = onNavigateHome,
            modifier = Modifier.fillMaxSize()
        )
    }
}
