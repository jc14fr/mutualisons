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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(baseUrl: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        if (done) {
            Text("Compte créé. Un administrateur doit le valider avant votre première connexion.")
            Button(onClick = onBack) { Text("Retour à la connexion") }
        } else {
            Text("Créer un compte")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Prénom ou pseudo") },
                singleLine = true
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("E-mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Mot de passe (8 caractères minimum)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it; error = null },
                label = { Text("Confirmer le mot de passe") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            error?.let { Text(it) }
            Button(
                enabled = !loading,
                onClick = {
                    when {
                        name.isBlank() -> error = "Indiquez un prénom ou un pseudo."
                        !email.contains("@") -> error = "Adresse e-mail invalide."
                        password.length < 8 -> error = "Le mot de passe doit contenir au moins 8 caractères."
                        password != confirm -> error = "Les deux mots de passe ne correspondent pas."
                        else -> {
                            loading = true
                            scope.launch {
                                try {
                                    PocketBaseApi.signup(baseUrl, email, name, password)
                                    done = true
                                } catch (e: Exception) {
                                    error = e.message
                                }
                                loading = false
                            }
                        }
                    }
                }
            ) { Text("Créer mon compte") }
            TextButton(onClick = onBack) { Text("Retour") }
        }
    }
}