# Mettre FLE Présence en ligne

Objectif : une adresse comme **https://cours.mon-association.fr**, accessible depuis n'importe quel téléphone,
pour que le QR code de l'affiche fonctionne et que les notifications partent.

Ce qu'il faut :

| Élément | Exemple | Coût indicatif |
|---|---|---|
| Un petit serveur Linux (VPS), 2 Go de mémoire | OVHcloud, Scaleway, Hetzner, o2switch… | ~4 à 7 € / mois |
| Un nom de domaine (ou un sous-domaine de celui de l'association) | `cours.mon-association.fr` | ~10 € / an, ou gratuit si sous-domaine |
| L'image Docker publiée sur Docker Hub | `rolandmerhej1/fle-presence:latest` | gratuit |

> Avant de choisir, demandez à la délégation : l'association a peut-être déjà un hébergeur
> ou un nom de domaine qu'on peut utiliser.

---

## 1. Publier l'image Docker

Deux possibilités :

**Automatique (recommandé)** — GitHub Actions construit et publie l'image à chaque `git push` sur `main` :

1. Sur Docker Hub : *Account settings → Personal access tokens → Generate new token* (droits *Read & Write*).
2. Sur GitHub, dans le dépôt : *Settings → Secrets and variables → Actions → New repository secret* :
   - `DOCKERHUB_USERNAME` = `rolandmerhej1`
   - `DOCKERHUB_TOKEN` = le jeton créé à l'étape 1
3. `git push` : l'onglet **Actions** du dépôt montre les tests puis la publication de l'image.

**À la main**, depuis votre PC :

```powershell
docker compose build app
docker login
docker push rolandmerhej1/fle-presence:latest
```

## 2. Préparer le serveur

Après avoir loué le VPS (choisir **Ubuntu 24.04**), connectez-vous depuis PowerShell :

```powershell
ssh root@ADRESSE_IP_DU_SERVEUR
```

Puis, sur le serveur :

```bash
# Docker
curl -fsSL https://get.docker.com | sh

# Pare-feu : SSH + web uniquement
ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw --force enable

# Récupérer le projet
git clone https://github.com/Rmerhej/fle-presence.git
cd fle-presence
```

## 3. Faire pointer le nom de domaine

Chez le fournisseur du domaine, créez un enregistrement DNS :

| Type | Nom | Valeur |
|---|---|---|
| `A` | `cours` | l'adresse IP du serveur |

La propagation prend de quelques minutes à quelques heures. Vérification : `ping cours.mon-association.fr`
doit répondre avec l'adresse du serveur.

## 4. Configurer

```bash
cp .env.example .env
nano .env
```

À remplir **obligatoirement** :

- `DB_PASSWORD`, `DB_ROOT_PASSWORD`, `APP_ADMIN_PASSWORD` : des mots de passe longs et différents ;
- `DOMAINE` : `cours.mon-association.fr` ;
- `APP_NOTIFICATIONS_SUJET` : `mailto:` suivi d'un email de contact de l'association.

(`Ctrl + O` pour enregistrer, `Ctrl + X` pour quitter.)

## 5. Démarrer

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f app
```

Attendez `Started FlePresenceApplication`, puis ouvrez **https://cours.mon-association.fr**.
Le certificat HTTPS est obtenu automatiquement par Caddy au premier accès.

Pour ne pas retaper les deux `-f` à chaque fois :

```bash
echo 'COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml' >> .env
docker compose ps
```

## 6. Mettre à jour après une modification

**Automatique** : après chaque `git push` sur `main`, GitHub Actions lance les tests, publie la nouvelle
image, puis se connecte au serveur et la met en ligne (étape « Mise en ligne »). Il vérifie ensuite que
`/actuator/health` répond `UP` ; sinon l'étape échoue en rouge et affiche les journaux de l'application.

Mise en place (une seule fois) :

1. Sur le PC (Git Bash), créer une clé réservée au déploiement, sans mot de passe :
   `ssh-keygen -t ed25519 -f ~/.ssh/fle_deploiement -N "" -C "github-actions-deploiement"`
2. L'autoriser sur le serveur :
   `cat ~/.ssh/fle_deploiement.pub | ssh -i ~/.ssh/oracle_rsa_4096 ubuntu@ADRESSE_IP 'cat >> ~/.ssh/authorized_keys'`
3. GitHub → dépôt → Settings → Secrets and variables → Actions → New repository secret :
   `SERVEUR_HOTE` (adresse IP), `SERVEUR_UTILISATEUR` (`ubuntu`) et `SERVEUR_CLE_SSH`
   (tout le contenu de `~/.ssh/fle_deploiement`, lignes BEGIN et END comprises).

**À la main**, si besoin, sur le serveur :

```bash
cd fle-presence
git pull
docker compose pull app
docker compose up -d
```

## 7. Sauvegardes

- Le service `sauvegarde` exporte la base chaque nuit (vers 2 h) dans `fle-presence/sauvegardes/`,
  et garde les 14 dernières.
- **Copie hors du serveur** (une sauvegarde sur le même serveur ne protège pas d'une panne du serveur) :
  le service `envoi-sauvegardes` envoie chaque nouvelle sauvegarde dans un bucket Oracle Object Storage
  (gratuit jusqu'à 20 Go), puis prévient Healthchecks.io. Réglages dans `.env` : `SAUVEGARDE_PAR_URL`
  et `HEALTHCHECKS_URL` (voir ci-dessous). Suivi : `docker compose logs envoi-sauvegardes`.
- Copie manuelle sur votre PC, si besoin : `scp ubuntu@ADRESSE_IP:fle-presence/sauvegardes/*.sql.gz .`
- Restaurer une sauvegarde :

  ```bash
  gunzip -c sauvegardes/fle_presence_2026-11-02_0200.sql.gz | \
    docker compose exec -T db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" fle_presence'
  ```

### Mettre en place la copie hors du serveur (Oracle Object Storage)

1. Console Oracle → **Storage → Buckets → Create Bucket** : nom `fle-sauvegardes`, niveau *Standard*,
   chiffrement par Oracle (par défaut).
2. Dans le bucket → **Pre-Authenticated Requests → Create** : cible *Bucket*, accès
   **Permit object writes** (écriture seule : l'URL ne permet pas de lire les sauvegardes),
   date d'expiration lointaine (ex. dans 3 ans, à noter dans l'agenda). Copier l'URL affichée
   **une seule fois** dans `SAUVEGARDE_PAR_URL`.
3. Suppression automatique après 30 jours : dans le bucket → **Lifecycle Policy Rules → Create Rule** :
   *Delete*, 30 jours. Si Oracle demande une autorisation, ajouter dans **Identity → Policies**
   (compartiment racine) : `Allow service objectstorage-eu-paris-1 to manage object-family in tenancy`.

### Surveiller les sauvegardes (Healthchecks.io)

1. Créer un compte gratuit sur healthchecks.io, puis un *check* : période **1 jour**, délai de grâce **2 heures**.
2. Copier son URL de ping (`https://hc-ping.com/…`) dans `HEALTHCHECKS_URL`.
3. Sans copie réussie pendant plus de 26 heures, Healthchecks envoie un e-mail.

Après avoir modifié `.env` : `docker compose up -d`.

### Tester une restauration (tous les 3 mois)

Télécharger une sauvegarde depuis le bucket (console Oracle → bucket → objet → *Download*) et la restaurer
sur le PC, dans la base de test, avec la commande ci-dessus. Une sauvegarde jamais restaurée n'est pas une
vraie sauvegarde.

## 8. phpMyAdmin sur le serveur

Il n'écoute que sur le serveur lui-même (127.0.0.1), donc il n'est **pas** visible sur Internet.
Pour l'utiliser depuis votre PC, ouvrez un tunnel SSH :

```powershell
ssh -L 8081:127.0.0.1:8081 root@ADRESSE_IP_DU_SERVEUR
```

puis ouvrez http://localhost:8081 sur votre PC (tant que la fenêtre SSH reste ouverte).

## En cas de problème

| Symptôme | Piste |
|---|---|
| Le site ne répond pas | `docker compose ps` : tous les services doivent être *running* |
| Erreur de certificat HTTPS | le DNS ne pointe pas encore vers le serveur ; vérifier avec `ping`, puis `docker compose restart caddy` |
| L'application redémarre en boucle | `docker compose logs app` : souvent un mot de passe de base différent entre `.env` et un ancien volume |
| Les notifications n'arrivent pas sur iPhone | l'appli doit être installée sur l'écran d'accueil (iOS 16.4 minimum), et `APP_NOTIFICATIONS_SUJET` doit être un vrai `mailto:` |
