# FLE Présence

Application de suivi des présences pour les cours de **français langue étrangère** d'une association
organisée en unités locales (Chatou, Sartrouville…).

**Stack** : Java 17+ · Spring Boot 3.5 · Spring Security · Thymeleaf · MySQL 8 · Flyway ·
Bootstrap 5 + thème Bootswatch **Zephyr** · Google Fonts (Inter, Nunito) · Chart.js

## Qui fait quoi

| Profil | Accès | Peut faire |
|---|---|---|
| **Apprenant** | Page publique `/u/{ville}`, **sans compte** (lien ou QR code) | Voir les prochains cours, alertes en toast, **abonnement à l'agenda du téléphone** (.ics), **appli installable** et **notifications** des annulations |
| **Bénévole** | `/saisie` | Saisir *une fois* le nombre de bénévoles et d'apprenants présents à une séance de son unité |
| **Responsable** (1 par unité) | `/responsable` | Groupes et horaires, annuler / déplacer une séance, **annuler une période** (vacances), **modifier les saisies**, comptes bénévoles, **historique**, affiche QR code |
| **Administrateur** | `/admin` | Tableau de bord par date, mois et année, **heures de bénévolat**, **comparaison avec l'année précédente**, **bilan annuel imprimable (PDF)**, export CSV, historique, unités, comptes |

Aucune donnée personnelle des apprenants n'est enregistrée : uniquement des **nombres** de présents.
Mode clair / sombre, pied de page configurable, plan du site et page confidentialité.

## Essayer tout de suite (sans MySQL)

Prérequis : Java 17 (ou plus récent) et Maven (ou un IDE comme IntelliJ / Eclipse / VS Code).

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Puis ouvrir http://localhost:8080. Le profil `dev` utilise une base H2 en mémoire avec des données d'exemple :

| Identifiant | Mot de passe | Rôle |
|---|---|---|
| `admin` | `admin1234` | Administrateur |
| `resp.chatou` | `responsable` | Responsable de Chatou |
| `benevole.chatou` | `benevole1` | Bénévole de Chatou |
| `resp.sartrouville` | `responsable` | Responsable de Sartrouville |
| `benevole.sartrouville` | `benevole1` | Bénévole de Sartrouville |

Page publique d'exemple : http://localhost:8080/u/chatou (avec un cours annulé et un cours déplacé pour voir les toasts).

## Lancer avec MySQL (développement)

Avec un MySQL 8 sur le port 3307, par exemple le conteneur `fle-mysql` :

```bash
mvn spring-boot:run
```

Paramètres (variables d'environnement) :

| Variable | Défaut |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3307/fle_presence?...` |
| `DB_USER` / `DB_PASSWORD` | `fle` / `fle` |
| `APP_ADMIN_USERNAME` | `admin` |
| `APP_ADMIN_PASSWORD` | *(vide : un mot de passe aléatoire est affiché une fois dans les logs au premier démarrage)* |
| `PORT` | `8080` |
| `APP_LIEN_...` | liens du pied de page (French for Beginners, réseaux sociaux…), voir `.env.example` |

## Lancer avec Docker (application + MySQL 8 + phpMyAdmin)

```bash
cp .env.example .env        # Windows : copy .env.example .env   — puis choisir les mots de passe
docker compose up -d --build
```

- Application : http://localhost:8080
- phpMyAdmin : http://localhost:8081 (serveur `db`, utilisateur `fle` ou `root`)

La base MySQL n'est pas exposée sur la machine ; ses données sont dans le volume `fle-presence_db-data`.
Si l'application tourne déjà dans IntelliJ, changez `APP_PORT` dans `.env` pour éviter le conflit sur 8080.

Les sauvegardes automatiques de la base arrivent chaque nuit dans `./sauvegardes` (14 conservées).

**Mettre en ligne avec HTTPS** (serveur, nom de domaine, Caddy) : voir le guide [docs/DEPLOIEMENT.md](docs/DEPLOIEMENT.md).

**GitHub Actions** (`.github/workflows/ci.yml`) : tests à chaque `git push`, puis publication de l'image sur
Docker Hub si les secrets `DOCKERHUB_USERNAME` et `DOCKERHUB_TOKEN` sont définis dans le dépôt.

Publier l'image sur Docker Hub à la main :

```bash
docker compose build app
docker push rolandmerhej1/fle-presence:latest
```

Le schéma est créé automatiquement par Flyway (`src/main/resources/db/migration`).
Au premier démarrage sur une base vide, le compte administrateur est créé ; ensuite :

1. **Unités** → créer les unités locales ;
2. **Comptes** → créer un responsable par unité ;
3. le responsable crée ses **groupes** et leurs **horaires hebdomadaires** : les séances des 8 semaines
   suivantes sont générées automatiquement (puis prolongées chaque nuit à 3h) ;
4. le responsable imprime l'**affiche QR code** pour la salle de cours.

## Règles métier

- Un bénévole ne saisit que les séances **de son unité**, **déjà commencées** (aujourd'hui ou passées, 30 jours max).
- Une séance déjà saisie ne peut plus être modifiée par un bénévole : **seul le responsable** peut corriger
  (la modification est tracée : qui, quand).
- Une séance annulée ou déplacée ne peut pas recevoir de présences ; une séance qui a des présences ne peut pas être annulée.
- Un seul responsable **actif** par unité.

## Structure

```
src/main/java/fr/asso/fle
├── model/        entités JPA (UniteLocale, Groupe, Creneau, Seance, Presence, Utilisateur)
├── repository/   Spring Data JPA (+ requêtes statistiques)
├── service/      règles métier (séances, présences, statistiques, administration)
├── security/     Spring Security (rôles ADMIN / RESPONSABLE / BENEVOLE)
├── web/          contrôleurs (public, saisie, responsable, admin, compte)
└── config/       création du compte admin, données de démo (profil dev)
src/main/resources
├── templates/    pages Thymeleaf (layout commun dans fragments/layout.html)
├── static/       app.css, app.js
└── db/migration/ scripts Flyway
```

## Tests

```bash
mvn test
```

## Pistes pour la suite

- Déclaration d'accessibilité (RGAA) pour la page publique.
- Connexion des bénévoles par lien magique ou code, sans mot de passe.
