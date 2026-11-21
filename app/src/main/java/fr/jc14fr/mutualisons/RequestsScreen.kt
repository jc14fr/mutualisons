package fr.jc14fr.mutualisons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private fun statusText(status: String): String = when (status) {
    "pending" -> "En attente"
    "accepted" -> "Acceptée"
    "refused" -> "Refusée"
    else -> status
}

@Composable
fun RequestsScreen(baseUrl: String, token: String, onSessionExpired: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var incoming by remember { mutableStateOf<List<PocketBaseApi.Req>>(emptyList()) }
    var outgoing by remember { mutableStateOf<List<PocketBaseApi.Req>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            val me = PocketBaseApi.currentUserId(baseUrl, token)
            incoming = PocketBaseApi.listRequests(baseUrl, token, "owner=\"$me\" && status=\"pending\"")
            outgoing = PocketBaseApi.listRequests(baseUrl, token, "requester=\"$me\"")
        } catch (e: AuthExpiredException) {
            ServerSettings.clearToken(context)
            onSessionExpired()
            return@LaunchedEffect
        } catch (e: Exception) {
            error = "Chargement impossible : ${e.message}"
        }
        loading = false
    }

    fun runAction(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                reload++
            } catch (e: AuthExpiredException) {
                ServerSettings.clearToken(context)
                onSessionExpired()
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Demandes", style = MaterialTheme.typography.headlineMedium)
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

        Spacer(Modifier.height(12.dp))
        Text("Demandes reçues", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        if (!loading && incoming.isEmpty()) {
            Text(
                "Aucune demande en attente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        incoming.forEach { r ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${r.requesterName.ifBlank { "Un membre" }} demande « ${r.itemTitle} »",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            runAction {
                                PocketBaseApi.respondRequest(baseUrl, token, r.id, "refused")
                                info = "Demande refusée."
                            }
                        }) { Text("Refuser") }
                        Button(onClick = {
                            runAction {
                                PocketBaseApi.respondRequest(baseUrl, token, r.id, "accepted")
                                info = "Prêt accepté : « ${r.itemTitle} » est maintenant prêté."
                            }
                        }) { Text("Accepter") }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Mes demandes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        if (!loading && outgoing.isEmpty()) {
            Text(
                "Vous n'avez fait aucune demande.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        outgoing.forEach { r ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(r.itemTitle.ifBlank { "Objet" }, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        statusText(r.status),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (r.status == "pending") {
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = {
                                runAction {
                                    PocketBaseApi.cancelRequest(baseUrl, token, r.id)
                                    info = "Demande annulée."
                                }
                            }) { Text("Annuler") }
                        }
                    }
                }
            }
        }
    }
}
