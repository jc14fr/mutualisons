package fr.jc14fr.mutualisons

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val CATEGORIES = listOf(
    "livre" to "Livre",
    "dvd" to "DVD",
    "outil" to "Outil",
    "appareil" to "Appareil",
    "jeu" to "Jeu",
    "autre" to "Autre"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddItemScreen(baseUrl: String, token: String, onSessionExpired: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var circles by remember { mutableStateOf<List<PocketBaseApi.Circle>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("livre") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            circles = PocketBaseApi.listCircles(baseUrl, token)
        } catch (e: AuthExpiredException) {
            ServerSettings.clearToken(context)
            onSessionExpired()
        } catch (e: Exception) {
            error = "Chargement des cercles impossible : ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Ajouter un objet", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null; info = null },
                    label = { Text("Titre (ex. Perceuse à percussion)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Catégorie", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORIES.forEach { (value, label) ->
                        FilterChip(
                            selected = category == value,
                            onClick = { category = value },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Partager avec", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Facultatif. Sans cercle coché, l'objet reste privé.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (circles.isEmpty() && error == null) {
                    Text(
                        "Vous n'êtes membre d'aucun cercle pour l'instant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                circles.forEach { c ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = c.id in selected,
                            onCheckedChange = { on ->
                                selected = if (on) selected + c.id else selected - c.id
                            }
                        )
                        Text(c.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        info?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            enabled = !loading && title.isNotBlank(),
            onClick = {
                loading = true
                error = null
                info = null
                val chosen = selected.toList()
                scope.launch {
                    try {
                        val me = PocketBaseApi.currentUserId(baseUrl, token)
                        PocketBaseApi.createItem(
                            baseUrl, token, title.trim(), category, chosen, me
                        )
                        info = "« ${title.trim()} » ajouté${if (chosen.isEmpty()) " (privé)" else ""}."
                        title = ""
                        selected = emptySet()
                    } catch (e: AuthExpiredException) {
                        ServerSettings.clearToken(context)
                        onSessionExpired()
                    } catch (e: Exception) {
                        error = e.message
                    }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Ajouter à ma liste") }
    }
}