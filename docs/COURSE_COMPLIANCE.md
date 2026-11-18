# Référentiel Android Cloud 2026–2027

Le cours de Jordane Serreau (UPJV/INSSET), fourni par l'utilisateur le 15 octobre 2026, constitue une exigence du projet. Les numéros ci-dessous sont les **pages du PDF** (la pagination imprimée diverge à partir du jour 1). Les exercices, liens de QCM, sondages et exemples de publication ne sont pas des demandes d'exécution. Aucune dépendance ne doit être ajoutée simplement pour cocher un chapitre.

| Exigence du cours | Pages PDF | Application obligatoire au projet |
|---|---|---|
| Ressources limitées, libération, permissions minimales | 9–10, 61–66 | E/S hors Main, arrêt audio/rendu hors premier plan, uniquement INTERNET, aucune télémétrie ajoutée |
| Wrapper, catalogue et Kotlin DSL | 20, 52–59 | Versions centralisées et conservées, scripts .gradle.kts |
| Kotlin, visibilité et immutabilité | 32–50 | val par défaut, nullabilité explicite, private/internal, noms explicites |
| Thème, ressources et listes efficaces | 70–100 | Thème Compose commun, textes/resources, formats/pluriels, LazyColumn avec clé UID |
| Navigation Compose, packages par rôle | 116–129 | Routes typées et NavHost, retour Android, écrans séparés |
| Règles pour les agents | 135–143 | AGENTS.md durable ; compréhension des changements et aucune clé dans Git |
| Coroutines, Flow et cycle de vie | 158–175 | viewModelScope, annulation propagée, StateFlow privé/mutable et public/lecture seule, collecte lifecycle |
| Repository, modèles par couche, SOLID/KISS/DRY | 176–187 | Interfaces et injection manuelle, domain Kotlin pur, mapping UI/données/entités, aucune source dans le ViewModel |
| Room, DAO, instance unique, mapping | 188–216 | Base créée au niveau Application ; transactions, Flow, schémas exportés ; import sans perte des préférences v1 |
| State · Action · Event et previews | 218–236 | State immuable par écran, onAction typé, Event ponctuel, Root collecte et Screen stateless, Preview sans base/Supabase |
| Source locale de vérité pour le cloud | 252–260 | Classement téléchargé dans Room puis observé, cache borné et lié au compte, erreurs et réessai explicites |
| Fournisseur cloud du cours | 274–290 | Supabase remplace Firebase sur instruction explicite de Julie ; Google Auth, RLS, DTO et repositories restent obligatoires. Aucun service Firebase conservé |
| Notifications et Remote Config | 291–329 | Applicables si demandés : permission Android 13 au moment utile, service déclaré, valeurs locales par défaut et intervalle de fetch adapté. Pas d'ajout dans la version actuelle |
| Publication, API REST et images URL | 110–115, 144–153, 237–268 | Retrofit et DTO Supabase pour Auth et RPC REST, HTTPS, erreurs et délais explicites. Coil seulement si des images URL apparaissent. Aucune publication demandée |

## Choix adaptés au jeu

La simulation, les bonus et les plans des trois niveaux appartiennent au domain Kotlin pur. La surface Canvas Android est un adaptateur de rendu dans game/ ; ce n'est pas un écran de navigation. Elle transmet les changements vers le contrat de jeu et reçoit la session ; elle n'accède ni à Room ni à Supabase. Le calcul de physique par image est limité et déterministe (120 Hz) ; les bitmaps sont mis en cache et le HUD est échantillonné à 10 Hz. Aucun StateFlow de milliers de sprites à chaque image.

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
