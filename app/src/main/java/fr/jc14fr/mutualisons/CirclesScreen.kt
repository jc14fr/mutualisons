package fr.jc14fr.mutualisons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Mes cercles")
            Button(onClick = { reload++ }, enabled = !loading) { Text("Actualiser") }
        }

        OutlinedTextField(
            value = code,
            onValueChange = { code = it; info = null; error = null },
            label = { Text("Code d'invitation") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
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
            }
        ) { Text("Rejoindre") }

        if (loading) Text("Chargement…")
        info?.let { Text(it) }
        error?.let { Text(it) }
        if (!loading && error == null && list.isEmpty()) {
            Text("Vous n'êtes membre d'aucun cercle.")
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(list) { circle ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(circle.name)
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                val me = PocketBaseApi.currentUserId(baseUrl, token)
                                val newCode = PocketBaseApi.createInvitation(baseUrl, token, circle.id, me)
                                info = "Code créé pour « ${circle.name} » : $newCode"
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