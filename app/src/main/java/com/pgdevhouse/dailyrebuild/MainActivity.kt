package com.pgdevhouse.dailyrebuild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf

/**
 * Android entry point only. Feature state, navigation, repositories, dialogs,
 * and screen coordination live outside the Activity in the Phase 2 shell.
 */
class MainActivity : ComponentActivity() {

    private val healthRefreshToken = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DailyRebuildAppTheme {
                DailyRebuildApp(
                    healthRefreshToken = healthRefreshToken.intValue
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        healthRefreshToken.intValue++
    }
}
