# Mode d'emploi : déployer PocketBase en auto-hébergement

Ce guide couvre deux cas : un serveur dédié sans reverse proxy existant (Caddy fournit le HTTPS), et un serveur qui utilise déjà un reverse proxy (Nginx Proxy Manager, Traefik ou autre). PocketBase écoute sur un port interne, n'est jamais exposé directement sur Internet, et un reverse proxy assure le HTTPS.

## Sommaire

1. Prérequis
2. Préparer le serveur
3. Installer Docker
4. Récupérer le projet
5. Le Dockerfile
6. Le fichier `.env`
7. Déploiement A : sans reverse proxy (Caddy)
8. Déploiement B : avec Nginx Proxy Manager
9. Déploiement C : autre reverse proxy
10. Créer le superutilisateur et importer le schéma
11. Configurer l'envoi de mails
12. Tester l'installation
13. Sauvegardes
14. Mise à jour
15. Dépannage

---

## 1. Prérequis

- Un nom de domaine, par exemple `pb.exemple.fr`, dont vous contrôlez le DNS.
- Un serveur Linux (Debian ou Ubuntu récents) avec une adresse IPv4 publique, ou un Raspberry Pi avec une IP publique fixe.
- Un accès SSH par clé.
- Pour un serveur à domicile : une box qui autorise la redirection des ports 80 et 443, et une IP fixe (sinon DDNS).
- Une boîte mail SMTP pour l'envoi des demandes et des rappels.

## 2. Préparer le serveur

Créez un utilisateur non root et sécurisez SSH :

```bash
adduser admin
usermod -aG sudo admin
```

Depuis votre poste : `ssh-copy-id admin@IP_DU_SERVEUR`. Vérifiez la connexion par clé, puis dans `/etc/ssh/sshd_config` :

```
PasswordAuthentication no
PermitRootLogin no
```

Redémarrez SSH (`sudo systemctl restart ssh`) en gardant une session ouverte.

Pare-feu :

```bash
sudo apt update && sudo apt install -y ufw
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

Le port 8090 de PocketBase ne doit jamais être ouvert sur Internet.

Mises à jour de sécurité automatiques :

```bash
sudo apt install -y unattended-upgrades
sudo dpkg-reconfigure -plow unattended-upgrades
```

## 3. Installer Docker

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
```

Déconnectez-vous puis reconnectez-vous, et vérifiez : `docker version` et `docker compose version`.

Ne donnez jamais de permissions `777` à votre dossier personnel.

## 4. Récupérer le projet

```bash
sudo apt install -y git
git clone https://github.com/jc14fr/mutualisons.git
cd mutualisons/server
```

Le dossier `server/` contient le `Dockerfile`, les hooks (`pb_hooks/`), le schéma (`pb_schema.json`) et `.env.example`.

## 5. Le Dockerfile

Le Dockerfile du dépôt télécharge le binaire ARM64 (Raspberry Pi). Sur un serveur x86_64, remplacez son contenu par :

```dockerfile
FROM alpine:3.20

ARG PB_VERSION=0.40.6
ARG TARGETARCH

RUN apk add --no-cache ca-certificates unzip wget \
 && wget -q "https://github.com/pocketbase/pocketbase/releases/download/v${PB_VERSION}/pocketbase_${PB_VERSION}_linux_${TARGETARCH}.zip" -O /tmp/pb.zip \
 && unzip /tmp/pb.zip -d /pb \
 && rm /tmp/pb.zip \
 && chmod +x /pb/pocketbase

WORKDIR /pb
EXPOSE 8090

CMD ["./pocketbase", "serve", "--http=0.0.0.0:8090"]
```

Vérifiez la dernière version sur la page des releases de PocketBase et adaptez `PB_VERSION`.

## 6. Le fichier `.env`

```bash
cp .env.example .env
nano .env
```

Renseignez :

