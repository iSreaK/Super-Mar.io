# Mario Sprint

Application Android Kotlin en portrait : **trois parcours originaux**, graphismes 2D illustrés, commandes à deux pouces, bonus, chronomètre et classements **Supabase** par niveau. Version 1.3.0 (code 4). Ouvrir ce répertoire dans Android Studio ou VS Code.

## Dépôt Git

Le dépôt [iSreaK/Super-Mar.io](https://github.com/iSreaK/Super-Mar.io) contient uniquement le projet MarioSprint, sur la branche `main`. Le dossier parent et les autres projets Android ne font pas partie du dépôt.

Le wrapper Gradle, les sources, les ressources, les tests, les schémas Room et les migrations SQL sont versionnés. Les caches, APK, logs, captures, configurations locales et clés de signature sont exclus. Les fichiers `local.properties` et `supabase.properties` doivent être renseignés sur chaque poste ; le modèle Supabase est fourni sans identifiants réels. La configuration du service Supabase reste à effectuer selon le guide ci-dessous.

## Jouer

Choisir Prairie des départs, Canyon des passerelles ou Nuit des étoiles. Chaque monde possède son propre décor, ses obstacles et ses records. Stick à gauche : inclinaison partielle pour marcher, complète pour courir. Grand bouton à droite : saut ; maintenir pour sauter plus haut. Les deux doigts sont indépendants. Clavier : Q/D ou flèches, Maj/X pour courir, Espace/Z pour sauter.

Les blocs ? donnent une pièce ou un champignon. Le champignon fait grandir et permet de casser les briques. Trois bonus supplémentaires sont disponibles dans chaque niveau : **étoile** (8 s contre les ennemis), **bouclier** (un choc absorbé), **aimant** (pièces proches pendant 10 s). Une chute reste mortelle. Les effets actifs sont affichés sous le chrono ; ils ne retirent jamais de temps au chronomètre.

Le chrono démarre au premier mouvement et suit le temps réel. Une pause, une perte de focus ou un délai entre images supérieur à 500 ms passe la tentative en entraînement. Recommencer repart sur un parcours neuf et classable. Durée maximale : 10 minutes. Les essais, victoires, pièces et meilleurs temps sont sauvegardés par niveau.

## Supabase et Google

Suivre le [guide complet pas à pas](docs/SUPABASE_SETUP.md). Le script SQL prêt à exécuter est dans [supabase/migrations](supabase/migrations/202610150001_levels_and_rankings.sql), avec des [contrôles de permissions en lecture seule](supabase/checks.sql).

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

| Dossier | Rôle |
|---|---|
| domain/game | Physique Kotlin pure à 120 Hz, trois plans versionnés et bonus |
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

Le schéma Room reste en version 1 : il possédait déjà les clés par niveau et UID. Aucun effacement ni migration destructive. L'import historique prairie-v1 est atomique et idempotent. Les anciens scores prairie-1-v1 et leurs envois restent conservés, sans être transférés aux nouveaux parcours. L'ancien meilleur temps est affiché séparément sur l'accueil. La prairie avec bonus porte désormais prairie-1-v2 ; le canyon canyon-2-v1 et la nuit night-3-v1. Toute modification des conditions de course impose un nouvel identifiant/version et une entrée SQL correspondante.

## Validation

Les tests JVM couvrent physique, commandes, bonus, parcours complets, chronométrage, comptes, concurrence de synchronisation, frontières d'architecture et contrats HTTP Supabase sur serveur simulé. Les sources des tests Android sont maintenues et compilées, sans être exécutées sur le téléphone pour cette modification. Aucune inspection visuelle ou mesure de performances sur appareil n'a été effectuée. Validation finale sur PC : **60 tests JVM et 18 tests SQL réussis, les deux APK compilés, lint sans erreur**. Les résultats et limites sont consignés dans docs/COURSE_COMPLIANCE.md. La connexion Google réelle et les permissions du projet distant devront être vérifiées après configuration par Julie.
