package fr.jc14fr.mutualisons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Confidentialité", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("Données sur le serveur", style = MaterialTheme.typography.titleMedium)
        Text(
            "Nom ou pseudo, adresse e-mail (mot de passe haché), cercles et membres, objets partagés, demandes et prêts, invitations.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text("Ce que l'application ne fait pas", style = MaterialTheme.typography.titleMedium)
        Text(
            "Aucun suivi publicitaire, aucune analyse d'usage, aucun SDK tiers. Aucune donnée n'est envoyée à l'auteur de l'application.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text("Qui voit quoi", style = MaterialTheme.typography.titleMedium)
        Text(
            "Les membres d'un cercle voient les objets partagés avec ce cercle et le pseudo des autres membres. L'adresse e-mail n'est visible que de l'administrateur du serveur.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text("Vos droits", style = MaterialTheme.typography.titleMedium)
        Text(
            "Vous pouvez modifier votre pseudo et votre e-mail, supprimer votre compte depuis Réglages, ou demander la suppression de vos données à l'administrateur de votre serveur.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text("Responsable", style = MaterialTheme.typography.titleMedium)
        Text(
            "L'administrateur du serveur que vous utilisez est responsable de ces données. Contact : apk@jc14fr.ovh",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}