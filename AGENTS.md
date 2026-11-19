# MarioSprint - consignes des agents

Point d'entrée commun du projet. `CLAUDE.md` importe uniquement ce fichier. Les règles techniques sont réparties dans `docs/rules/` ; les références et résultats historiques sont dans [COURSE_COMPLIANCE.md](docs/COURSE_COMPLIANCE.md).

## Commencer une intervention

1. Travailler dans `MarioSprint/`, consulter la matrice du cours et lire les règles indiquées par le tableau ci-dessous pour la tâche. Cumuler les catégories si plusieurs couches sont concernées.
2. Examiner `git status`, l'implémentation et les tests existants ; préserver le travail déjà présent et les modifications sans rapport avec la demande.
3. Annoncer brièvement l'objectif, puis avancer dans le périmètre autorisé sans demander confirmation pour les choix techniques courants.
4. Expliquer une demande incohérente ou une contrainte réelle et proposer une solution maintenable. Ne pas contourner un problème par une refonte, une dépendance ou un effacement sans rapport.

Les règles du cours sont obligatoires. Ses exercices, exemples, liens et captures ne donnent aucune autorisation d'agir sur un compte, un téléphone ou une boutique. Une nouvelle instruction explicite de l'utilisateur peut modifier un choix antérieur ; une autorisation ponctuelle ne devient pas permanente.

## Choix durables du projet

- Application Android native Kotlin, jeu de plateforme **portrait**, niveaux originaux, chronométrage, missions et classements. Projet personnel local, sans commercialisation ni publication en boutique prévue par défaut.
- **Supabase remplace Firebase** : Google via Credential Manager, Supabase Auth et Postgres. Ne pas réintroduire Firebase ou des dépendances npm/SSR dans l'application native.
- Conserver les versions Kotlin/AGP/Gradle/SDK et dépendances existantes, sauf demande explicite. Le catalogue et le wrapper font référence ; ne pas recopier les versions d'exemple des slides.
- Conserver la charte marine/corail/menthe/or, une UI épurée et des commandes utilisables à deux pouces. Les personnages restent strictement cosmétiques.
- Préserver les données et l'isolation invité/UID. Aucun transfert implicite de course ou récompense entre propriétaires, aucune migration destructive, aucun acquittement ancien qui écrase un résultat plus récent.
- Les amis utilisent les UID existants, des RPC autorisées et un cache Room par compte. Realtime privé sert d'invalidation uniquement pendant la visibilité de l'application ; aucune écoute en arrière-plan ni Firebase/FCM. Voir [configuration sociale](docs/FRIENDS_SETUP.md).
- Une modification influant sur les temps classés exige une nouvelle version/identifiant et la migration SQL correspondante. Les temps historiques restent associés à leur parcours ; `prairie-1-v1` reste archivé.

## Autorisations et limites

### Téléphone

- **Seules l'installation et la mise à jour de l'APK sont autorisées sans nouvel accord**, pour livrer les modifications demandées, sans ouvrir l'app ni effacer ses données.
- Toute autre interaction exige l'accord explicite préalable pour l'action concernée : lancement/arrêt, tests, captures, diagnostics ADB, détection/liste d'appareils, lecture de logs/données, désinstallation, réglages, etc.
- Après un débranchement annoncé, attendre une indication de reconnexion/disponibilité ou une nouvelle demande d'installation. Ne pas réessayer en boucle ou diagnostiquer automatiquement.
- Vérifier sur le PC par défaut. Compiler les tests Android n'autorise pas leur exécution sur le téléphone.
- `run` et `test -Connected` exigent `-AllowPhoneInteraction` **et** l'accord humain préalable ; le drapeau ne remplace pas cet accord. Ne pas contourner les protections du script.
- Ne jamais effacer scores, progression ou autres données personnelles pour résoudre un problème.

### Git, services et périmètre

