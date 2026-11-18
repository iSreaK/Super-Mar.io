# Configurer Supabase et Google — Mario Sprint

Le jeu local fonctionne immédiatement sans compte. Le réseau devient actif une fois ces étapes terminées et l'APK recompilé. Les tests HTTP automatisés utilisent un serveur simulé sur le PC ; la connexion Google et les classements du projet distant doivent aussi être vérifiés manuellement par Julie après configuration.

Pour les amis et les profils de la version 1.6, compléter ces étapes avec [FRIENDS_SETUP.md](FRIENDS_SETUP.md) : troisième migration SQL, activation de Realtime privé et contrôles en lecture seule. Les identifiants Google/Supabase existants sont réutilisés.

## 1. Créer le projet Supabase

Ouvrir https://supabase.com/dashboard, créer une organisation personnelle si nécessaire, puis **New project**. Choisir un nom, une région proche des joueurs (Europe) et un mot de passe de base fort. Conserver ce mot de passe dans ton gestionnaire ; il ne va jamais dans l'application. Attendre la préparation de la base.

Dans **Connect** ou **Project Settings → API / API Keys**, relever :

- **Project URL** : https://REFERENCE.supabase.co
- **Publishable key** : commence par **sb_publishable_**.

Ce sont les identifiants publics attendus par le client. Ne pas utiliser de clé **sb_secret_**, **service_role**, de mot de passe Postgres ou de secret OAuth dans l'APK. Les clés publiques n'accordent pas les permissions des utilisateurs ; les accès sont limités par Auth et les fonctions SQL.

## 2. Créer les niveaux et protéger les records

Pour un nouveau projet, dans **SQL Editor → New query**, coller tout le fichier **supabase/migrations/202610150001_levels_and_rankings.sql**, puis **Run**. Exécuter ensuite de la même façon **supabase/migrations/202611180001_courses_missions_and_skins.sql**. Les deux scripts sont transactionnels et rejouables sans effacer les records.

**Si la version 1.3 est déjà configurée**, exécuter seulement le second fichier : **202611180001_courses_missions_and_skins.sql**. Aucun identifiant Google ou clé n'est à recréer.

La migration initiale crée les trois niveaux historiques, la table best_runs, son index de classement, la RLS et deux fonctions :

- submit_run : identité issue de la session Google, bornes validées, meilleur temps atomique par niveau/UID ; un temps égal ou moins bon ne remplace rien.
- get_leaderboard : au maximum 50 résultats, tri par temps puis UID pour départager les égalités.

L'extension ajoute six parcours (neuf au total, trois univers × trois difficultés), les métadonnées des niveaux, la table player_appearance et deux RPC :

- get_player_progress : retourne uniquement les niveaux terminés du compte courant et son personnage équipé. Les missions sont déduites de ses records en ligne.
- equip_skin : vérifie la mission avant de sauvegarder le personnage pour auth.uid(). Luigi est toujours disponible ; Toad demande un premier niveau, Yoshi les trois univers, Peach les trois parcours Expert.

Les écritures directes dans player_appearance sont interdites, avec RLS activée. Aucun UID ni compteur de missions n'est accepté depuis le client. Les personnages sont cosmétiques ; les temps restent calculés par l'application, avec la même limite contre un client modifié qu'en version 1.3.

Les rôles anon/authenticated ne peuvent pas lire ou modifier directement les tables. Seul authenticated peut appeler les deux fonctions, qui vérifient aussi le fournisseur Google. Le nom affiché est nettoyé et limité à 40 caractères ; aucun email, jeton ou photo n'est ajouté aux classements.

Lancer ensuite **supabase/checks.sql**, qui ne fait que lire la structure. Vérifier les neuf niveaux actifs, relrowsecurity=true, toutes les permissions directes à false, EXECUTE=false pour anon et true pour authenticated. Les quatre fonctions doivent afficher prosecdef=true et search_path vide.

Pour activer aussi les amis, appliquer ensuite **supabase/migrations/202611180002_friend_requests_and_profiles.sql**, puis suivre les réglages et contrôles du [guide social](FRIENDS_SETUP.md). Cette troisième migration conserve les données des deux précédentes.

## 3. Préparer Google Cloud

