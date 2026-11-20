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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CirclesScreen(baseUrl: String, token: String, onSessionExpired: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<PocketBaseApi.Circle>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    var toLeave by remember { mutableStateOf<PocketBaseApi.Circle?>(null) }
    var code by remember { mutableStateOf("") }
    var joining by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            list = PocketBaseApi.listCircles(baseUrl, token)
        } catch (e: AuthExpiredException) {
            ServerSettings.clearToken(context)
            onSessionExpired()
            return@LaunchedEffect
        } catch (e: Exception) {
            error = "Chargement impossible : ${e.message}"
        }
        loading = false
    }

    toLeave?.let { circle ->
        AlertDialog(
            onDismissRequest = { toLeave = null },
            title = { Text("Quitter « ${circle.name} » ?") },
            text = {
                Text("Vos objets restent à vous, mais ne seront plus partagés dans ce cercle. " +
                    "Vous ne pouvez pas partir tant qu'un prêt est en cours.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = circle
                    toLeave = null
                    scope.launch {
                        try {
                            PocketBaseApi.leaveCircle(baseUrl, token, target.id)
                            info = "Vous avez quitté « ${target.name} »."
                            reload++
                        } catch (e: AuthExpiredException) {
                            ServerSettings.clearToken(context)
                            onSessionExpired()
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }) { Text("Quitter") }
            },
            dismissButton = {
                TextButton(onClick = { toLeave = null }) { Text("Annuler") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mes cercles", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { reload++ }, enabled = !loading) {
                Icon(Icons.Filled.Refresh, contentDescription = "Actualiser")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Création d'un cercle
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Créer un cercle", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it; info = null; error = null },
                    label = { Text("Nom (ex. Famille, Club de lecture)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = !creating && newName.isNotBlank(),
                    onClick = {
                        creating = true
                        error = null
                        info = null
                        val name = newName.trim()
                        scope.launch {
                            try {
                                PocketBaseApi.createCircle(baseUrl, token, name)
                                info = "Cercle « $name » créé. Invitez vos proches avec le bouton Inviter."
                                newName = ""
                                reload++
                            } catch (e: AuthExpiredException) {
                                ServerSettings.clearToken(context)
                                onSessionExpired()
                            } catch (e: Exception) {
                                error = e.message
                            }
                            creating = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Créer le cercle") }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Rejoindre avec un code
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Rejoindre un cercle", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; info = null; error = null },
                    label = { Text("Code d'invitation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    enabled = !joining && code.isNotBlank(),
                    onClick = {
                        joining = true
                        error = null
                        info = null
                        scope.launch {
                            try {
                                val name = PocketBaseApi.joinCircle(baseUrl, token, code)
                                info = "Vous avez rejoint « $name »."
                                code = ""
                                reload++
                            } catch (e: AuthExpiredException) {
                                ServerSettings.clearToken(context)
                                onSessionExpired()
                            } catch (e: Exception) {
                                error = e.message
                            }
                            joining = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Rejoindre") }
            }
        }

        Spacer(Modifier.height(8.dp))

        info?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        when {
            loading -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }

            list.isEmpty() && error == null -> Text(
                "Vous n'êtes membre d'aucun cercle.",
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                items(list, key = { it.id }) { circle ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(circle.name, style = MaterialTheme.typography.titleMedium)
                            Row {
                                TextButton(onClick = {
                                    scope.launch {
                                        try {
                                            val me = PocketBaseApi.currentUserId(baseUrl, token)
                                            val newCode = PocketBaseApi.createInvitation(baseUrl, token, circle.id, me)
                                            info = "Code pour « ${circle.name} » : $newCode"
                                            error = null
                                        } catch (e: AuthExpiredException) {
                                            ServerSettings.clearToken(context)
                                            onSessionExpired()
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                    }
                                }) { Text("Inviter") }
                                TextButton(onClick = { toLeave = circle }) { Text("Quitter") }
                            }
                        }
                    }
                }
            }
        }
    }
}