# Mario Sprint

Application Android Kotlin en portrait : **neuf parcours originaux**, trois difficultés par univers, missions, quatre personnages, commandes à deux pouces, bonus, chronomètre et classements **Supabase** par niveau. Version 1.5.1 (code 7). Ouvrir ce répertoire dans Android Studio ou VS Code.

## Dépôt Git

Le dépôt [iSreaK/Super-Mar.io](https://github.com/iSreaK/Super-Mar.io) contient uniquement le projet MarioSprint, sur la branche `main`. Le dossier parent et les autres projets Android ne font pas partie du dépôt.

Le wrapper Gradle, les sources, les ressources, les tests, les schémas Room et les migrations SQL sont versionnés. Les caches, APK, logs, captures, configurations locales et clés de signature sont exclus. Les fichiers `local.properties` et `supabase.properties` doivent être renseignés sur chaque poste ; le modèle Supabase est fourni sans identifiants réels. La configuration du service Supabase reste à effectuer selon le guide ci-dessous.

## Jouer

L'accueil se concentre sur le parcours : choix de l'univers et de la difficulté, aperçu, meilleur temps, puis accès au vestiaire. **Jouer** et le classement restent accessibles en bas de l'écran pendant le défilement. L'icône de profil ouvre **Mon compte** (Google, synchronisation et son) ; le point d'interrogation ouvre **Comment jouer** (commandes, bonus et règles des records). Le vestiaire présente les personnages avant leurs missions. Les menus utilisent la même palette marine, corail, menthe et or, une largeur de lecture bornée et des boutons extensibles pour les grands caractères.

Choisir un univers (Prairie, Canyon ou Nuit), puis une difficulté (Facile, Normal ou Expert). Chaque combinaison possède son parcours fixe, son décor et ses records. Les trois parcours de la version 1.3 gardent leur géométrie et leur identifiant. Stick à gauche : inclinaison partielle pour marcher, complète pour courir. Grand bouton à droite : saut ; maintenir pour sauter plus haut. Les deux doigts sont indépendants. Clavier : Q/D ou flèches, Maj/X pour courir, Espace/Z pour sauter.

| Univers | Facile | Normal | Expert |
|---|---|---|---|
| Prairie | Prairie des départs | Sentiers de la prairie | Sommets verdoyants |
| Canyon | Oasis du canyon | Canyon des passerelles | Crêtes du canyon |
| Nuit | Chemin des lanternes | Course de l'observatoire | Nuit des étoiles |

Les blocs ? donnent une pièce ou un champignon. Le champignon fait grandir et permet de casser les briques. Trois bonus supplémentaires sont disponibles dans chaque niveau : **étoile** (8 s contre les ennemis), **bouclier** (un choc absorbé), **aimant** (pièces proches pendant 10 s). Une chute reste mortelle. Les effets actifs sont affichés sous le chrono ; ils ne retirent jamais de temps au chronomètre.

Le chrono démarre au premier mouvement et suit le temps réel. Une pause, une perte de focus ou un délai entre images supérieur à 500 ms passe la tentative en entraînement. Recommencer repart sur un parcours neuf et classable. Durée maximale : 10 minutes. Les essais, victoires, pièces et meilleurs temps sont sauvegardés par niveau.

## Missions et personnages

Luigi remplace le personnage initial et reste disponible immédiatement. **Missions & personnages**, sur l'accueil, montre la progression et permet d'équiper les récompenses :

- **Toad** : terminer un premier niveau sans pause.
- **Yoshi** : terminer au moins un niveau dans chacun des trois univers.
- **Peach** : terminer les trois parcours Expert.

Chaque niveau ne compte qu'une fois ; une course interrompue ou inférieure à 10 secondes ne valide pas de mission. Les personnages ont des dessins et animations distincts, mais exactement les mêmes déplacements et collisions. Le vestiaire, l'aperçu et la partie affichent le personnage équipé.

Room conserve la progression par UID et un profil invité séparé. Une course invitée n'est jamais transférée à un compte ; changer de compte pendant une course n'attribue pas de récompense. Hors connexion, les déblocages et le choix du personnage sont conservés localement. Après reconnexion, **Synchroniser** envoie d'abord les records en attente, récupère les missions déduites par Supabase des records du compte, puis sauvegarde le personnage si nécessaire. Une révision locale préserve un choix fait pendant une réponse réseau. Les records déjà en ligne permettent aussi de retrouver les missions sur un autre téléphone.

## Supabase et Google

Suivre le [guide complet pas à pas](docs/SUPABASE_SETUP.md). Le script SQL prêt à exécuter est dans [supabase/migrations](supabase/migrations/202610150001_levels_and_rankings.sql), avec des [contrôles de permissions en lecture seule](supabase/checks.sql).

**Projet Supabase déjà configuré en 1.3 :** exécuter uniquement la [nouvelle migration des niveaux, missions et personnages](supabase/migrations/202611180001_courses_missions_and_skins.sql), puis checks.sql. Elle ajoute six niveaux, les catégories, une table d'apparence et deux RPC protégées. Elle conserve les records. Pour une première installation, appliquer les deux migrations dans l'ordre chronologique. Sans cette extension, les nouveaux parcours et le vestiaire fonctionnent localement mais leur synchronisation en ligne échoue.

Sans configuration, tout le jeu local fonctionne. Pour activer Google et le réseau, copier supabase.properties.example vers supabase.properties puis renseigner l'URL du projet, sa clé publique sb_publishable_ et le Client ID OAuth **Web**. Ne jamais inclure de clé secrète/service_role, mot de passe Postgres ou secret OAuth dans Android. Aucun google-services.json ni service Firebase n'est utilisé.

Room est la source de vérité des records et du cache. La session Supabase est chiffrée par AES/GCM avec une clé Android Keystore non exportable ; les jetons ne sont pas journalisés. Le nonce Google est vérifié par Supabase (empreinte envoyée à Google, valeur brute à Supabase). Les jetons expirés sont renouvelés à la demande ; les sessions invalides sont supprimées localement.

Le serveur déduit l'UID de la session Google, accepte seulement une amélioration atomique du temps et retourne au maximum 50 joueurs par niveau, triés par temps puis UID. Aucun email, jeton ou photo n'est publié. Les écritures directes sont interdites ; les fonctions RPC contrôlent leur appelant avec RLS activée sur les tables.

Une course invitée reste locale. Se connecter pendant une course ne la réattribue pas au compte. Les envois échoués restent liés au niveau et à l'UID ; Synchroniser permet de réessayer. Un acquittement ne peut supprimer un meilleur résultat arrivé pendant l'envoi. Aucun polling ni service réseau en arrière-plan. Un client modifié peut falsifier son chrono : une compétition publique nécessiterait une validation du parcours côté serveur.

## Compiler et installer

Chaîne conservée : **AGP 9.4.0, Kotlin 2.2.10, Gradle 9.6.0, SDK 36, Compose BOM 2026.06.01**, minimum Android 7/API 24, JDK 21 installé sur ce PC. Kotlin est intégré à AGP 9. Toutes les versions sont centralisées dans le catalogue Gradle. SDK local : C:/Android/Sdk ; adapter local.properties sur un autre PC.

    .\tools\android.ps1 -Action build
    .\tools\android.ps1 -Action test
    .\tools\android.ps1 -Action install -Device d2627d0d
    .\tools\android.ps1 -Action sha

**test exécute uniquement les vérifications JVM et lint sur le PC. install met à jour l'APK sans ouvrir l'app.** APK : app/build/outputs/apk/debug/app-debug.apk. La tâche VS Code d'installation demande le numéro du téléphone.

**Règle obligatoire :** toute action sur le téléphone autre qu'installer/mettre à jour exige l'accord explicite préalable de Julie, y compris ouvrir l'app, capturer, lire ses données, diagnostiquer, exécuter ou désinstaller des tests. Les options run et test -Connected sont bloquées sans le drapeau AllowPhoneInteraction, qui ne remplace pas cet accord. Ne pas effacer les scores personnels.

## Architecture et cours

Le cours Android Cloud 2026–2027 est obligatoire pour les évolutions futures : lire [AGENTS.md](AGENTS.md) et la [matrice de conformité](docs/COURSE_COMPLIANCE.md). Supabase remplace le fournisseur Firebase sur instruction explicite de Julie, en conservant les principes du cours et son approche REST/Retrofit.

**AGENTS.md** conserve le périmètre, les autorisations et les décisions globales, puis [oriente vers les règles utiles selon la tâche](AGENTS.md#quelles-règles-lire). [CLAUDE.md](CLAUDE.md) contient seulement `@AGENTS.md`, selon le [mécanisme d'import de Claude Code](https://code.claude.com/docs/en/memory#import-additional-files), afin de partager les mêmes consignes. Les dix fichiers de [docs/rules](docs/rules) traitent architecture, Kotlin/build, ViewModels/état, Compose/navigation, coroutines/cycle de vie, Room, Supabase/synchronisation, jeu/performances, capacités Android et validation. Chaque règle précise son périmètre, les pages PDF du cours, ses prescriptions, les adaptations MarioSprint, des exemples et les vérifications attendues. La matrice de conformité conserve la correspondance avec les slides et l'historique des résultats.

| Dossier | Rôle |
|---|---|
| domain/game | Physique Kotlin pure à 120 Hz, neuf plans versionnés et bonus |
| domain/model, domain/repository | Modèles métier et interfaces injectables |
| game | Canvas, audio, entrées multipoints, bitmaps partagés |
| ui/navigation, ui/SprintApp.kt | Routes typées et branchement des ViewModels |
| ui/model, ui/mapping, ui/viewmodel | State/Action/Event, mapping et coordination |
| ui/screens, ui/components, ui/theme | Écrans stateless, previews et thème commun |
| data/local | Room, DAO, schémas, import historique et session chiffrée |
| data/remote, data/repository | DTO/Retrofit Supabase, comptes, cache et synchronisation |
| architecture | Application et injection manuelle |
| supabase | Migration SQL, droits et contrôles de configuration |

UpperCamelCase pour classes/composables, lowerCamelCase pour fonctions/propriétés, snake_case pour ressources. StateFlow en lecture seule, collecte avec le cycle de vie, SharedFlow sans replay pour les événements. E/S hors Main, préchargement graphique sur Default, rendu arrêté au repos/hors focus, HUD à 10 Hz. Les images, chemins et textures sont mis en cache ; aucun téléchargement d'assets pendant une course.

## Conservation des données

Le schéma Room passe de 1 à 2 par migration additive : seules les tables completed_levels et player_appearance sont créées. Les quatre tables existantes restent intactes. Aucun effacement ni migration destructive. L'import historique prairie-v1 reste atomique et idempotent. Les anciens scores prairie-1-v1 et leurs envois restent conservés, sans être transférés aux nouveaux parcours. L'ancien meilleur temps est affiché séparément sur l'accueil. Les trois identifiants prairie-1-v2, canyon-2-v1 et night-3-v1 sont conservés ; les six ajouts portent leurs propres identifiants dans LevelId. Toute modification des conditions de course impose un nouvel identifiant/version et une entrée SQL correspondante.

## Validation

Les tests JVM couvrent physique, commandes, bonus, parcours complets, chronométrage, missions, comptes, concurrence de synchronisation, frontières d'architecture et contrats HTTP Supabase sur serveur simulé. Les sources des tests Android sont maintenues et compilées, sans être exécutées sur le téléphone pour cette modification. La migration Room est aussi vérifiée sur SQLite PC avec les deux schémas exportés : `python tools/test_local_migration.py`. Les tests Postgres isolés couvrent les deux migrations, les records, l'accès aux personnages et les permissions. Aucune inspection visuelle ou mesure de performances sur appareil n'est effectuée. Les résultats et limites sont consignés dans docs/COURSE_COMPLIANCE.md. La connexion Google réelle et les écritures du projet distant devront être vérifiées par Julie.