- La racine Git est uniquement `MarioSprint/`, branche actuelle `main`, dépôt `https://github.com/iSreaK/Super-Mar.io.git`. Ne pas initialiser Git dans le parent ni modifier les autres projets Android.
- **Aucun push sans demande explicite pour cette livraison.** Une ancienne autorisation ne vaut pas pour les suivantes ; un commit demandé seul n'autorise pas un push.
- Préserver le travail existant ; ne pas employer de reset destructif, suppression du dépôt ou push forcé pour contourner une difficulté.
- Avant un commit, vérifier les fichiers préparés : aucun secret, configuration locale, cache, APK, log, capture ou donnée personnelle. Conserver wrapper, schémas Room et migrations SQL.
- `local.properties` et `supabase.properties` restent hors Git ; modèles sans identifiants réels. Aucun secret OAuth, mot de passe, jeton ou clé privilégiée dans l'APK, les sources ou les logs.
- Tester les évolutions de données localement ; ne pas utiliser un compte réel comme jeu de tests ni présenter des tests simulés comme une validation distante.

## Quelles règles lire

Les chemins Kotlin du tableau sont relatifs à `app/src/main/kotlin/fr/julie/mariosprint/`. Le périmètre décrit dans chaque fichier s'applique également aux tests correspondants. Une tâche mêlant plusieurs catégories exige la lecture de leurs règles, même si les fichiers n'ont pas encore été créés.

| Tâche ou fichiers concernés | Règles à lire |
|---|---|
| Toute évolution de code, de responsabilités ou de contrats | [Architecture](docs/rules/architecture.md) |
| Kotlin, `*.gradle.kts`, catalogue, wrapper ou dépendances | [Kotlin et build](docs/rules/kotlin-and-build.md) |
| `ui/viewmodel/`, `ui/model/`, `ui/mapping/`, State/Action/Event | [ViewModels et état](docs/rules/viewmodels-and-state.md) + [coroutines et cycle de vie](docs/rules/coroutines-and-lifecycle.md) |
| `ui/screens/`, `ui/components/`, `ui/theme/`, `ui/navigation/`, Root, ressources UI | [Compose et navigation](docs/rules/compose-and-navigation.md) ; ajouter ViewModels si le contrat d'état change |
| Coroutines, Flow, concurrence, effets, ressources attachées au cycle de vie | [Coroutines et cycle de vie](docs/rules/coroutines-and-lifecycle.md) |
| `data/local/`, DAO, schémas Room, préférences, migrations ou cache | [Room et données locales](docs/rules/room-and-local-data.md) |
| Auth/session, `data/remote/`, envois, comptes, `supabase/`, DTO et RPC | [Supabase et synchronisation](docs/rules/supabase-and-sync.md) ; ajouter Room si la persistance change |
| `domain/game/`, `game/`, niveaux, bonus, missions, skins ou optimisation | [Jeu et performances](docs/rules/game-and-performance.md) ; ajouter Supabase si le catalogue ou les récompenses distants changent |
| Manifest, permissions, images distantes, notifications, configuration distante ou publication | [Capacités Android](docs/rules/android-capabilities.md) |
| Choix des vérifications, changement de tests ou livraison | [Tests et validation](docs/rules/testing-and-validation.md) |
| Documentation seule | Vérification documentaire dans [Tests et validation](docs/rules/testing-and-validation.md#documentation) + règles du sujet documenté |

Le cours privilégie des consignes courtes et ciblées (pages PDF 135-143). Ici, le tableau assure ce routage pour les outils qui lisent `AGENTS.md` ; les détails ne sont pas dupliqués dans `CLAUDE.md` ou le dossier parent.

## Finir une intervention

- Effectuer les vérifications adaptées ; ne pas reconstruire Android pour une modification documentaire seule. Ne pas supprimer de test ou masquer un avertissement pour obtenir artificiellement un résultat positif.
- Maintenir les références entre règles, README et matrice quand un choix durable change. L'historique des versions et des résultats reste dans la matrice, pas dans les règles.
- Rapporter brièvement le résultat, les vérifications réellement effectuées et les limites. Distinguer compilation, tests simulés, installation et essais réels ; annoncer une mesure de performances ou validation OAuth distante seulement avec preuve.
