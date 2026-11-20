package fr.jc14fr.mutualisons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment as UiAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private fun categoryLabel(c: String): String = when (c) {
    "livre" -> "Livre"
    "dvd" -> "DVD"
    "outil" -> "Outil"
    "appareil" -> "Appareil"
    "jeu" -> "Jeu"
    else -> "Autre"
}

private fun statusLabel(status: String): String = when (status) {
    "available" -> "Disponible"
    "lent" -> "Prêté"
    "reserved" -> "Réservé"
    "repair" -> "En réparation"
    "hidden" -> "Masqué"
    else -> status
}

private fun statusColor(status: String): Color = when (status) {
    "available" -> Color(0xFF2E7D32)
    "lent" -> Color(0xFFEF6C00)
    "reserved" -> Color(0xFF1565C0)
    "repair" -> Color(0xFF6D4C41)
    else -> Color(0xFF757575)
}

@Composable
fun ItemsScreen(
    baseUrl: String,
    token: String,
    onSessionExpired: () -> Unit,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<PocketBaseApi.Item>>(emptyList()) }
    var circles by remember { mutableStateOf<List<PocketBaseApi.Circle>>(emptyList()) }
    var me by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var toShare by remember { mutableStateOf<PocketBaseApi.Item?>(null) }
    var toDelete by remember { mutableStateOf<PocketBaseApi.Item?>(null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            if (me.isEmpty()) me = PocketBaseApi.currentUserId(baseUrl, token)
            list = PocketBaseApi.listItems(baseUrl, token)
            circles = PocketBaseApi.listCircles(baseUrl, token)
        } catch (e: AuthExpiredException) {
            ServerSettings.clearToken(context)
            onSessionExpired()
            return@LaunchedEffect
        } catch (e: Exception) {
            error = "Chargement impossible : ${e.message}"
        }
        loading = false
    }

    toShare?.let { item ->
        AlertDialog(
            onDismissRequest = { toShare = null },
            title = { Text("Partager « ${item.title} »") },
            text = {
                Column {
                    if (circles.isEmpty()) Text("Vous n'êtes membre d'aucun cercle.")
                    circles.forEach { c ->
                        Row(verticalAlignment = UiAlignment.CenterVertically) {
                            Checkbox(
                                checked = c.id in selected,
                                onCheckedChange = { on ->
                                    selected = if (on) selected + c.id else selected - c.id
                                }
                            )
                            Text(c.name)
                        }
                    }
                    Text("Aucun cercle coché : l'objet reste privé.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = item
                    val chosen = selected.toList()
                    toShare = null
                    scope.launch {
                        try {
                            PocketBaseApi.updateItemCircles(baseUrl, token, target.id, chosen)
                            info = "Partage de « ${target.title} » mis à jour."
                            reload++
                        } catch (e: AuthExpiredException) {
                            ServerSettings.clearToken(context)
                            onSessionExpired()
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { toShare = null }) { Text("Annuler") }
            }
        )
    }

    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer « ${item.title} » ?") },
            text = { Text("Cette suppression est définitive.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = item
                    toDelete = null
                    scope.launch {
                        try {
                            PocketBaseApi.deleteItem(baseUrl, token, target.id)
                            info = "« ${target.title} » supprimé."
                            reload++
                        } catch (e: AuthExpiredException) {
                            ServerSettings.clearToken(context)
                            onSessionExpired()
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Annuler") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = UiAlignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Objets", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { reload++ }, enabled = !loading) {
                Icon(Icons.Filled.Refresh, contentDescription = "Actualiser")
            }
        }

        info?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        when {
            loading -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = UiAlignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }

            list.isEmpty() && error == null -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = UiAlignment.CenterHorizontally
            ) {
                Text("Aucun objet pour le moment", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ajoutez vos livres, outils, DVD ou appareils, et partagez-les avec vos cercles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddClick) { Text("Ajouter un objet") }
            }

            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                items(list, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            val partage = if (item.circles.isEmpty()) "Privé" else "${item.circles.size} cercle(s)"
                            Text(
                                "${categoryLabel(item.category)} · $partage",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                statusLabel(item.status),
                                style = MaterialTheme.typography.labelLarge,
                                color = statusColor(item.status)
                            )
                            if (item.owner == me) {
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = {
                                        selected = item.circles.toSet()
                                        toShare = item
                                    }) { Text("Partager") }
                                    TextButton(onClick = { toDelete = item }) { Text("Supprimer") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}