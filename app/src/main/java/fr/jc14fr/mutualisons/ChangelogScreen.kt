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

private val CHANGELOG = listOf(
    "1.4.0" to listOf(
        "État des objets calculé à partir des prêts (disponible, réservé, prêté).",
        "Suppression bloquée pour un objet prêté.",
        "Propriétaire : bouton « Rendre indisponible » / « Remettre disponible ».",
        "Retour de prêt et réouverture gérés par le serveur.",
        "Code interne réorganisé."
    ),

    "1.3.0" to listOf(
        "Objets : fiche réduite, tap pour déplier les détails.",
        "Objets : propriétaire et prêts affichés sur chaque fiche.",
        "Réservations par période, avec vérification des chevauchements.",
        "Cercles : liste des membres au tap, créateur affiché.",
        "Profil : pseudo et e-mail modifiables.",
        "Demandes : mail au propriétaire avec demandeur, objet, période.",
        "Rappel automatique à l'emprunteur la veille de la fin du prêt.",
        "Interface refaite : navigation en bas, cartes, icône."
    ),
    "1.2.0" to listOf(
        "Réservations par période (version intermédiaire).",
        "Suppression d'un objet bloquée s'il est prêté."
    ),
    "1.1.0" to listOf(
        "Demandes de prêt, acceptation ou refus par le propriétaire.",
        "Mail au propriétaire à chaque demande."
    ),
    "1.0.0" to listOf(
        "Première version stable : inscription avec validation, objets, cercles, réglages."
    ),
    "0.3.0" to listOf("Inscription avec validation par lien mail, écrans cercles et invitations."),
    "0.2.0" to listOf("Connexion au serveur, liste des objets."),
    "0.1.0" to listOf("Écran de réglage de l'adresse du serveur.")
)

@Composable
fun ChangelogScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Journal des modifications", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        CHANGELOG.forEach { (version, lines) ->
            Text("Version $version", style = MaterialTheme.typography.titleMedium)
            lines.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(12.dp))
        }
    }
}