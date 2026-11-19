package fr.jc14fr.mutualisons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(baseUrl: String, onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var identity by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var signup by remember { mutableStateOf(false) }

    if (signup) {
        SignupScreen(baseUrl = baseUrl, onBack = { signup = false })
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("Connexion")
            OutlinedTextField(
                value = identity,
                onValueChange = { identity = it },
                label = { Text("E-mail ou identifiant") },
                singleLine = true
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mot de passe") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            error?.let { Text(it) }
            Button(
                enabled = !loading,
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            val token = PocketBaseApi.login(baseUrl, identity.trim(), password)
                            ServerSettings.saveToken(context, token)
                            onLoggedIn()
                        } catch (e: Exception) {
                            error = e.message
                            loading = false
                        }
                    }
                }
            ) { Text("Se connecter") }
            TextButton(onClick = { signup = true }) { Text("Créer un compte") }
        }
    }
}