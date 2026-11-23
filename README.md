# Mutualisons

Application Android pour partager entre amis des livres, outils, DVD et appareils,
et pour échanger du temps.

Chaque groupe héberge son propre serveur (PocketBase, derrière un reverse proxy HTTPS).
L'application demande l'adresse du serveur au premier lancement.

## Structure

- `app/` : application Android (Kotlin, Jetpack Compose)
- `server/` : serveur PocketBase (Dockerfile, docker-compose, hooks, migrations)

## Licence

GPL-3.0-or-later. Voir [LICENSE](LICENSE).

## Confidentialité

Voir [PRIVACY.md](PRIVACY.md).