```
# Adresse publique, en HTTPS, sans / final
PUBLIC_URL=https://pb.exemple.fr

# Adresse qui reçoit les demandes d'inscription et de prêt
ADMIN_NOTIFY_EMAIL=vous@exemple.fr

# Réseau Docker partagé avec le reverse proxy (déploiement B et C)
PROXY_NETWORK=proxy
```

Ce fichier n'est jamais versionné.

## 7. Déploiement A : sans reverse proxy (Caddy)

Caddy obtient et renouvelle seul le certificat HTTPS. Dans `server/`, créez `docker-compose.yml` :

```yaml
services:
  pocketbase:
    build: .
    container_name: pocketbase
    restart: unless-stopped
    environment:
      - ADMIN_NOTIFY_EMAIL=${ADMIN_NOTIFY_EMAIL}
      - PUBLIC_URL=${PUBLIC_URL}
    volumes:
      - ./pb_data:/pb/pb_data
      - ./pb_hooks:/pb/pb_hooks
      - ./pb_migrations:/pb/pb_migrations
    ports:
      # Administration uniquement, depuis le serveur lui-même
      - "127.0.0.1:8090:8090"
    networks:
      - web

  caddy:
    image: caddy:2
    container_name: caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    networks:
      - web

networks:
  web:

volumes:
  caddy_data:
  caddy_config:
```

Puis `Caddyfile`, à côté :

```
pb.exemple.fr {
    @admin path /_/*
    respond @admin 404

    reverse_proxy pocketbase:8090
}
```

Remplacez `pb.exemple.fr` par votre domaine. Lancez :

```bash
docker compose up -d --build
docker compose logs -f caddy
```

Le message `certificate obtained successfully` confirme le HTTPS. Le DNS doit déjà pointer vers le serveur (`dig +short pb.exemple.fr`).

## 8. Déploiement B : avec Nginx Proxy Manager

NPM tourne déjà dans Docker. PocketBase rejoint son réseau, sans publier le port 8090.

`docker-compose.yml` :

```yaml
services:
  pocketbase:
    build: .
    container_name: pocketbase
    restart: unless-stopped
    environment:
      - ADMIN_NOTIFY_EMAIL=${ADMIN_NOTIFY_EMAIL}
      - PUBLIC_URL=${PUBLIC_URL}
    volumes:
      - ./pb_data:/pb/pb_data
      - ./pb_hooks:/pb/pb_hooks
      - ./pb_migrations:/pb/pb_migrations
    networks:
      - proxy

networks:
  proxy:
    name: ${PROXY_NETWORK}
    external: true
```

Vérifiez le nom du réseau de NPM avec `docker network ls` (souvent `nginx-proxy-manager_default`) et renseignez-le dans `PROXY_NETWORK`. Puis :

```bash
docker compose up -d --build
docker exec nginx-proxy-manager getent hosts pocketbase
```

Une adresse IP doit s'afficher.

Dans NPM, **Hosts → Proxy Hosts → Add Proxy Host** :
- Domain Names : `pb.exemple.fr`
- Scheme : `http`, Forward Hostname : `pocketbase`, Forward Port : `8090`
- Cochez **Block Common Exploits** et **Websockets Support**

Onglet **SSL** : certificat Let's Encrypt neuf, **Force SSL** et **HTTP/2** cochés.

Onglet **Advanced** :

```nginx
proxy_buffering off;
proxy_cache off;
proxy_read_timeout 1h;

location /_/ {
    return 404;
}
```

