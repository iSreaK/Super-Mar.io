# Jeu, parcours et performances

## Périmètre

À lire pour `domain/game/`, le modèle des missions/personnages, `game/`, les commandes, niveaux, bonus, chronométrage et optimisations. Ajouter [Supabase](supabase-and-sync.md) si catalogue/récompenses distants changent, [Room](room-and-local-data.md) si la progression persistée change et [Compose](compose-and-navigation.md) pour le HUD.

## Références du cours

- Pages PDF 9-10 : mémoire, batterie, processeur, réseau et libération des ressources.
- Pages 65-66 : visibilité, interruption et destruction du processus.
- Pages 158-161 et 184-187 : travail long hors Main, simplicité et métier indépendant.

Le cours n'impose pas de moteur de plateforme ou de fréquence de simulation. Les valeurs et règles de jeu ci-dessous sont les invariants de MarioSprint, construits en appliquant ces principes.

## Règles

- Séparer simulation, adaptation Android et dessin. Aucune requête Room/Supabase dans le moteur, le rendu ou les commandes tactiles.
- Une modification de présentation ne change pas implicitement la physique, le chrono, l'identité du parcours ou les conditions de récompense.
- Préparer les ressources coûteuses hors Main puis réutiliser bitmaps, Paint, Path et métriques. Aucune génération de personnage, lecture disque, requête ou décodage d'image à chaque frame.
- Respecter la propriété des ressources : libérer ce que l'adaptateur possède, sans recycler un bitmap partagé encore utilisé par un autre écran.
- Arrêter callbacks, audio et écoutes quand la vue n'est plus visible/attachée ou perd le focus selon le contrat de jeu. Ne pas garder une boucle active derrière un menu.
- Limiter mémoire, appels réseau et allocations avant d'ajouter un mécanisme complexe. Justifier une optimisation par le travail évité ou une mesure, sans annoncer de gain chiffré sans preuve.
- Tester un parcours avec les commandes normales ; un scénario qui téléporte le joueur ou supprime les ennemis ne démontre pas qu'il est terminable.

## Adaptations MarioSprint

### Simulation et commandes

- Portrait, stick analogique à gauche et saut indépendant à droite. Conserver deux pointeurs pour courir et sauter simultanément ; la course suit l'amplitude du stick.
- Garder le pas fixe déterministe de 120 Hz et les bornes de rattrapage existantes. Le chrono suit le temps réel, sans soustraire l'effet d'un bonus.
- Une pause, perte de focus ou interruption importante invalide la tentative pour le classement. La reprise se fait en entraînement ; recommencer crée une nouvelle session classable.
- Le HUD reçoit au maximum 10 mises à jour par seconde hors transition d'état. Ne pas exposer une collection de sprites via StateFlow à chaque image.
- Le moteur mutable est transmis par le Root à l'adaptateur Canvas, hors du State Compose. Ne pas généraliser cette liaison à des objets partagés entre écrans.
- Les emplacements de bonus restent présents ; leur apparition ne déplace pas la scène ou les commandes. Les entrées sous un panneau de pause/résultat ne doivent pas relancer la course.

### Parcours, missions et personnages

- Les parcours sont fixes, identifiés et versionnés. Toute géométrie, bonus ou règle qui influe sur le temps classé exige une nouvelle version/identifiant, une entrée SQL et les tests de cohérence correspondants.
- Conserver les records liés aux anciens identifiants, notamment `prairie-1-v1` archivé. Ne pas réattribuer un ancien temps à un nouveau parcours ou propriétaire.
- Les règles de mission sont dans le domaine. Actuellement, une arrivée sans interruption compte entre 10 000 et 600 000 ms ; une répétition du même parcours ne doit pas gonfler artificiellement une mission de parcours distincts.
- Luigi est disponible ; Toad se débloque au premier parcours validé, Yoshi après au moins un parcours dans chacun des trois univers, Peach après les trois parcours Expert.
- Les skins changent uniquement les dessins/animations : même vitesse, même saut, même hitbox et mêmes collisions pour tous.
- La validation locale d'une mission est transactionnelle avec la course ; la progression invitée reste séparée de chaque UID et les déblocages en ligne sont déduits des records du compte.
- À ce jour, neuf parcours couvrent trois univers et trois difficultés. De futurs ajouts doivent posséder leurs propres identifiants et mettre à jour le catalogue, les filtres et les conditions de mission concernées explicitement.

## Exemples et pièges

- [GameEngine](../../app/src/main/kotlin/fr/julie/mariosprint/domain/game/GameEngine.kt) simule ; [GameView](../../app/src/main/kotlin/fr/julie/mariosprint/game/GameView.kt) adapte Android ; [GameSceneRenderer](../../app/src/main/kotlin/fr/julie/mariosprint/game/rendering/GameSceneRenderer.kt) dessine.
- [TouchControls](../../app/src/main/kotlin/fr/julie/mariosprint/game/input/TouchControls.kt) partage la géométrie des commandes ; [PlayerProgress](../../app/src/main/kotlin/fr/julie/mariosprint/domain/model/PlayerProgress.kt) porte les règles de déblocage.
- Augmenter le nombre de ticks pour rendre une animation plus fluide peut modifier la simulation ou la consommation ; traiter le besoin dans le rendu avant de toucher au contrat moteur.
- Un test virtuel rapide ne mesure ni le FPS ni la température du Redmi. Une nouvelle UI ne justifie pas l'effacement des scores.

## Vérifications

- Maintenir [GameEngineTest](../../app/src/test/kotlin/fr/julie/mariosprint/game/GameEngineTest.kt), [BonusTest](../../app/src/test/kotlin/fr/julie/mariosprint/game/BonusTest.kt) et [ProgressTest](../../app/src/test/kotlin/fr/julie/mariosprint/game/ProgressTest.kt) selon la règle modifiée.
- Compiler les tests tactiles [TouchControlsTest](../../app/src/androidTest/kotlin/fr/julie/mariosprint/TouchControlsTest.kt) si les commandes évoluent ; leur exécution sur téléphone exige un accord distinct.
- Pour les nouveaux parcours, vérifier le catalogue Kotlin/SQL et l'arrivée avec les commandes normales. Pour les performances, expliquer la méthode et les limites selon [Tests et validation](testing-and-validation.md).
