package fr.jc14fr.mutualisons

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val url by ServerSettings.serverUrl(context).collectAsState(initial = "")
                    if (url.isNullOrEmpty()) {
                        ServerSetupScreen(onSaved = {})
                    } else {
                        Accueil()
                    }
                }
            }
        }
    }
}

@Composable
fun Accueil() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Mutualisons : le projet démarre.")
    }
}