package fr.jc14fr.mutualisons

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val url by ServerSettings.serverUrl(context).collectAsState(initial = "")
                    val token by ServerSettings.token(context).collectAsState(initial = "")
                    var screen by remember { mutableStateOf("items") }

                    when {
                        url.isNullOrEmpty() -> ServerSetupScreen(onSaved = {})
                        token.isNullOrEmpty() -> LoginScreen(baseUrl = url.orEmpty(), onLoggedIn = {})
                        else -> Column {
                            Row {
                                Button(onClick = { screen = "items" }) { Text("Objets") }
                                Button(onClick = { screen = "circles" }) { Text("Cercles") }
                                Button(onClick = { screen = "settings" }) { Text("Réglages") }
                            }
                            when (screen) {
                                "circles" -> CirclesScreen(
                                    baseUrl = url.orEmpty(),
                                    token = token.orEmpty(),
                                    onSessionExpired = {}
                                )
                                "settings" -> SettingsScreen()
                                else -> ItemsScreen(
                                    baseUrl = url.orEmpty(),
                                    token = token.orEmpty(),
                                    onSessionExpired = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}