Ouvrir https://console.cloud.google.com/ et sélectionner ou créer un projet pour Mario Sprint. Aller dans **Google Auth Platform** (ou **APIs & Services → OAuth consent screen** selon la console).

Configurer **Branding** : nom Mario Sprint, email d'assistance et contact développeur. Dans **Audience**, choisir **External** pour des comptes Gmail personnels. Pour ce projet privé, le mode **Testing** suffit. Avec uniquement les scopes d'identité ci-dessous, Google n'exige pas l'ajout des joueurs dans **Test users** : l'exception Sign in with Google s'applique. D'autres scopes pourraient changer cette règle.

Dans **Data Access**, utiliser uniquement les scopes d'identité **openid**, **userinfo.email** et **userinfo.profile**. Aucune API Drive, Contacts ou autre permission n'est nécessaire.

## 4. Créer le client OAuth Web

Dans Google Auth Platform → **Clients → Create client → Web application**, créer un client « Mario Sprint Supabase ».

Pour **Authorized redirect URIs**, ajouter exactement la **Callback URL** affichée par Supabase dans **Authentication → Sign In / Providers → Google** : généralement **https://REFERENCE.supabase.co/auth/v1/callback**. Ne pas deviner la référence, copier l'URL de ton propre projet.

Enregistrer et récupérer le **Client ID Web** (suffixe .apps.googleusercontent.com) et le **Client secret**. Le secret va uniquement dans le panneau du fournisseur Google de Supabase, à l'étape 6. Aucun secret ne doit être envoyé dans une conversation, enregistré dans le projet Android ou inclus dans Git.

## 5. Créer le client OAuth Android

Dans le même projet Google Cloud, créer un second client de type **Android** :

- Nom : Mario Sprint Android debug.
- Package : **fr.julie.mariosprint**.
- SHA-1 debug de la clé actuelle du PC : **68:08:FB:15:97:78:9A:CA:1E:B1:5D:D8:70:E3:A4:FA:5B:8C:EC:F8**.

Cette empreinte a été lue depuis la clé de compilation sur le PC, sans accéder au téléphone. Pour la recalculer : lancer **.\tools\android.ps1 -Action sha** à la racine MarioSprint. En cas de nouvelle clé ou de version release, créer le client Android correspondant à sa propre SHA-1. Ne pas confondre le Client ID Android et le Client ID Web attendu dans l'application.

## 6. Activer Google dans Supabase

Revenir dans **Authentication → Sign In / Providers → Google**. Activer Google. Dans la liste des **Client IDs**, mettre le **Client ID Web en premier**, puis le **Client ID Android**, séparés par une virgule. Copier le **Client secret Web** dans le champ secret du fournisseur. Enregistrer.

Laisser **Skip nonce checks désactivé**. Le code envoie le SHA-256 du nonce à Google et le nonce brut à Supabase, conformément au flux natif. Ne pas activer les connexions anonymes ; les comptes de classement utilisent Google. Les autres fournisseurs ne sont pas utilisés par l'app.

Le jeu utilise Credential Manager et l'échange d'un ID token Google : aucun navigateur, schéma de deep link Android ou redirect URI mobile supplémentaire n'est nécessaire pour ce flux. L'URI de callback précédente appartient à Supabase et au client OAuth Web.

## 7. Renseigner les trois identifiants publics dans le projet

Copier **supabase.properties.example** en **supabase.properties**, à la racine MarioSprint (même dossier que settings.gradle.kts), puis remplacer les trois valeurs :

    SUPABASE_URL=https://REFERENCE.supabase.co
    SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
    GOOGLE_WEB_CLIENT_ID=CLIENT_WEB.apps.googleusercontent.com

Copier l'ID client Web en entier, y compris le tiret après le numéro de projet et le suffixe .apps.googleusercontent.com. Ne pas entourer les valeurs de guillemets. Ce fichier est ignoré par Git. Le build refuse les clés secrètes et les URL hors HTTPS Supabase. Sans configuration complète, les neuf niveaux et les scores locaux fonctionnent ; le bouton Google explique ce qui manque.

Aucun google-services.json n'est nécessaire. Firebase et son plugin ont été retirés. La chaîne Kotlin/AGP/Gradle/SDK initiale reste inchangée.

