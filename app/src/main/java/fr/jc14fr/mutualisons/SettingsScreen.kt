package fr.jc14fr.mutualisons

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val CONTACT_EMAIL = "apk@jc14fr.ovh"
private const val DONATION_URL = "https://revolut.me/jc14fr"

@Composable
fun SettingsScreen(
    baseUrl: String,
    token: String,
    onSessionExpired: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var userId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var originalEmail by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    LaunchedEffect(Unit) {
        try {
            val me = PocketBaseApi.currentUser(baseUrl, token)
            userId = me.id
            name = me.name
            email = me.email
            originalEmail = me.email
            loaded = true
        } catch (e: AuthExpiredException) {
            ServerSettings.clearToken(context)
            onSessionExpired()
        } catch (e: Exception) {
            error = "Chargement du profil impossible : ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Réglages", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        // Profil
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mon profil", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))

                if (loaded && name.isBlank()) {
                    Text(
                        "Ajoutez votre pseudo : c'est le nom que vos cercles verront.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; info = null; error = null },
                    label = { Text("Pseudo ou prénom") },
                    singleLine = true,
                    enabled = loaded,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; info = null; error = null },
                    label = { Text("E-mail") },
                    singleLine = true,
                    enabled = loaded,
                    modifier = Modifier.fillMaxWidth()
                )
                if (loaded && email.trim() != originalEmail && email.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Après changement, vous vous connecterez avec la nouvelle adresse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                info?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = loaded && !saving && name.isNotBlank() && email.contains("@"),
                    onClick = {
                        saving = true
                        error = null
                        info = null
                        val n = name.trim()
                        val m = email.trim()
                        scope.launch {
                            try {
                                PocketBaseApi.updateProfile(baseUrl, token, userId, n, m)
                                originalEmail = m
                                name = n
                                email = m
                                info = "Profil enregistré."
                            } catch (e: AuthExpiredException) {
                                ServerSettings.clearToken(context)
                                onSessionExpired()
                            } catch (e: Exception) {
                                error = e.message
                            }
                            saving = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Enregistrer mon profil") }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Compte
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Compte", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Se déconnecter n'efface rien sur le serveur.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { scope.launch { ServerSettings.clearToken(context) } },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Se déconnecter") }
            }
        }

        Spacer(Modifier.height(12.dp))

        // À propos
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("À propos", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Mutualisons · version $version",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CONTACT_EMAIL"))
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Mutualisons $version")
                        context.startActivity(Intent.createChooser(intent, "Écrire à l'auteur"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Me contacter") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL)))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Offrir un café ☕") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenChangelog,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Journal des modifications") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenPrivacy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confidentialité") }
            }
        }
    }
}