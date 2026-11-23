# Mutualisons

Application Android pour partager entre amis des livres, outils, DVD et appareils,
et pour échanger du temps.

Chaque groupe héberge son propre serveur (PocketBase, derrière un reverse proxy HTTPS).
L'application demande l'adresse du serveur au premier lancement.

## Structure

- `app/` : application Android (Kotlin, Jetpack Compose)
- `server/` : serveur PocketBase (Dockerfile, docker-compose, hooks, migrations)

## Installer son serveur

Chaque groupe peut héberger son propre serveur PocketBase. Le guide complet est dans [server/DEPLOIEMENT.md](server/DEPLOIEMENT.md).

### En bref

1. **Prérequis** : un domaine (ex. `pb.exemple.fr`), un serveur Linux avec Docker, une adresse IP publique.
2. **Configuration** :
   ```bash
   cd server
   cp .env.example .env   # puis renseigner PUBLIC_URL, ADMIN_NOTIFY_EMAIL, PROXY_NETWORK
   ```
3. **Lancement** :
   - sans reverse proxy : `docker compose up -d --build` (Caddy gère le HTTPS, voir `Caddyfile`) ;
   - avec Nginx Proxy Manager ou autre proxy : rattacher PocketBase au réseau du proxy, puis créer un hôte vers `pocketbase:8090` avec HTTPS et temps réel (`proxy_buffering off`).
4. **Administrateur** :
   ```bash
   docker exec -it pocketbase ./pocketbase superuser upsert VOTRE_EMAIL VOTRE_MOT_DE_PASSE
   ```
   Puis importer `pb_schema.json` dans l'administration (accessible uniquement en local, jamais sur le domaine public).
5. **Mails** : configurer le SMTP dans *Settings → Mail settings*, pour recevoir les demandes d'inscription.
6. **Test** : `https://pb.exemple.fr/api/health` doit répondre `"code":200`.

### Sauvegardes

Sauvegarder `pb_data/` et `.env` régulièrement, hors du serveur. L'archive contient des données personnelles : elle doit rester privée.

### Vie privée

Le serveur stocke les comptes, cercles, objets et prêts. Son administrateur en est responsable. Voir [PRIVACY.md](PRIVACY.md).

## Licence

GPL-3.0-or-later. Voir [LICENSE](LICENSE).

## Confidentialité

Voir [PRIVACY.md](PRIVACY.md).