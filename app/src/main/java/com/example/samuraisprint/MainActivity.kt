package com.example.samuraisprint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.samuraisprint.audio.SoundManager
import com.example.samuraisprint.data.GameRepository
import com.example.samuraisprint.engine.GameEngine
import com.example.samuraisprint.ui.screens.ArmoryScreen
import com.example.samuraisprint.ui.screens.DojoScreen
import com.example.samuraisprint.ui.screens.GamePlayScreen
import com.example.samuraisprint.ui.screens.HonorTrialsScreen
import com.example.samuraisprint.ui.screens.MainMenuScreen
import com.example.samuraisprint.ui.theme.SamuraiBg
import com.example.samuraisprint.ui.theme.SamuraiSprintTheme

enum class Screen {
    MAIN_MENU,
    GAME_PLAY,
    DOJO,
    ARMORY,
    TRIALS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SoundManager.init(applicationContext)
        val repository = GameRepository(applicationContext)
        val engine = GameEngine(repository)

        setContent {
            SamuraiSprintTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SamuraiBg)
                        .safeDrawingPadding()
                ) {
                    AppNavigation(
                        repository = repository,
                        engine = engine
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    repository: GameRepository,
    engine: GameEngine
) {
    var currentScreen by remember { mutableStateOf(Screen.MAIN_MENU) }

    when (currentScreen) {
        Screen.MAIN_MENU -> {
            MainMenuScreen(
                repository = repository,
                onStartGame = {
                    engine.startRun()
                    currentScreen = Screen.GAME_PLAY
                },
                onNavigateDojo = { currentScreen = Screen.DOJO },
                onNavigateArmory = { currentScreen = Screen.ARMORY },
                onNavigateTrials = { currentScreen = Screen.TRIALS }
            )
        }
        Screen.GAME_PLAY -> {
            GamePlayScreen(
                engine = engine,
                onNavigateHome = {
                    currentScreen = Screen.MAIN_MENU
                }
            )
        }
        Screen.DOJO -> {
            DojoScreen(
                repository = repository,
                onBack = { currentScreen = Screen.MAIN_MENU }
            )
        }
        Screen.ARMORY -> {
            ArmoryScreen(
                repository = repository,
                onBack = { currentScreen = Screen.MAIN_MENU }
            )
        }
        Screen.TRIALS -> {
            HonorTrialsScreen(
                repository = repository,
                onBack = { currentScreen = Screen.MAIN_MENU }
            )
        }
    }
}
