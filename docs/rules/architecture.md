# Architecture et responsabilités

## Périmètre

À lire pour toute évolution de code : nouveau contrat, fonctionnalité, déplacement de responsabilité, injection ou refactorisation. S'applique aux couches `ui/`, `domain/`, `data/`, `game/` et `architecture/` ainsi qu'à leurs tests.

## Références du cours

- Pages PDF 129 et 138 : packages et séparation des couches.
- Pages 166, 176-187 : ViewModel, repository, modèles distincts, SOLID/KISS/DRY et domaine Kotlin pur.
- Pages 190, 240 et 252-255 : place des sources et circulation des modèles.

Les exemples intermédiaires du cours construisent parfois directement un repository ou utilisent une Application globale. L'injection par constructeur ci-dessous est une convention de MarioSprint appliquant la dépendance aux abstractions.

## Règles

| Couche | Responsabilité | Dépendances attendues |
|---|---|---|
| `domain/` | Règles, modèles métier, simulation et interfaces | Kotlin pur ; aucune plateforme ou implémentation de données |
| `data/` | Room, session, Retrofit/DTO, sources, mappings et implémentations | Contrats métier et bibliothèques nécessaires aux sources |
| `ui/` | State/Action/Event, présentation, coordination et navigation | Contrats métier ; adaptateurs Android assemblés au Root |
| `game/` | Canvas, audio, entrées et adaptation Android du moteur | Simulation métier ; aucune source Room ou Supabase |
| `architecture/` | Création et assemblage des dépendances | Implémentations et contrats nécessaires à l'injection |

- Un repository est la porte d'entrée vers les données pour le ViewModel. Le Screen ne connaît ni repository, ni DAO, ni client HTTP, ni ViewModel.
- Le Root obtient le ViewModel, collecte son état et assemble les adaptateurs ; le Screen affiche et émet des actions. Les règles métier ne résident pas dans un composable.
- Le modèle métier ne porte pas d'annotations Room/JSON. Les entités locales, DTO distants et modèles UI sont distincts, avec conversions explicites aux frontières.
- Injecter les dépendances par constructeur via des interfaces ciblées. Ne pas aller chercher un conteneur global depuis une classe métier ou un ViewModel.
- Une classe a une responsabilité identifiable. Une modification doit pouvoir s'expliquer sans imposer une réécriture des autres couches.
- Étendre un contrat lorsqu'un besoin réel l'exige, conserver ses garanties pour toutes ses implémentations et tester les comportements partagés. Éviter les variantes qui acceptent le type mais changent silencieusement son sens.
- Préférer de petites interfaces à un service regroupant tous les accès de l'application. Une abstraction doit isoler une responsabilité ou faciliter un usage/test concret.
- Appliquer KISS et DRY avec mesure : factoriser une logique commune, sans introduire un framework, un module ou une classe générique pour une seule ligne.

## Adaptations MarioSprint

- Le domaine, optionnel dans l'exemple simplifié du cours, est nécessaire à la simulation et aux missions du jeu. Garder `domain` indépendant d'Android, Compose, Room et Supabase.
- L'injection manuelle est assemblée dans `AppContainer`, au niveau Application. Ne pas ajouter Hilt/Koin ou un nouveau conteneur pour résoudre un problème ponctuel.
- Le moteur mutable est transmis par le Root à l'adaptateur Canvas. Il ne devient pas un modèle de State Compose et ne justifie pas un partage général d'objets mutables entre écrans.
- Un écran statique peut se passer de ViewModel. Un cas d'usage supplémentaire se justifie par une coordination métier utile, pas par une obligation de créer une couche par appel.

## Exemples et pièges

- [AppContainer](../../app/src/main/kotlin/fr/julie/mariosprint/architecture/AppContainer.kt) assemble les dépendances ; [ProgressRepository](../../app/src/main/kotlin/fr/julie/mariosprint/domain/repository/ProgressRepository.kt) en expose le contrat métier.
- [ProgressMapping](../../app/src/main/kotlin/fr/julie/mariosprint/data/mapping/ProgressMapping.kt) est une frontière de conversion ; ce mapping n'a pas à être recopié dans un écran.
- Déplacer une requête de `Screen` vers une fonction utilitaire appelée par ce même Screen ne rétablit pas la séparation des responsabilités.
- Les implémentations existantes sont des repères à relire, pas une preuve automatique qu'un nouveau cas respecte leurs garanties.

## Vérifications

- Examiner les imports et le sens des dépendances, notamment pour tout nouveau fichier.
- Maintenir [ArchitectureTest](../../app/src/test/kotlin/fr/julie/mariosprint/architecture/ArchitectureTest.kt) et les tests des contrats modifiés.
- Compléter par les règles de la couche touchée et [Tests et validation](testing-and-validation.md).