## 8. Compiler et installer uniquement

Depuis le dossier MarioSprint :

    .\tools\android.ps1 -Action test
    .\tools\android.ps1 -Action install -Device d2627d0d

La première commande exécute uniquement les tests JVM et le lint sur le PC. La seconde compile et met à jour l'APK, sans ouvrir le jeu et sans effacer les records. Le téléphone doit être branché, déverrouillé et autoriser l'installation USB. Les scripts run et test -Connected exigent un accord humain explicite préalable et le drapeau AllowPhoneInteraction ; ils ne font pas partie de cette procédure.

## 9. Faire toi-même le premier essai connecté

Ouvrir Mario Sprint sur le téléphone, toucher **Continuer avec Google**, choisir son compte Google. Sélectionner un niveau et finir une course sans pause ni passage en arrière-plan. Le résultat doit annoncer sa synchronisation. Ouvrir son classement, puis comparer avec un second compte si souhaité.

Dans Supabase, **Authentication → Users** doit contenir le joueur et **Table Editor → best_runs** son record pour le bon level_id. Ces consultations administrateur ne modifient pas les permissions de l'app. Chaque niveau conserve son meilleur temps séparément.

Après ce premier niveau, ouvrir **Missions & personnages** : Toad doit être débloqué. L'équiper, puis utiliser **Synchroniser mon compte**. Dans **Table Editor → player_appearance**, le compte doit afficher skin_id=toad. Terminer un niveau dans chacun des trois univers débloque Yoshi ; les trois parcours Expert débloquent Peach. Les skins ne changent ni la vitesse ni les collisions. Les records du compte permettent de retrouver les déblocages sur un autre téléphone après connexion et synchronisation. Le profil invité reste local et séparé.

Une course invitée reste locale. Une connexion en cours de partie ne réattribue pas ce temps. En cas de réseau absent, l'envoi est conservé localement pour le bon UID et niveau ; toucher **Synchroniser** après retour du réseau. Aucun service ne tourne en arrière-plan. Les temps historiques prairie-1-v1 sont conservés séparément et ne sont pas réenvoyés comme s'ils appartenaient aux nouveaux parcours.

## Si la connexion échoue

- Google refuse le compte : vérifier Audience, le projet Google Cloud choisi et les restrictions éventuelles du compte Google (organisation ou protection avancée). Avec les seuls scopes d'identité du jeu, l'absence dans Test users ne bloque pas la connexion.
- Le sélecteur échoue : vérifier le package exact, la SHA-1 de l'APK et le Client ID Web dans supabase.properties, puis recompiler.
- Supabase refuse le jeton : vérifier que les Client IDs du fournisseur sont ceux du même projet Google, Web en premier, que le secret est enregistré et que les contrôles de nonce restent actifs.
- RPC introuvable : exécuter le script SQL entier dans le bon projet. Il recharge le cache de schéma PostgREST.
- Record non envoyé : il doit venir d'une course Google commencée connectée, sans pause, entre 10 secondes et 10 minutes, sur une version de niveau active. Réessayer avec Synchroniser.
- Session expirée/révoquée : se reconnecter ; aucun record local n'est effacé.

Le classement reste fondé sur un chrono calculé dans l'application. Les droits serveur empêchent l'usurpation d'UID et l'écrasement par un temps plus lent ; une compétition publique demanderait aussi une validation de parcours côté serveur contre les clients modifiés.

## Références officielles

- [Connexion Google native et nonces Supabase](https://supabase.com/docs/guides/auth/social-sign-in/auth-google)
- [Audience Google et exception des scopes d'identité](https://support.google.com/cloud/answer/15549945?hl=en)
- [Clés publiques et secrètes Supabase](https://supabase.com/docs/guides/getting-started/api-keys)
- [RLS Postgres](https://supabase.com/docs/guides/database/postgres/row-level-security)
- [Sessions et renouvellement](https://supabase.com/docs/guides/auth/sessions)
- [Contrat HTTP Supabase Auth](https://github.com/supabase/auth/blob/master/openapi.yaml)
- [Credential Manager / Sign in with Google Android](https://developer.android.com/identity/sign-in/credential-manager-siwg)
