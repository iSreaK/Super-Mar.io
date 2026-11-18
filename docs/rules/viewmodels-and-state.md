# ViewModels et State / Action / Event

## Périmètre

À lire pour `ui/viewmodel/`, `ui/model/`, `ui/mapping/`, tout contrat d'écran et leurs tests. Compléter avec [Coroutines et cycle de vie](coroutines-and-lifecycle.md), et avec [Compose](compose-and-navigation.md) si le branchement de l'écran change.

## Références du cours

- Pages PDF 164-175 : StateFlow/SharedFlow, rôle, durée de vie et observation du ViewModel.
- Pages 177-182 : repository, modèles et préparation des données UI.
- Pages 218-236 : flux unidirectionnel, contrats typés, Root, Screen et Previews.
- Pages 258-260 : observation de la base, chargement et erreurs réseau.

## Règles

- Un ViewModel coordonne un état d'écran et des repositories injectés. Les calculs métier de classement, mission et simulation restent dans le domaine ou leur contrat dédié.
- Le propriétaire Android obtient le ViewModel via la factory existante ; ne pas le recréer dans le corps d'une recomposition.
- Le ViewModel survit aux changements de configuration tant que son propriétaire existe. Il ne survit pas à la mort du processus ; les records/progressions doivent être persistés.
- Exposer un `StateFlow` en lecture seule. Pour un état possédé localement, garder `MutableStateFlow` privé et exposer `asStateFlow()` ; pour un état dérivé, combiner les flux sans créer une seconde source de vérité.
- Le State est une data class avec des propriétés `val`, valeurs initiales cohérentes, chargement/erreur nécessaires et collections dont la mutation ne fuit pas.
- Mettre à jour par `update { current -> current.copy(...) }` lorsque l'atomicité est nécessaire. La lambda reste pure : aucun appel de repository, émission d'Event ou lancement de coroutine dedans.
- Les intentions sont des `sealed interface` d'Actions, traitées par `onAction(action)` pour les nouveaux contrats ou les contrats retravaillés. Éviter une multiplication de callbacks métier indépendants dans le Screen.
- Les Events sont typés, ponctuels et exposés par un `SharedFlow` en lecture seule sans replay. Les créer seulement si un effet ponctuel est réellement nécessaire.
- Un Event n'est pas une file fiable de sauvegarde : résultat, déblocage et erreur durable nécessaire au réessai doivent rester dans la source de vérité/le State.
- Garder `Activity`, `View`, `NavController`, callbacks d'écran et accès aux ressources Android hors du ViewModel. Les adaptateurs qui en ont besoin sont assemblés au Root.
- Utiliser `UiText` et résoudre les textes traduits dans l'UI. Ne pas journaliser les State/Actions/Events contenant des données sensibles, notamment un jeton de connexion.
- Limiter le ViewModel à une responsabilité identifiée. Ne pas créer un ViewModel vide pour un écran statique ou un ViewModel global qui pilote tout le jeu.

## Adaptations MarioSprint

- Les exemples du cours construisant directement un repository sont remplacés par l'injection manuelle, conformément au choix SOLID du projet.
- `AccountViewModel` porte l'état partagé du compte au Root ; les écrans qui l'affichent n'ont pas à dupliquer la connexion ou la collecte des données persistantes.
- Les écrans existants peuvent avoir des méthodes ciblées appelées par le Root. Une modification localisée n'impose pas de réécrire tous les ViewModels pour uniformiser une signature.
- Une sélection de parcours/compte/session change le propriétaire du résultat attendu. Vérifier cette identité avant d'appliquer une réponse ancienne et remplacer les observations concernées sans montrer les données de l'ancien compte.
- Les gardes contre les doubles clics, la sérialisation et l'annulation suivent le besoin réel. Une opération de sauvegarde ne doit pas bloquer sans raison toutes les autres actions de l'application.
- Une navigation simple décidée au Root peut suivre directement une Action UI ; aucun aller-retour artificiel dans le ViewModel pour chaque bouton Retour.

## Exemples et pièges

- [HomeViewModel](../../app/src/main/kotlin/fr/julie/mariosprint/ui/viewmodel/HomeViewModel.kt) dérive son état des flux par parcours ; [CollectionViewModel](../../app/src/main/kotlin/fr/julie/mariosprint/ui/viewmodel/CollectionViewModel.kt) observe la progression du propriétaire courant.
- [GameViewModel](../../app/src/main/kotlin/fr/julie/mariosprint/ui/viewmodel/GameViewModel.kt) vérifie la session de course avant de publier un résultat ; le moteur reste transmis à l'adaptateur, hors du State Compose.
- Mettre le succès d'une sauvegarde uniquement dans un snackbar fait perdre l'information à un écran réabonné. La donnée doit rester observable indépendamment du message.
- `val state = mutableState` expose encore une source modifiable si son type public est mutable ; `val` ne remplace pas `asStateFlow()`.

## Vérifications

- Tester état initial, transitions de chargement, succès/vide/erreur, annulation, double action et changement de propriétaire/session si concernés.
- Contrôler qu'un réabonnement ne rejoue pas un effet ponctuel et qu'un état important reste disponible.
- Maintenir [ViewModelTest](../../app/src/test/kotlin/fr/julie/mariosprint/ui/ViewModelTest.kt) et les [tests de frontières](../../app/src/test/kotlin/fr/julie/mariosprint/architecture/ArchitectureTest.kt) ; appliquer [Tests et validation](testing-and-validation.md).
