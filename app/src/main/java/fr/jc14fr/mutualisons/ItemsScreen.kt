package fr.jc14fr.mutualisons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private fun statusLabel(status: String): String = when (status) {
    "available" -> "Disponible"
    "lent" -> "Prêté"
    "reserved" -> "Réservé"
    "repair" -> "En réparation"
    "hidden" -> "Masqué"
    else -> status
}

@Composable
fun ItemsScreen(baseUrl: String, token: String, onSessionExpired: () -> Unit) {
    val context = LocalContext.current
    var list by remember { mutableStateOf<List<PocketBaseApi.Item>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            list = PocketBaseApi.listItems(baseUrl, token)
        } catch (e: AuthExpiredException) {
            ServerSettings.clearToken(context)
            onSessionExpired()
            return@LaunchedEffect
        } catch (e: Exception) {
            error = "Chargement impossible : ${e.message}"
        }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Objets partagés")
            Button(onClick = { reload++ }, enabled = !loading) {
                Text("Actualiser")
            }
        }
        if (loading) Text("Chargement…")
        error?.let { Text(it) }
        if (!loading && error == null && list.isEmpty()) {
            Text("Aucun objet visible pour le moment.")
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(list) { item ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(item.title)
                    Text("${item.category} · ${statusLabel(item.status)}")
                }
            }
        }
    }
}