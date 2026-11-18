# Coroutines, Flow et cycle de vie

## Périmètre

À lire pour toute coroutine, collecte de Flow, opération concurrente, effet Compose, horloge, délai ou ressource attachée à une Activity/View. S'applique également aux tests d'annulation et aux adaptateurs de jeu.

## Références du cours

- Pages PDF 9-10 et 65-66 : ressources limitées, visibilité et destruction du processus sans `onDestroy` garanti.
- Pages 158-164 : Main, suspension, dispatchers, flux froids/chauds et StateFlow/SharedFlow.
- Pages 167-173 : durée de vie du ViewModel, scope et collecte de State.
- Pages 223-230 et 258-260 : Events, effet de composition et gestion d'erreurs.

## Règles

- Garder un propriétaire identifié pour chaque travail : `viewModelScope` pour la coordination d'écran, effets lifecycle/composition pour les ressources UI. Aucun `GlobalScope`, `runBlocking` ou scope détaché en production.
- Ne jamais bloquer Main avec disque, réseau synchrone ou gros calcul. Une fonction `suspend` ne déplace pas à elle seule un travail bloquant vers un autre thread.
- Dans la couche qui les réalise, déporter les E/S bloquantes vers IO et les calculs lourds vers Default. Room/Retrofit suspend gèrent déjà leurs appels ; ne pas ajouter un dispatcher par automatisme autour de chaque appel suspend.
- Le State UI est collecté avec `collectAsStateWithLifecycle`. Les Events sont collectés dans un effet avec `repeatOnLifecycle(STARTED)`, lié au ViewModel/propriétaire concerné.
- Une coroutine lancée dans `viewModelScope` est annulée lorsque le ViewModel est détruit. Un écran couvert dans la pile peut conserver son ViewModel ; ne pas assimiler tout `onStop` à `onCleared`.
- Propager `CancellationException`. Traiter un `TimeoutCancellationException` attendu avant la relance des autres annulations si ce délai doit produire un état d'erreur récupérable.
- Libérer les ressources avec `finally`, `DisposableEffect` ou le callback de détachement approprié ; ne pas attendre uniquement `onDestroy` pour persister une donnée importante ou arrêter une ressource visible.
- Capturer les erreurs à une frontière utile, avec état/message de reprise. Aucun succès fictif, `catch` silencieux non justifié ou fuite de données sensibles dans les erreurs.
- Les mises à jour de State restent atomiques et sans effets de bord dans leur lambda. Éviter qu'une opération longue réécrive un State construit à partir d'une ancienne valeur.
- Maîtriser la concurrence : annulation/remplacement pour une sélection, garde pour un double clic, mutex pour une opération atomique selon le besoin ; pas de verrou global sans raison.

## Adaptations MarioSprint

- `stateIn` avec `SharingStarted.WhileSubscribed(5_000)` convient aux états dérivés observés par l'UI quand leur responsabilité le permet. La durée de partage se choisit consciemment, pas par copier/coller.
- `flatMapLatest` remplace les observations lors d'un changement de compte/parcours. Vérifier en plus l'UID ou la session avant d'appliquer une opération déjà engagée qui peut terminer malgré le changement.
- La collecte des Events avec le cycle de vie renforce l'exemple `LaunchedEffect` simplifié des slides. Un SharedFlow sans replay peut perdre un message sans abonné ; la donnée importante est conservée ailleurs.
- Réserver `NonCancellable` aux courtes écritures locales atomiques déjà justifiées pour conserver une course. Aucun réseau, polling ou observation sans borne dans ce contexte.
- Délais réseau bornés, réessai manuel et un seul renouvellement/réessai sur 401 selon le contrat de [synchronisation](supabase-and-sync.md). Ne pas ajouter de boucle de fond pour cacher une erreur.
- Précharger les dessins sur Default et arrêter callbacks de rendu/audio/écoutes hors focus/visibilité. Conserver l'ordonnancement borné du moteur plutôt que déplacer sa boucle arbitrairement sur des threads.
- Injecter dispatcher/horloge lorsqu'un besoin de responsabilité ou de test le justifie, sans mécanisme global inutilisé.

## Exemples et pièges

- [AccountViewModel](../../app/src/main/kotlin/fr/julie/mariosprint/ui/viewmodel/AccountViewModel.kt) combine l'identité et la progression ; [SprintApp](../../app/src/main/kotlin/fr/julie/mariosprint/ui/SprintApp.kt) effectue les collectes lifecycle.
- [GameView](../../app/src/main/kotlin/fr/julie/mariosprint/game/GameView.kt) attache/détache les callbacks et ressources Android du jeu.
- `try { operation() } catch (error: Exception) { afficherErreur() }` peut avaler une annulation : celle-ci doit être relancée avant de convertir les autres erreurs.
- Ne pas lancer un job à chaque recomposition, ni recommencer toutes les observations à chaque valeur du HUD.

## Vérifications

- Tester annulation, délai attendu, double appel, changement de propriétaire/session et réponse tardive lorsque ces cas concernent la modification.
- Contrôler l'arrêt des observations/ressources à la frontière prévue et l'absence de travail bloquant en production.
- Utiliser les tests de coroutines contrôlés du projet ; suivre [Tests et validation](testing-and-validation.md), sans attendre réellement plusieurs secondes quand une horloge de test convient.