Sur la box, redirigez 80 et 443 vers le serveur (DDNS si l'IP change).

Pour l'administration, passez par un tunnel SSH : publiez temporairement `127.0.0.1:8095:8090` dans `ports:`, puis `ssh -L 8095:127.0.0.1:8095 admin@IP_DU_SERVEUR` et ouvrez `http://127.0.0.1:8095/_/`.

## 9. Déploiement C : autre reverse proxy

Le principe est identique : PocketBase écoute sur `8090` dans un réseau que le proxy peut joindre.

- **Traefik** : labels sur le service `pocketbase` :
  ```yaml
      labels:
        - "traefik.enable=true"
        - "traefik.http.routers.pb.rule=Host(`pb.exemple.fr`)"
        - "traefik.http.routers.pb.entrypoints=websecure"
        - "traefik.http.routers.pb.tls.certresolver=letsencrypt"
        - "traefik.http.services.pb.loadbalancer.server.port=8090"
  ```
- **Nginx ou Apache sur l'hôte** : publiez `127.0.0.1:8090:8090`, puis `proxy_pass http://127.0.0.1:8090;` dans le bloc HTTPS, avec `proxy_buffering off;` et un `proxy_read_timeout` long, et masquez `/_/`.

Dans tous les cas, le proxy transmet `Host` et `X-Forwarded-*`, ne met pas en cache l'API, et n'expose jamais `/_/` sur le domaine public.

## 10. Superutilisateur et schéma

Créez le compte administrateur, en saisissant le mot de passe à l'invite :

```bash
read -s -p "Mot de passe superutilisateur : " PWD; echo
docker exec -it pocketbase ./pocketbase superuser upsert vous@exemple.fr "$PWD"
unset PWD
history -c
```

Accédez à l'administration via le tunnel SSH, puis **Settings → Import/Export → Import collections** et sélectionnez `pb_schema.json`. Ne validez aucune suppression touchant `_superusers`.

Contrôlez les règles de `users` : `Create` vide (inscription ouverte), `List` et `View` à `approved = true`.

## 11. Envoi de mails

Dans **Settings → Mail settings** : sender name `Mutualisons`, sender address égale à l'adresse du compte SMTP, host, port 587 (STARTTLS), identifiants, TLS activé. Testez depuis l'admin. En cas d'échec, `docker compose logs --tail 50 pocketbase` donne l'erreur SMTP.

## 12. Tester

1. `curl -s https://pb.exemple.fr/api/health` renvoie `"code":200`.
2. `https://pb.exemple.fr/_/` renvoie 404.
3. Une inscription de test crée un compte avec `approved` à `false`.
4. Le mail de demande arrive à `ADMIN_NOTIFY_EMAIL`.
5. Après validation, la connexion fonctionne. Le lien déjà utilisé est refusé.
6. Supprimez le compte de test.

## 13. Sauvegardes

Les données sont dans `pb_data/`. Sauvegardez `pb_data`, `pb_hooks` et `.env` chaque jour, copiés hors du serveur (par exemple avec `rclone`). L'archive contient des données personnelles : elle doit rester privée. Testez une restauration régulièrement : une sauvegarde jamais restaurée n'est pas fiable.

## 14. Mise à jour

1. Sauvegardez.
2. Lisez les notes de version de PocketBase.
3. Modifiez `PB_VERSION` dans le Dockerfile.
4. `git pull`, puis `docker compose up -d --build`.
5. Vérifiez `docker compose logs --tail 50 pocketbase` et `/api/health`.

## 15. Dépannage

- **Certificat HTTPS impossible** : DNS non propagé, ou ports 80/443 fermés.
- **502 Bad Gateway** : le proxy ne joint pas `pocketbase:8090` (conteneur arrêté, ou réseau différent).
- **404 sur `/api/health`** : PocketBase vient de redémarrer, ou le proxy route mal. Testez `curl http://127.0.0.1:8090/api/health` sur le serveur.
- **Temps réel par paquets** : `proxy_buffering off` manque.
- **`Failed to create record`** : la cause est dans `docker logs --tail 80 pocketbase`.
- **Hook non chargé** : erreur de syntaxe JavaScript au démarrage, puis `docker compose restart pocketbase`.
- **Mail absent** : testez l'envoi depuis l'admin, puis vérifiez les spams.