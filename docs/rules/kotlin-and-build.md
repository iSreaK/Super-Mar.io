# Kotlin, conventions et build

## Périmètre

À lire pour les sources Kotlin, les nouveaux fichiers/classes, le catalogue de versions, les scripts Gradle, les SDK et toute dépendance. Les conventions de nommage s'appliquent également aux tests et ressources.

## Références du cours

- Page PDF 20 : projet Android et Kotlin DSL.
- Pages 32-50 : valeurs, collections, fonctions, paramètres, nullabilité et visibilité.
- Pages 52-59 : structure du projet, wrapper, catalogue, SDK et dépendances.
- Pages 191-195 : génération Room/KSP ; les numéros de version montrés restent des exemples.

## Règles

- `val` par défaut, `var` seulement pour une mutation nécessaire et maîtrisée. `const val` pour les constantes compatibles connues à la compilation.
- Une référence `val` vers une collection mutable n'est pas une garantie d'immuabilité. Ne pas exposer une collection que le producteur continue à modifier.
- Visibilité minimale : `private` pour les détails, `internal` pour l'API du module quand suffisant. Ne pas ouvrir un setter ou un objet mutable uniquement pour simplifier un appel UI.
- Classes/composables : `UpperCamelCase` ; fonctions/propriétés : `lowerCamelCase` ; constantes : `UPPER_SNAKE_CASE` ; ressources Android : `snake_case`.
- Noms exprimant le rôle et l'unité : `timeMs`, `starSeconds`, propriétaire/UID explicite. Éviter les suffixes vagues comme `Manager`, `Helper` ou `Utils` sans responsabilité précise.
- Fonctions ciblées, paramètres nommés lorsque plusieurs valeurs se ressemblent, valeurs par défaut seulement si elles ont un sens métier réel.
- Nullabilité explicite et traitement du cas absent. Pas de `!!` ; ne pas remplacer une identité invalide par un compte ou parcours arbitraire.
- Conserver les versions Kotlin/AGP/Gradle/SDK et dépendances existantes sauf demande explicite. Lire les valeurs dans le catalogue et les fichiers de build ; les slides ne déclenchent aucune mise à niveau.
- Déclarer les versions et alias dans `gradle/libs.versions.toml`, utiliser les scripts `.gradle.kts` et le wrapper du dépôt.
- Placer une dépendance selon son usage : production, test JVM, test Android, debug ou génération KSP. Un outil SQL local ne devient pas une dépendance Android.
- Distinguer `compileSdk`, `minSdk` et `targetSdk` ; une modification doit tenir compte des comportements Android et des appareils supportés, pas seulement faire compiler un appel.
- Ne pas modifier les fichiers générés pour corriger un problème de source ou de configuration.

## Adaptations MarioSprint

- Kotlin natif, Compose et sources Retrofit existantes : aucun ajout npm/SSR pour utiliser Supabase dans Android.
- La configuration locale est ignorée par Git. Le wrapper et les schémas Room exportés sont versionnés ; les APK, caches et secrets restent exclus.
- Les ajustements mémoire/cache Gradle existants répondent aux contraintes du poste. Ne pas multiplier les workers ou augmenter les limites sans problème observé et justification.
- Conserver le package et l'identité de l'application pour permettre une mise à jour sans désinstallation.

## Exemples et pièges

```kotlin
val values: List<Int> = mutableListOf(1, 2)
```

Ce type limite les opérations du consommateur, mais ne prouve pas que la liste d'origine ne sera jamais modifiée. Pour le State, produire une valeur dont la mutation ne fuit pas.

- [Catalogue](../../gradle/libs.versions.toml), [build application](../../app/build.gradle.kts) et [wrapper](../../gradle/wrapper/gradle-wrapper.properties) sont les références concrètes des versions.
- Un avertissement proposant une version plus récente ne constitue pas une autorisation de mettre à jour la chaîne conservée par l'utilisateur.

## Vérifications

- Vérifier nommage, unités, nullabilité, visibilité et compatibilité des alias de catalogue.
- Pour un changement de build/dépendance, compiler les variantes et tests concernés puis lint ; contrôler les fichiers Git pour éviter les générés/configurations locales.
- Suivre [Tests et validation](testing-and-validation.md) sans relancer une compilation pour un changement documentaire seul.
