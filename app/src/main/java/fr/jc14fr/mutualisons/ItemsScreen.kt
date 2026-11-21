package fr.jc14fr.mutualisons

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
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
    var loans by remember { mutableStateOf<Map<String, PocketBaseApi.ActiveLoan>>(emptyMap()) }
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }
    var me by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var toShare by remember { mutableStateOf<PocketBaseApi.Item?>(null) }
    var toDelete by remember { mutableStateOf<PocketBaseApi.Item?>(null) }
    var toRequest by remember { mutableStateOf<PocketBaseApi.Item?>(null) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var reqStart by remember { mutableStateOf("") }
    var reqEnd by remember { mutableStateOf("") }
    var reqError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            if (me.isEmpty()) me = PocketBaseApi.currentUserId(baseUrl, token)
            list = PocketBaseApi.listItems(baseUrl, token)
            circles = PocketBaseApi.listCircles(baseUrl, token)
            loans = list.mapNotNull { item ->
                PocketBaseApi.activeLoanInfo(baseUrl, token, item.id)?.let { item.id to it }
            }.toMap()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = c.id in selected,
                                onCheckedChange = { on ->
                                    selected = if (on) selected + c.id else selected - c.id
                                }
                            )
                            Text(if (c.creatorName.isNotBlank()) "${c.name} (par ${c.creatorName})" else c.name)
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

    toRequest?.let { item ->
        AlertDialog(
            onDismissRequest = { toRequest = null },
            title = { Text("Demander « ${item.title} »") },
            text = {
                Column {
                    Text(
                        "Période souhaitée, au format JJ/MM/AAAA",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reqStart,
                        onValueChange = { reqStart = it; reqError = null },
                        label = { Text("Du") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reqEnd,
                        onValueChange = { reqEnd = it; reqError = null },
                        label = { Text("Au") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    reqError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val startUtc = DateUtils.frToPbUtc(reqStart, endOfDay = false)
                    val endUtc = DateUtils.frToPbUtc(reqEnd, endOfDay = true)
                    if (startUtc == null || endUtc == null) {
                        reqError = "Dates invalides (JJ/MM/AAAA)."
                        return@TextButton
                    }
                    val target = item
                    toRequest = null
                    scope.launch {
                        try {
                            PocketBaseApi.createRequest(baseUrl, token, target.id, startUtc, endUtc)
                            info = "Demande envoyée pour « ${target.title} »."
                            error = null
                        } catch (e: AuthExpiredException) {
                            ServerSettings.clearToken(context)
                            onSessionExpired()
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }) { Text("Envoyer la demande") }
            },
            dismissButton = {
                TextButton(onClick = { toRequest = null }) { Text("Annuler") }
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
            verticalAlignment = Alignment.CenterVertically,
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }

            list.isEmpty() && error == null -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                    val ln = loans[item.id]
                    val isOpen = item.id in expanded
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expanded = if (isOpen) expanded - item.id else expanded + item.id
                                }
                                .padding(16.dp)
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)

                            if (isOpen) {
                                Spacer(Modifier.height(4.dp))
                                val partage = if (item.circles.isEmpty()) "Privé" else "${item.circles.size} cercle(s)"
                                Text(
                                    "${categoryLabel(item.category)} · $partage",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                    Text(
                                    if (item.owner == me) "Appartient à vous"
                                    else "Appartient à ${item.ownerName.ifBlank { "un membre" }}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    statusLabel(item.status),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = statusColor(item.status)
                                )

                                if (ln != null) {
                                    val ligne = when {
                                        ln.lenderId == me && ln.borrowerId == me -> null
                                        ln.lenderId == me -> "Prêté à ${ln.borrowerName}"
                                        ln.borrowerId == me -> "Prêté par ${ln.lenderName}"
                                        else -> "Prêté par ${ln.lenderName} à ${ln.borrowerName}"
                                    }
                                    ligne?.let {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val periode = "du ${DateUtils.dateFr(ln.startDate)} au ${DateUtils.dateFr(ln.dueDate)}"
                                    val texte = when {
                                        item.owner == me && ln.started -> "Jusqu'au ${DateUtils.dateFr(ln.dueDate)}"
                                        item.owner == me -> "Réservé $periode"
                                        ln.borrowerId == me && ln.started -> "À rendre le ${DateUtils.dateFr(ln.dueDate)}"
                                        ln.borrowerId == me -> "Vous avez réservé $periode"
                                        else -> "Indisponible $periode"
                                    }
                                    Text(texte, style = MaterialTheme.typography.bodyMedium)
                                }

                                if (item.owner == me) {
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        TextButton(onClick = {
                                            selected = item.circles.toSet()
                                            toShare = item
                                        }) { Text("Partager") }
                                        TextButton(onClick = { toDelete = item }) { Text("Supprimer") }
                                    }
                                }

                                if (item.owner == me && ln != null && ln.started) {
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        TextButton(onClick = {
                                            scope.launch {
                                                try {
                                                    PocketBaseApi.markReturned(baseUrl, token, ln.id)
                                                    info = "« ${item.title} » marqué rendu."
                                                    reload++
                                                } catch (e: AuthExpiredException) {
                                                    ServerSettings.clearToken(context)
                                                    onSessionExpired()
                                                } catch (e: Exception) {
                                                    error = e.message
                                                }
                                            }
                                        }) { Text("Marquer rendu") }
                                    }
                                }

                                if (item.owner != me && item.status != "hidden") {
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        TextButton(onClick = {
                                            reqStart = DateUtils.frDansJours(0)
                                            reqEnd = DateUtils.frDansJours(7)
                                            reqError = null
                                            toRequest = item
                                        }) { Text("Demander") }
                                    }
                                }
                            } else {
                                // Vue réduite : on indique seulement si l'objet est disponible
                                Text(
                                    statusLabel(item.status),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = statusColor(item.status)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}