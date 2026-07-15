package com.elendheim.overwatchchallenges

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.elendheim.overwatchchallenges.ui.RollScreen
import com.elendheim.overwatchchallenges.ui.RollViewModel
import com.elendheim.overwatchchallenges.ui.theme.OverwatchChallengesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RollViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state = viewModel.state
            OverwatchChallengesTheme(
                textScale = state.textSize.scale,
                highContrast = state.highContrast,
            ) {
                RollScreen(viewModel)
            }
        }
    }
}
