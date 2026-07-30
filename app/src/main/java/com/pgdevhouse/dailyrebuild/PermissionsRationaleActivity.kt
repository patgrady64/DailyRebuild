package com.pgdevhouse.dailyrebuild

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Privacy explanation opened from Health Connect's permissions screen.
 */
class PermissionsRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DailyRebuildAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement =
                            Arrangement.Center
                    ) {
                        Text(
                            text = "Health Connect privacy",
                            style =
                                MaterialTheme.typography
                                    .headlineMedium
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Text(
                            text =
                                "Daily Rebuild requests read-only access " +
                                    "to your steps, walking distance, and " +
                                    "recorded activity time. These totals appear " +
                                    "on your dashboard and are stored in the " +
                                    "day's Daily Rebuild snapshot automatically " +
                                    "as information changes."
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text =
                                "Daily Rebuild never writes to, changes, " +
                                    "or deletes information in Health " +
                                    "Connect, Google Fit, or your fitness " +
                                    "device. Delete Entire Day removes only " +
                                    "the snapshot stored inside Daily " +
                                    "Rebuild."
                        )

                        Spacer(
                            modifier = Modifier.height(24.dp)
                        )

                        Button(
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}
