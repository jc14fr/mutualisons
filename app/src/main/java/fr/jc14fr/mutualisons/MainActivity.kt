package fr.jc14fr.mutualisons

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable

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

                    val loggedIn = !url.isNullOrEmpty() && !token.isNullOrEmpty()

                    Scaffold(
                        bottomBar = {
                            if (loggedIn) {
                                MainNavigation(selected = screen, onSelect = { screen = it })
                            }
                        }
                    ) { inner ->
                        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
                            when {
                                url.isNullOrEmpty() -> ServerSetupScreen(onSaved = {})
                                token.isNullOrEmpty() -> LoginScreen(baseUrl = url.orEmpty(), onLoggedIn = {})
                                else -> when (screen) {
                                    "add" -> AddItemScreen(
                                        baseUrl = url.orEmpty(),
                                        token = token.orEmpty(),
                                        onSessionExpired = {}
                                    )
                                    "circles" -> CirclesScreen(
                                        baseUrl = url.orEmpty(),
                                        token = token.orEmpty(),
                                        onSessionExpired = {}
                                    )
                                    "settings" -> SettingsScreen()
                                    else -> ItemsScreen(
                                        baseUrl = url.orEmpty(),
                                        token = token.orEmpty(),
                                        onSessionExpired = {},
                                        onAddClick = { screen = "add" }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainNavigation(selected: String, onSelect: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == "items",
            onClick = { onSelect("items") },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Objets") }
        )
        NavigationBarItem(
            selected = selected == "add",
            onClick = { onSelect("add") },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            label = { Text("Ajouter") }
        )
        NavigationBarItem(
            selected = selected == "circles",
            onClick = { onSelect("circles") },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            label = { Text("Cercles") }
        )
        NavigationBarItem(
            selected = selected == "settings",
            onClick = { onSelect("settings") },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Réglages") }
        )
    }
}