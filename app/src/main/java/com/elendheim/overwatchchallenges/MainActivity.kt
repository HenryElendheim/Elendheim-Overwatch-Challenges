package com.elendheim.overwatchchallenges

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.elendheim.overwatchchallenges.ui.RollScreen
import com.elendheim.overwatchchallenges.ui.theme.OverwatchChallengesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OverwatchChallengesTheme {
                RollScreen()
            }
        }
    }
}
