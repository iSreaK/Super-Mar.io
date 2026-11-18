# Room et données locales

## Périmètre

À lire pour `data/local/`, les DAO, entités, mappings locaux, repositories/cache, préférences, schémas et migrations. Ajouter [Supabase et synchronisation](supabase-and-sync.md) si une réponse distante, un envoi ou un propriétaire de compte est concerné.

## Références du cours

- Pages PDF 188-202 : Room/SQLite, KSP, Entity/DAO, base unique et classe Application.
- Pages 208-211 : extensions, mapping et repository devant le DAO.
- Pages 248 et 252-255 : évolution de la base et chemin réseau -> Room -> modèle -> UI.

Les slides 200 et 202 utilisent `exportSchema = false` et `fallbackToDestructiveMigration` pour leur démonstration. **Ces simplifications ne s'appliquent pas à MarioSprint**, dont les données doivent être conservées.

## Règles

- Une seule base au niveau Application, assemblée dans le conteneur. Les ViewModels et Screens ne créent pas de base et n'accèdent pas aux DAO directement.
- DAO dédiés avec requêtes explicites, fonctions suspend pour les écritures et Flow pour les observations utiles. Les résultats Room sont mappés par le repository vers le métier.
- Entités, modèles métier et State UI sont séparés ; aucune annotation de persistance sur un modèle métier juste pour éviter un mapping.
- Room reste la source de vérité locale : une réponse distante met à jour le cache, puis les observations alimentent l'UI. Ne pas maintenir deux listes concurrentes, une distante et une locale, sans contrat clair.
- Les opérations qui doivent rester cohérentes sont transactionnelles. Une erreur ne doit pas laisser un compteur, un envoi ou une récompense à moitié enregistré.
- Indexer les recherches correspondant aux clés de propriétaire/parcours ; borner les caches et requêtes. Éviter les lectures répétées à chaque image ou chaque recomposition.
- Aucun `allowMainThreadQueries`. Préférences, import initial et stockage de session ne lisent pas le disque sur Main.
- Toute modification de schéma requiert une version adaptée, une migration explicite, un schéma exporté et une vérification de conservation. Ne pas modifier un ancien schéma exporté ou une ancienne migration appliquée pour masquer un défaut.
- Aucun fallback destructif, effacement des tables ou désinstallation de l'app pour faire passer une évolution.
- Garder les configurations du poste et données d'exécution hors du dépôt. Les schémas Room et sources de migration restent versionnés.

## Adaptations MarioSprint

- Conserver les records historiques, statistiques, réglages, cache et envois en attente lors d'une migration. Les anciens parcours restent associés à leurs identifiants, même si l'UI ne les propose plus.
- Progression invitée et progression de chaque UID sont séparées. Une connexion ou un changement de compte ne réattribue pas les courses et récompenses existantes.
- Sauvegarder résultat de course, statistiques, record en attente et validation de mission dans la transaction prévue par leur contrat.
- Un acquittement n'efface un envoi que si le résultat attendu correspond encore ; un meilleur temps arrivé pendant l'appel doit rester en attente.
- Une révision protège le choix de personnage d'une réponse distante ancienne. Ne pas remplacer cette vérification par une mise à jour inconditionnelle de l'apparence.
- Valider les identifiants lus : ne pas transformer un parcours inconnu en parcours courant, ni un propriétaire absent en autre UID. Conserver les données historiques même si elles sont ignorées pour les missions actuelles.
- Le cache de classement est lié au compte/parcours et limité au Top 50. À un changement de compte, cesser d'exposer l'ancien cache sans effacer les records personnels.
- Garder le contrat de préférences existant ; ajouter un nouveau stockage uniquement si une fonctionnalité le nécessite.

## Exemples et pièges

- [SprintDatabase](../../app/src/main/kotlin/fr/julie/mariosprint/data/local/SprintDatabase.kt), [DatabaseMigrations](../../app/src/main/kotlin/fr/julie/mariosprint/data/local/DatabaseMigrations.kt) et [schémas](../../app/schemas/fr.julie.mariosprint.data.local.SprintDatabase) décrivent les versions à préserver.
- [RoomRecordsRepository](../../app/src/main/kotlin/fr/julie/mariosprint/data/repository/RoomRecordsRepository.kt) écrit une course ; [RoomProgressRepository](../../app/src/main/kotlin/fr/julie/mariosprint/data/repository/RoomProgressRepository.kt) protège les choix de personnage.
- Mettre seulement `version = version + 1` dans la base ne remplace pas une migration. Une base neuve qui fonctionne ne prouve pas qu'une ancienne installation conserve ses données.
- Une validation SQLite du DDL ne prouve pas à elle seule que Room exécute correctement toute la migration sur Android.

## Vérifications

- Tester conservation des anciennes tables/valeurs, correspondance au schéma exporté, clés/contraintes, isolation invité/UID et concurrence des acquittements selon le changement.
- Utiliser [la validation SQLite PC](../../tools/test_local_migration.py), les tests JVM des mappings/synchronisations et maintenir [RoomRecordsTest](../../app/src/androidTest/kotlin/fr/julie/mariosprint/RoomRecordsTest.kt) compilable.
- L'exécution instrumentée sur téléphone exige l'accord prévu dans [AGENTS.md](../../AGENTS.md#téléphone). Choisir les commandes selon [Tests et validation](testing-and-validation.md).
