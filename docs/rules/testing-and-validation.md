# Tests, vérification et livraison

## Périmètre

À lire pour choisir les contrôles d'une tâche, modifier des tests ou livrer une évolution. La section [Documentation](#documentation) suffit pour les vérifications d'une modification documentaire seule ; les règles techniques du sujet documenté restent applicables.

## Références du cours

- Page PDF 135 : comprendre les modifications, avancer par étapes, compiler et vérifier les propositions d'un agent.
- Pages 144-153 : tests avant distribution et suivi des plantages/ANR, sans autorisation implicite de publication.
- Pages 158 et 234-235 : réactivité et écran isolable avec de fausses données.

Les suites JVM, HTTP simulées, SQLite/Postgres et contrôles de concurrence sont les moyens de validation choisis pour MarioSprint. Le cours ne prescrit pas le nombre de tests ni une obligation de tout rejouer pour chaque changement.

## Règles

- Relier les contrôles au comportement modifié et au risque de régression. Ajouter un test sur une règle, une erreur ou une régression utile ; éviter les tests qui recopient les lignes de l'implémentation.
- Examiner les échecs et corriger leur cause. Ne pas retirer une assertion, ignorer un test ou ajouter une suppression lint pour obtenir artificiellement un résultat positif.
- Les versions et le portrait volontairement conservés peuvent produire des avertissements connus : les documenter sans mettre à jour la chaîne contre la demande de l'utilisateur.
- Employer des doubles, une base isolée et un serveur HTTP local pour les cas automatisables. Ne pas créer de compte, course, récompense ou autre donnée dans un compte réel pour valider automatiquement une modification.
- Compiler les tests Android concernés pour maintenir leurs sources. Cette compilation ne les exécute pas et n'autorise aucune interaction sur le téléphone.
- Tester les issues qui comptent : succès, échec, annulation, données préexistantes, changement de propriétaire et réponse ancienne, selon la fonctionnalité.
- Une vérification réussie peut être réutilisée tant que son périmètre n'a pas changé. Répéter/élargir lorsqu'un nouveau changement, un échec ou une incertitude pertinente le justifie.
- Rapporter les commandes/résultats réellement obtenus ; une suite compilée ou un ancien rapport ne devient pas un test exécuté pour une nouvelle modification.

## Choisir les contrôles

| Modification | Vérifications pertinentes |
|---|---|
| Documentation seule | Chemins, liens/ancres, cohérence des consignes, import Claude et diff |
| Kotlin / ViewModel | Tests métier/état concernés, compilation et lint |
| Compose / navigation / ressources | Compilation, lint, Previews représentatives et sources de tests UI compilables |
| Moteur / bonus / niveaux | Tests de simulation, chrono, commandes et arrivée normale ; cohérence Kotlin/SQL si classé |
| Room / données persistées | Migration et schémas exportés, anciennes valeurs conservées, clés, isolation et concurrence |
| API / Auth / synchronisation | Contrats HTTP simulés, renouvellement/annulation, compte courant et acquittements concurrents |
| SQL / RPC | Migrations rejouées localement, permissions, RLS/UID, bornes, atomicité, récompenses et conservation |

Pour les ViewModels, maintenir notamment [ViewModelTest](../../app/src/test/kotlin/fr/julie/mariosprint/ui/ViewModelTest.kt). Pour les frontières de couches et appels bloquants, conserver [ArchitectureTest](../../app/src/test/kotlin/fr/julie/mariosprint/architecture/ArchitectureTest.kt). Les règles par sujet pointent vers les autres tests existants.

## Commandes sur PC

Depuis la racine `MarioSprint/`, avec le JDK/SDK configurés comme dans le [README](../../README.md#compiler-et-installer) :

```powershell
# Compiler l'APK application
.\tools\android.ps1 -Action build

# Exécuter les tests JVM et le lint, sans téléphone
.\tools\android.ps1 -Action test

# Compiler les tests Android sans les installer ni les exécuter
.\gradlew.bat --console=plain --project-cache-dir C:\Android\Caches\MarioSprint :app:assembleDebugAndroidTest

# Vérifier le DDL des migrations et les schémas Room sur SQLite PC, si concernés
python tools/test_local_migration.py
```

Les [tests Postgres locaux](../../supabase/tests/README.md) utilisent PGlite et un schéma Auth simulé. Suivre leur procédure depuis `supabase/tests/` ; leurs dépendances npm servent aux tests SQL et ne sont pas ajoutées à Android.

<a id="documentation"></a>
## Documentation

- Vérifier les liens locaux et ancres, les chemins du routage, les noms de fichiers et les références de pages du cours.
- Maintenir `CLAUDE.md` avec l'unique import `@AGENTS.md`. Les règles détaillées sont référencées par `AGENTS.md` selon la tâche, sans dupliquer leur contenu dans le parent.
- Distinguer les prescriptions du cours, les adaptations MarioSprint et les limites fixées par l'utilisateur. Ne pas attribuer RLS/Supabase ou une fréquence de jeu aux slides qui ne les spécifient pas.
- Préserver les invariants lors d'un déplacement de règles, corriger les anciens liens et éviter une seconde copie du guide sous un autre nom.
- Exécuter `git diff --check`, puis relire le diff et les fichiers nouveaux non encore suivis. Aucun build, installation ou accès au téléphone nécessaire pour ce seul changement.

## Livraison et preuves

- Avant un commit, examiner les fichiers préparés : aucun secret, configuration locale, donnée personnelle, APK, cache, log ou capture. Ne pas retirer wrapper, schémas ou migrations utiles.
- Le push exige une demande explicite pour cette livraison selon [AGENTS.md](../../AGENTS.md#git-services-et-périmètre). Une autorisation passée ne se prolonge pas automatiquement.
- Lorsque l'installation fait partie de la livraison autorisée, `tools/android.ps1 -Action install -Device <numero>` met à jour sans ouvrir le jeu. Ne pas détecter un appareil, lancer une app ou exécuter un test sous prétexte de vérifier l'installation.
- Un téléphone annoncé débranché attend l'indication de disponibilité ou une nouvelle demande d'installation. Toutes les autres limites du téléphone restent dans [AGENTS.md](../../AGENTS.md#téléphone).
- Distinguer explicitement :
  - tests JVM/SQL/HTTP sur données simulées ;
  - compilation de l'application ou des tests Android ;
  - installation confirmée ;
  - rendu réellement inspecté ou parcours réellement joué ;
  - OAuth/écritures réellement validés sur un backend distant ;
  - performances mesurées avec leur méthode et leur contexte.
- Garder résultats datés et limites dans [COURSE_COMPLIANCE.md](../COURSE_COMPLIANCE.md) pour une évolution fonctionnelle ; ne pas mettre des compteurs de tests historiques dans les règles permanentes.

## Exemples et pièges

- Un refus RPC anonyme prouve ce refus, pas l'ensemble des droits d'un utilisateur connecté.
- Un test SQLite PC conserve des données de test ; il ne remplace pas une exécution de migration par Room sur Android.
- Une Preview ajoutée au code ne prouve pas une inspection de son rendu. Un APK installé ne prouve pas que le jeu a été ouvert, parcouru ou connecté.
- L'exemple de skill du cours qui lance `adb devices` puis démarre l'app ne s'applique pas automatiquement : les limitations de l'utilisateur restent prioritaires.
