package com.example.samuraisprint.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.samuraisprint.engine.GameEngine
import com.example.samuraisprint.ui.components.GameCanvas
import com.example.samuraisprint.ui.components.GameHud

@Composable
fun GamePlayScreen(
    engine: GameEngine,
    onNavigateHome: () -> Unit
) {
    // Back button pauses or returns
    BackHandler {
        if (!engine.isPaused.value && !engine.isGameOver.value) {
            engine.togglePause()
        } else {
            engine.isRunning.value = false
            onNavigateHome()
        }
    }

    // 60fps Game loop coroutine
    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
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
