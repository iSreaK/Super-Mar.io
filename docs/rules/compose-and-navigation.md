# Compose, navigation et présentation

## Périmètre

À lire pour les Screens, composants, thème, navigation, Root, ressources d'interface et leurs tests. Compléter avec [ViewModels et état](viewmodels-and-state.md) si un contrat change, et avec [Coroutines](coroutines-and-lifecycle.md) si un effet ou une collecte change.

## Références du cours

- Pages PDF 70-100 : thème, UI déclarative, composants, modifiers, ressources et listes.
- Pages 116-129 : pile de navigation, routes sérialisables, callbacks et packages.
- Pages 230-235 : séparation Root/Screen, observation et Preview isolée.
- Pages 261-265 : désactivation pendant le chargement, listes et branchement des destinations.

## Règles

- Le Root obtient le ViewModel, collecte son State et traite les effets Android/navigation. Le Screen reçoit des valeurs et `onAction`, sans importer de ViewModel, repository ou source de données.
- L'écran décrit le State courant et émet des intentions ; une recomposition n'est pas une action utilisateur. Aucun appel réseau, accès disque ou effet de navigation dans le corps d'affichage.
- Collecter le State via `collectAsStateWithLifecycle`. Pour les Events, utiliser le branchement lifecycle décrit dans [Coroutines](coroutines-and-lifecycle.md).
- Déclarer les destinations et arguments typés dans `Destination` ; lire les arguments à la frontière du NavHost. Faire circuler des identifiants stables, pas des ViewModels, objets de base ou jetons dans les routes.
- Préserver la pile et le retour Android. Un Screen signale Retour ; le Root décide du `popBackStack` ou de l'action de jeu préalable.
- Mettre textes et descriptions d'accessibilité dans les ressources ; utiliser les formats et pluriels adaptés. Aucune description redondante pour une illustration décorative.
- Utiliser `MaterialTheme`/`SprintTheme` pour couleurs, typographie et formes. Les couleurs de pixels du moteur Canvas appartiennent au rendu, pas aux composants de menu.
- L'ordre des modifiers affecte taille, clic, bordure et placement. Vérifier les contraintes et les zones tactiles après une modification de cet ordre.
- Appliquer les insets/paddings du Scaffold une seule fois à chaque frontière prévue. Éviter les chevauchements des commandes avec les barres système ou des doubles marges à cause de Scaffolds imbriqués.
- Longues listes via `LazyColumn`/grille et clés stables : UID, mission, personnage ou autre identité réelle. Ne pas créer une grande liste complète dans une Column défilante.
- Réserver `remember` à la mémoire d'un objet/calcul de présentation, `rememberSaveable` à un petit état UI restaurable. Ni l'un ni l'autre ne sauvegarde durablement les records.
- Fournir une Preview autonome par écran, avec des états utiles : vide, chargé, erreur ou caractères agrandis. Ne pas démarrer de source réelle dans une Preview.

## Adaptations MarioSprint

- Charte marine/corail/menthe/or et interface portrait conservées ; typographie et formes centralisées dans `SprintTheme`.
- Accueil centré sur le choix du parcours, Jouer accessible pendant le défilement, compte/aide dans leurs écrans dédiés. Préserver cette hiérarchie en ajoutant une fonctionnalité.
- Menus de largeur bornée, contenu pouvant défiler sur petit écran, cibles tactiles d'au moins 48 dp et hauteur des boutons extensible avec le texte.
- Prévoir chargement, vide, erreur, action désactivée, hors connexion et réessai. Les états ne reposent pas sur la seule couleur ; une sauvegarde en attente ne doit pas paraître confirmée.
- Garder les chiffres du chrono lisibles et les emplacements de bonus stables pour que la scène/les commandes ne se déplacent pas pendant une course.
- Ne pas déplacer les règles de simulation dans Compose pour simplifier le rendu. L'intégration `AndroidView` reste assemblée par le Root et respecte la durée de vie de la vue.
- Une optimisation de recomposition doit répondre à un problème réel ; ne pas ajouter `@Stable`/`@Immutable` pour masquer un modèle dont le contenu est mutable.

## Exemples et pièges

- [SprintApp](../../app/src/main/kotlin/fr/julie/mariosprint/ui/SprintApp.kt) branche les destinations, adaptateurs et ViewModels ; [CollectionScreen](../../app/src/main/kotlin/fr/julie/mariosprint/ui/screens/CollectionScreen.kt) reçoit son State et ses Actions.
- [SprintComponents](../../app/src/main/kotlin/fr/julie/mariosprint/ui/components/SprintComponents.kt) et [SprintTheme](../../app/src/main/kotlin/fr/julie/mariosprint/ui/theme/SprintTheme.kt) sont les repères de cohérence visuelle.
- Un appel de source caché dans une fonction utilitaire invoquée par le Screen reste un accès aux données depuis l'UI.
- Une hauteur fixe qui convient au texte à 100 % peut couper le même bouton avec une taille système agrandie. Garder une hauteur minimale plutôt qu'un plafond arbitraire.
- Déclarer une Preview ne signifie pas que son rendu a été inspecté ; une compilation ne prouve pas l'affichage sur un téléphone.

## Vérifications

- Compiler les écrans et les tests Android concernés ; contrôler les ressources, la navigation et la séparation de couches.
- Préparer des Previews pour petite largeur/grands caractères et états d'erreur. Rapporter séparément tout rendu effectivement inspecté.
- Maintenir [AppSmokeTest](../../app/src/androidTest/kotlin/fr/julie/mariosprint/AppSmokeTest.kt) compilable. Son exécution, les captures et l'ouverture du jeu sur le téléphone restent soumises aux autorisations d'[AGENTS.md](../../AGENTS.md#téléphone).
- Choisir les contrôles selon [Tests et validation](testing-and-validation.md).
