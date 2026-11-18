package fr.jc14fr.mutualisons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ServerSetupScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("https://") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Adresse du serveur de votre groupe")
        Text("Par exemple : https://pb.mondomaine.fr")
        OutlinedTextField(
            value = url,
            onValueChange = { url = it; error = null },
            label = { Text("Adresse") },
            isError = error != null,
            modifier = Modifier.fillMaxSize(0.9f)
        )
        error?.let { Text(it) }
        Button(onClick = {
            if (isValidServerUrl(url)) {
                scope.launch {
                    ServerSettings.saveServerUrl(context, url)
                    onSaved()
                }
            } else {
                error = "Adresse invalide : elle doit commencer par https://"
            }
        }) {
            Text("Enregistrer")
        }
    }
}