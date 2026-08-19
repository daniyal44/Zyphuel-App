package com.example.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.desktop.ui.OpsConsoleScreen

/**
 * Zyphuel Operations Console - the desktop half of the Zyphuel platform.
 *
 * The phone apps (customer and rider) stay the source of truth; this console reads and
 * writes the same Firestore collections so a dispatcher on a PC can watch every rider
 * move in real time and push order statuses without touching a phone.
 */
fun main() = application {
    val windowState = rememberWindowState(size = DpSize(1300.dp, 840.dp))

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Zyphuel Operations Console"
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Color(0xFF0284C7),
                onPrimary = Color.White
            )
        ) {
            val config = remember { DesktopConfig.load() }
            val firebase = config.getOrNull()

            if (firebase != null) {
                val state = remember(firebase) { OpsConsoleState(FirestoreRest(firebase)) }
                OpsConsoleScreen(state)
            } else {
                ConfigErrorScreen(config.exceptionOrNull()?.message)
            }
        }
    }
}

@Composable
private fun ConfigErrorScreen(message: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
    ) {
        Text("Zyphuel Operations Console", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("Cannot start: Firebase configuration missing", color = Color(0xFFFCA5A5), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            text = message ?: "Unknown configuration error.",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp
        )
        Text(
            text = "Fix: launch the console from the Zyphuel project folder (the one containing " +
                "app\\google-services.json), or set ZYPHUEL_FIREBASE_PROJECT_ID and " +
                "ZYPHUEL_FIREBASE_API_KEY in the environment.",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
        )
    }
}
