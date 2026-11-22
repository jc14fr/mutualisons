package fr.jc14fr.mutualisons

import android.content.Context

/**
 * Exécute un appel API. En cas de session expirée, efface le jeton et déclenche
 * la sortie vers la connexion. Les autres erreurs sont transmises à onError.
 * Renvoie null si l'appel a échoué.
 */
suspend fun <T> runApi(
    context: Context,
    onSessionExpired: () -> Unit,
    onError: (String) -> Unit,
    block: suspend () -> T
): T? = try {
    block()
} catch (e: AuthExpiredException) {
    ServerSettings.clearToken(context)
    onSessionExpired()
    null
} catch (e: Exception) {
    onError(e.message ?: "Erreur inconnue")
    null
}