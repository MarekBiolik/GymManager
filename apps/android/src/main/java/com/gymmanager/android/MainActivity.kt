package com.gymmanager.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymmanager.android.theme.GymManagerTheme
import com.gymmanager.android.theme.GymTokens
import com.gymmanager.core.HelloCore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymManagerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GymTokens.ColorBgCanvas)
                        .padding(GymTokens.Space4),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = HelloCore.greeting,
                        color = GymTokens.ColorTextPrimary,
                    )
                }
            }
        }
    }
}
