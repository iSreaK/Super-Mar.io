# Référentiel Android Cloud 2026–2027

Le cours de Jordane Serreau (UPJV/INSSET), fourni par l'utilisateur le 15 octobre 2026, constitue une exigence du projet. Les numéros ci-dessous sont les **pages du PDF** (la pagination imprimée diverge à partir du jour 1). Les exercices, liens de QCM, sondages et exemples de publication ne sont pas des demandes d'exécution. Aucune dépendance ne doit être ajoutée simplement pour cocher un chapitre.

Les règles d'intervention sont centralisées dans [AGENTS.md](../AGENTS.md), importé par [CLAUDE.md](../CLAUDE.md). Son [tableau d'orientation](../AGENTS.md#quelles-règles-lire) indique les fichiers de `docs/rules/` à lire selon les chemins et la nature de la tâche ; ce document conserve la correspondance avec le cours et les résultats historiques. Les règles distinguent prescriptions des slides et adaptations MarioSprint, notamment l'injection par constructeur, la conservation des migrations et Supabase. Emplacement connu du PDF source sur ce poste : `C:/Users/julie/Downloads/Android Cloud 2026-2027.pdf`.

| Exigence du cours | Pages PDF | Application obligatoire au projet | Règles détaillées |
|---|---|---|---|
| Ressources limitées, libération, permissions minimales | 9–10, 61–66 | E/S hors Main, arrêt audio/rendu hors premier plan, uniquement INTERNET, aucune télémétrie ajoutée | [Cycle de vie](rules/coroutines-and-lifecycle.md), [jeu](rules/game-and-performance.md), [capacités Android](rules/android-capabilities.md) |
| Wrapper, catalogue et Kotlin DSL | 20, 52–59 | Versions centralisées et conservées, scripts .gradle.kts | [Kotlin/build](rules/kotlin-and-build.md) |
| Kotlin, visibilité et immutabilité | 32–50 | val par défaut, nullabilité explicite, private/internal, noms explicites | [Kotlin/build](rules/kotlin-and-build.md) |
| Thème, ressources et listes efficaces | 70–100 | Thème Compose commun, textes/resources, formats/pluriels, LazyColumn avec clé UID | [Compose](rules/compose-and-navigation.md) |
| Navigation Compose, packages par rôle | 116–129 | Routes typées et NavHost, retour Android, écrans séparés | [Compose](rules/compose-and-navigation.md), [architecture](rules/architecture.md) |
| Règles pour les agents | 135–143 | Point d'entrée court, règles par sujet, compréhension des changements et aucune clé dans Git | [AGENTS.md](../AGENTS.md), [validation](rules/testing-and-validation.md) |
| Coroutines, Flow et cycle de vie | 158–175 | viewModelScope, annulation propagée, StateFlow privé/mutable et public/lecture seule, collecte lifecycle | [Coroutines](rules/coroutines-and-lifecycle.md), [ViewModels](rules/viewmodels-and-state.md) |
| Repository, modèles par couche, SOLID/KISS/DRY | 176–187 | Interfaces et injection manuelle, domain Kotlin pur, mapping UI/données/entités, aucune source dans le ViewModel | [Architecture](rules/architecture.md) |
| Room, DAO, instance unique, mapping | 188–216 | Base créée au niveau Application ; transactions, Flow, schémas exportés ; import sans perte des préférences v1 | [Room](rules/room-and-local-data.md) |
| State · Action · Event et previews | 218–236 | State immuable par écran, onAction typé, Event ponctuel, Root collecte et Screen stateless, Preview sans base/Supabase | [ViewModels](rules/viewmodels-and-state.md), [Compose](rules/compose-and-navigation.md) |
| Source locale de vérité pour le cloud | 252–260 | Classement téléchargé dans Room puis observé, cache borné et lié au compte, erreurs et réessai explicites | [Room](rules/room-and-local-data.md), [synchronisation](rules/supabase-and-sync.md) |
| Fournisseur cloud du cours | 274–290 | Supabase remplace Firebase sur instruction explicite de Julie ; Google Auth, RLS, DTO et repositories restent obligatoires. Aucun service Firebase conservé | [Supabase](rules/supabase-and-sync.md) |
| Notifications et Remote Config | 291–329 | Applicables si demandés : permission Android 13 au moment utile, service déclaré, valeurs locales par défaut et intervalle de fetch adapté. Pas d'ajout dans la version actuelle | [Capacités Android](rules/android-capabilities.md) |
| Publication, API REST et images URL | 110–115, 144–153, 237–268 | Retrofit et DTO Supabase pour Auth et RPC REST, HTTPS, erreurs et délais explicites. Coil seulement si des images URL apparaissent. Aucune publication demandée | [Supabase](rules/supabase-and-sync.md), [capacités Android](rules/android-capabilities.md) |

## Choix adaptés au jeu

La simulation, les bonus et les plans des neuf niveaux appartiennent au domain Kotlin pur. La surface Canvas Android est un adaptateur de rendu dans game/ ; ce n'est pas un écran de navigation. Elle transmet les changements vers le contrat de jeu et reçoit la session ; elle n'accède ni à Room ni à Supabase. Le calcul de physique par image est limité et déterministe (120 Hz) ; les bitmaps sont mis en cache et le HUD est échantillonné à 10 Hz. Aucun StateFlow de milliers de sprites à chaque image.

Les paramètres du niveau classé sont versionnés. Une pause/interruption rend la course non classable. Les classements SQL sont limités à 50 entrées ; le cache et les envois sont bornés à 12 secondes, les appels Auth à 15 secondes. Un envoi en attente appartient au couple niveau/UID et un acquittement ne peut supprimer un meilleur temps arrivé entre-temps. Aucun envoi des courses invitées après connexion.

Les versions préexistantes (AGP 9.4.0, Kotlin 2.2.10, Gradle 9.6.0, SDK 36) restent fixées sur demande de l'utilisateur. Les versions d'exemple du cours ne constituent pas une obligation de mise à niveau. Ne pas recopier le fallbackToDestructiveMigration de la démonstration : les records doivent être conservés.

## Vérifications à chaque évolution

Compiler et exécuter sur le PC les vérifications adaptées : physique, bonus, comptes, concurrence, requêtes et états. **Depuis la demande de Julie du 15 octobre 2026, seule l'installation/mise à jour est autorisée sur le téléphone sans nouvel accord.** Toute autre action exige son consentement préalable. Les tests Android sont maintenus et compilés ; leur exécution n'est pas autorisée pour cette évolution. Ne pas effectuer de captures ni de contrôle visuel sur téléphone pour cette demande.

## Évolution 1.3.0 — 15 octobre 2026

Trois parcours distincts et versionnés, avec étoiles, boucliers et aimants. Les effets sont simulés à pas fixe et affichés dans un emplacement réservé du HUD pour ne pas déplacer les commandes pendant une course. Les flèches utilisent des vecteurs/chemins géométriques centrés, les cibles d'icônes font 48 dp, la palette Material est explicite. Aucun changement de la chaîne Kotlin/AGP/Gradle/SDK.

Supabase remplace entièrement les SDK Firebase : Auth Google par Credential Manager et nonce, requêtes REST Retrofit, session chiffrée AES/GCM + Android Keystore, renouvellement à la demande et déconnexion locale. Les secrets OAuth restent dans le panneau fournisseur Supabase ; seules trois valeurs publiques configurent Android. Les tables ont la RLS activée, aucun droit direct pour anon/authenticated, deux fonctions sécurisées et bornées ; l'UID est toujours dérivé de la session. La validation SQL protège les droits et valeurs, sans prétendre valider un parcours côté serveur.

Room conserve son schéma 1 déjà indexé par niveau/UID. L'ancien parcours prairie-1-v1 est archivé sans effacement ni réattribution ; la nouvelle prairie avec bonus est prairie-1-v2. Les routes, stats et caches sont associés au niveau sélectionné. Les couches, State/Action/Event et règles de nommage de l'audit 1.2 sont conservés.

### Validation de cette version

18 tests SQL réussis avec Postgres en mémoire (PGlite 0.5.10) et schéma Auth simulé : migration rejouable, RLS, permissions, Google/UID, bornes, meilleur temps, niveaux, Top 50 et suppression de compte. **60 tests JVM réussis**, dont 24 tests moteur (avec les trois parcours complets), 7 bonus, 11 ViewModels, 13 contrats Supabase/session, 2 synchronisations et 3 frontières d'architecture. Compilation APK application et tests Android réussie, lint debug sans erreur (build final : 6 min 35 s). Les avertissements restants concernent les versions conservées, le portrait demandé et des suggestions KTX ; aucune suppression de ces avertissements. Les ressources inutilisées et le pluriel des pièces ont été corrigés.

La suppression d'une session invalide efface le stockage chiffré avant de notifier la déconnexion, afin qu'un observateur qui annule sa requête ne laisse pas un jeton enregistré. Ce cas possède un test dédié. La mémoire de métadonnées de Gradle passe de 512 à 640 Mo pour éviter la pression observée pendant le lint ; heap 1 Go et un worker conservés. Les protections des commandes run et test -Connected ont été vérifiées sur le PC : elles refusent ces actions sans accord et drapeau explicites, avant tout accès au téléphone. Mise à jour de l’APK 1.3.0 confirmée par ADB install -r (Success), sans ouverture de l’application. Aucune autre commande sur le téléphone, aucun test ni contrôle visuel. Supabase réel non configuré ; les tests HTTP locaux ne valident pas les permissions d'un serveur distant. Le guide docs/SUPABASE_SETUP.md explique comment les configurer et les vérifier.

### Historique distinct

La version 1.2 avait obtenu 38 tests JVM et 17 tests instrumentés sur Redmi, avec les autorisations alors en vigueur. Ces résultats historiques ne sont pas présentés comme une validation sur téléphone de la version 1.3. Les vérifications Firebase antérieures deviennent obsolètes depuis le changement de fournisseur.

## Configuration Supabase — 18 novembre 2026

Les trois identifiants publics sont renseignés dans supabase.properties, toujours exclu de Git. Le BuildConfig généré a été vérifié : Supabase activé et ID client Web identique à la valeur fournie par Julie, tiret inclus. Aucun secret OAuth n'est ajouté au projet. Le guide de configuration précise désormais l'exception Google pour les seuls scopes d'identité : l'ajout dans Test users n'est pas obligatoire pour ce flux.

Vérifications en lecture seule depuis le PC sur le projet distant : Auth settings répond HTTP 200 avec Google activé ; l'appel anonyme de get_leaderboard répond HTTP 401, code Postgres 42501, permission refusée pour cette fonction. Ces contrôles ne valident ni une connexion Google réelle, ni l'ensemble des droits ou des écritures authentifiées. Aucun compte ni record n'a été créé ou modifié.

Compilation de l'APK debug, **60 tests JVM réussis sans échec ni test ignoré** et **lint debug sans erreur** (build : 6 min 26 s). Les avertissements des versions conservées, du portrait et des suggestions KTX restent présents. Aucun changement de code métier, de schéma Room, de migration SQL ou de dépendance ; les tests SQL et les tests instrumentés n'ont pas été exécutés pour cette configuration.

La première installation de mise à jour a échoué car ADB ne trouvait pas l'appareil. Après les accords spécifiques de Julie pour lister les appareils et redémarrer le serveur ADB du PC, la connexion USB a finalement été rétablie et la mise à jour 1.3 a été installée avec install -r (Success). Aucune ouverture du jeu, capture, lecture de ses données ou exécution de test sur le téléphone. L'essai connecté est laissé à Julie.

## Évolution 1.4.0 — 18 novembre 2026

Neuf parcours fixes, avec trois univers et trois difficultés. Les six nouveaux parcours possèdent leurs propres identifiants et versions ; les géométries et identifiants des trois parcours publiés restent inchangés. Le catalogue Kotlin et les versions SQL sont comparés par un test. Les skins Luigi, Toad, Yoshi et Peach ne changent aucune règle de simulation : seules les images du rendu sont remplacées, préchargées sur Default et conservées en mémoire.

Les missions et leurs conditions appartiennent au domain Kotlin pur. L'écran de collection reçoit un State immuable et des Actions typées, avec sa Preview ; son Root assemble le ViewModel et la navigation typée. Le personnage est transmis au rendu sans accès aux sources depuis un composable. La synchronisation est nommée PlayerSyncRepository car elle traite désormais les records et la progression du compte. Les sources REST partagent le renouvellement de jeton borné à une relance sur 401.

Room passe en version 2 avec une migration additive qui crée uniquement completed_levels et player_appearance. Les schémas 1 et 2 sont conservés. La validation d'un niveau pour les missions se fait dans la même transaction que la sauvegarde de sa course ; elle exige une arrivée classable entre 10 secondes et 10 minutes. Les profils invités et les UID utilisent des espaces de clés séparés. Aucun déblocage n'est réattribué après une connexion ou un changement de compte en cours de partie. Une révision protège un nouveau choix local contre un acquittement réseau antérieur.

La nouvelle migration Supabase 202611180001 ajoute les six parcours, les catégories, player_appearance et deux RPC sous SECURITY DEFINER avec search_path vide. Les missions en ligne sont déduites des best_runs du compte courant ; aucun UID, compteur ou déblocage envoyé par le client n'est accepté. Les accès directs à la table d'apparence sont interdits et la RLS est activée. Les deux migrations sont rejouées en tests sans perte de records ou de personnage. La limite anti-triche du chrono client reste applicable aux missions fondées sur ces records.

**31 tests Postgres locaux réussis** avec Auth simulé. **4 tests SQLite PC réussis** : conservation des quatre tables historiques, correspondance des colonnes/types/clés avec le schéma Room 2 exporté, isolation des propriétaires et conservation de la progression lors d'une répétition du DDL. Cela ne remplace pas une exécution de la migration avec Room sur appareil. Julie a confirmé l'application de la migration Supabase ; les deux nouvelles RPC refusent effectivement les appels anonymes depuis le PC (HTTP 401, code 42501). Aucun compte, record ou personnage réel n'a été créé ou modifié par ces vérifications.

Validation finale : **83 tests JVM réussis, aucun échec ni test ignoré**, dont les neuf parcours terminés avec les commandes normales, les missions, les changements de compte et les choix de skin concurrents. APK application et tests Android compilés ; lint sans erreur. Build complet : 6 min 29 s ; dernier assemblage après correction d'un libellé : 3 min 15 s, tests inchangés réutilisés par Gradle. Les avertissements restants concernent les versions conservées, l'orientation portrait, les suggestions KTX et un candidat de pluriel pour le compteur « 1 / 4 personnages » ; aucune suppression de ces avertissements.

Version 1.4.0 (code 5) installée sur le Redmi par la seule commande install -r, réponse Success. Aucun test instrumenté, lancement, capture, diagnostic ou lecture de données sur le téléphone pour cette évolution. L'affichage et les parcours réels avec un joueur, la migration Room sur appareil et les écritures Google/Supabase restent à vérifier manuellement par Julie. Aucune mesure de performances sur téléphone n'est revendiquée.

## Évolution 1.5.0 — interface — 18 novembre 2026

L'accueil présente le choix du parcours, son aperçu et le record local. Jouer et le classement restent accessibles pendant le défilement. Le compte Google, la synchronisation et les préférences sont regroupés dans un écran dédié ; l'aide aux commandes, bonus et records possède également sa route typée. Ces écrans émettent des Actions et n'accèdent à aucune source de données. Le profil réutilise le State et le ViewModel existants ; l'aide statique n'ajoute aucun ViewModel ou repository.

La palette marine/corail/menthe/or est conservée. Typographie et formes sont centralisées dans SprintTheme ; composants communs pour les titres, boutons, retours et messages. Les menus sont limités à 600 dp, les boutons ont une hauteur minimale extensible et les zones interactives font au moins 48 dp. Les listes peuvent défiler, y compris le classement entier et les panneaux de pause/résultat. Le vestiaire passe à une colonne sur petite largeur ou avec de grands caractères. Le chrono garde ses neuf chiffres sur une ligne en adaptant sa taille à la largeur disponible. Des Previews à 320–400 dp et avec un facteur de texte de 1,5 sont déclarées dans les sources ; cela ne constitue pas une vérification visuelle exécutée.

Les commandes Canvas utilisent les mêmes zones et le même stick analogique, avec un dessin plat, des libellés agrandis et un centrage par métriques de police conservées en mémoire. Les trois emplacements de bonus sont toujours présents pour éviter les déplacements du HUD. Les touches de la surface de jeu sont ignorées pendant une pause ou après la fin de la tentative. La physique, les neuf plans, les identifiants classés, les missions, Room et les contrats Supabase restent inchangés. Aucune nouvelle dépendance ni migration.

Vérifications PC : **83 tests JVM réussis sans échec ni test ignoré**, compilation des APK application et tests Android, lint sans erreur. Les 25 avertissements restants concernent les versions conservées, le portrait et les suggestions KTX/API déjà présentes ; aucune suppression ajoutée. Cinq couples de couleurs de base ont été contrôlés par calcul, avec un contraste minimal de 6,86:1 ; il ne s'agit pas d'un audit visuel complet. Les sources du test de navigation ont été adaptées aux actions fixes et aux routes compte/aide, sans exécution instrumentée. Les tests SQL/SQLite ne sont pas relancés pour cette modification d'interface sans changement de données.

La préparation de la version 1.5.0 (code 6) a été effectuée uniquement sur le PC, le téléphone ayant été annoncé débranché. Julie a ensuite autorisé explicitement le commit, le push et la mise à jour sur son téléphone pour cette livraison. L'APK a été installé sur le Redmi par la seule commande install -r, réponse Success. Aucun lancement, test instrumenté, diagnostic, capture, lecture de données ou mesure de performances sur le téléphone. L'essai visuel et les interactions réelles restent à effectuer par Julie.

## Fiabilité de la déconnexion — 18 novembre 2026

Le nettoyage de la session Google après déconnexion ne dépend plus d'un Event sans replay. La demande reste dans `ProfileState.googleSessionClearPending`, observée par le Root pendant STARTED, jusqu'à son acquittement. Si le cycle de vie annule le nettoyage, la demande reste disponible au retour au premier plan. Une confirmation reçue sans nettoyage en attente ne peut plus débloquer une connexion en cours. Aucun client Android ni Activity n'est introduit dans le ViewModel.

Le nettoyage des préférences Google est borné à 15 secondes ; la session Supabase est déjà supprimée localement avant cette étape. Un échec ou une annulation de la déconnexion libère correctement l'état occupé s'il n'y a aucun nettoyage à terminer. Aucun changement des comptes, records, schémas Room, migrations SQL, versions ou dépendances.

Quatre nouveaux tests JVM couvrent la déconnexion terminée sans collecteur UI, la reprise et l'acquittement sans répétition, l'échec avec réessai, l'annulation et la confirmation hors contexte. Le contrôle existant conserve l'interdiction de se reconnecter pendant le nettoyage. Les reprises sont simulées par arrêt/réabonnement au State, sans abonné au flux d'Events ; elles ne constituent pas un essai du sélecteur Google sur Android.

Vérifications PC : **87 tests JVM réussis, aucun échec ni test ignoré**, dont 18 tests de ViewModels. Compilation des APK application et tests Android réussie, lint sans erreur et avec les mêmes 25 avertissements connus ; build complet en 7 min 3 s. Les tests SQL/SQLite ne sont pas relancés pour cette correction des contrats UI et de leur cycle de vie, sans changement de persistance ou de serveur.

Aucune installation, interaction avec le téléphone ou écriture sur un compte réel pour cette correction. Le parcours Google/Supabase réel, le rendu/accessibilité et les performances sur appareil restent à valider avec une autorisation spécifique. Ces résultats ne constituent pas une certification exhaustive de toutes les règles.

## Corrections 1.5.1 — 18 novembre 2026

Quatre défauts relevés à la lecture du code sont corrigés, sans changement de physique, de parcours, de schéma Room ni de migration SQL. Aucune dépendance ajoutée. Un cinquième, la déconnexion bloquée en arrière-plan, avait reçu ici une correction équivalente ; celle du binôme, décrite dans la section précédente, a été conservée à sa place.

- **Synchronisation** : un temps refusé définitivement par `submit_run` (HTTP 400, « Invalid run ») devient une `RunRejectedException` du domain. Il reste enregistré, mais la synchronisation passe aux autres parcours puis aux missions, et signale le refus à la fin. Une panne réseau ou un 5xx interrompt toujours l'envoi sans boucle de réessai.
- **Progression distante** : la réponse Supabase utilise la même règle que le cache Room. Un parcours ou un personnage inconnu de cette version est ignoré au lieu de faire échouer toute la lecture ; il ne débloque rien. La borne de 50 parcours est conservée.
- **Vestiaire** : seul le refus d'équiper affiche « personnage non débloqué » ; une erreur de synchronisation affiche le message d'attente.
- **Manifeste** : `android:appCategory="game"` déclare l'app comme jeu, pour conserver le portrait sur grand écran avec le SDK cible 36.

La précision du chrono reste d'une image (16,7 ms à 60 Hz) : c'est la conséquence de la mesure en temps réel demandée, non modifiée.

Vérifications sur un second PC (JDK 25, chemin de profil non ASCII, options `android.overridePathCheck` et `file.encoding=COMPAT` passées en ligne de commande) : **93 tests JVM réussis sans échec ni test ignoré**, soit les 87 de la section précédente et 6 nouveaux tests de régression ; APK debug et sources des tests Android compilés ; lint sans erreur, 26 avertissements. Le nouvel avertissement signale que `appCategory` est ignoré avant l'API 26. Avant ces corrections, les 31 tests Postgres et les 4 tests SQLite ont été rejoués avec succès ; ils ne couvrent aucun fichier modifié ici.

Non vérifié : le comportement du portrait sur tablette ou pliable, la réponse 400 d'un serveur Supabase réel, les tests instrumentés et tout essai sur téléphone. Aucun commit ni push n'a été fait pour cette évolution.

## Écart avec le sujet « Projet en binôme — Jour 3 »

Le sujet remis le 18 novembre 2026 impose des éléments absents de cette version : accueil avec les noms du binôme et un texte piloté par Remote Config, liste alimentée par une API publique (Retrofit, Room, Coil) avec en-têtes, tri ou filtre, suppression et écran de détail, use cases dans le domain, un fichier `…Contract.kt` par écran, notification FCM avec donnée personnalisée, `CLAUDE.md` contenant `@AGENTS.md` et skill android-cli. Remote Config et FCM sont des services Firebase, que la règle actuelle du projet interdit. La décision de lever cette règle et le périmètre à réaliser restent à prendre par le binôme.
