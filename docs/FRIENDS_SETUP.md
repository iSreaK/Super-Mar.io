# Amis, profils et Realtime

Cette extension réutilise les sessions Google/Supabase et les clés publiques déjà configurées. Il n'y a ni Firebase/FCM, ni notification Android, ni permission supplémentaire. « À propos » est une page locale : Julien / iSreaK et Clément / KiSs.

## 1. Appliquer la migration SQL

1. Ouvrir le projet existant dans le Dashboard Supabase et vérifier qu'il correspond à l'URL de `supabase.properties`.
2. Aller dans **SQL Editor → New query**.
3. Si les deux migrations des niveaux/skins sont déjà exécutées, copier **uniquement** tout le contenu de [202611180002_friend_requests_and_profiles.sql](../supabase/migrations/202611180002_friend_requests_and_profiles.sql), puis cliquer **Run**.
4. Pour un nouveau projet, suivre d'abord [SUPABASE_SETUP.md](SUPABASE_SETUP.md), appliquer `202610150001_levels_and_rankings.sql`, puis `202611180001_courses_missions_and_skins.sql`, puis cette troisième migration.
5. La migration crée les profils des comptes existants, leur code stable et un trigger pour les comptes futurs. Elle conserve les records, les personnages et les relations déjà enregistrées lors d'une réexécution.

L'erreur « Supabase Realtime doit être disponible » signifie que les objets Realtime manquent : vérifier le service à l'étape 2 avant de rejouer la migration entière. Ne pas supprimer de tables pour résoudre cette erreur.

## 2. Configurer les canaux privés

1. Ouvrir **Realtime → Settings** dans le Dashboard du projet. Le [lien officiel vers ces réglages](https://supabase.com/dashboard/project/_/realtime/settings) permet aussi de choisir le projet.
2. Laisser **Enable Realtime service** activé.
3. Désactiver **Allow public access to channels** et enregistrer. Les abonnements du jeu utilisent déjà `private: true`.
4. Conserver les limites, tailles de pools et l'offre existantes ; ces étapes ne demandent aucune modification de quota ou de formule.

Les noms peuvent être affichés en anglais même dans une console francisée. Le réglage de canaux privés est décrit dans les [réglages officiels](https://supabase.com/docs/guides/realtime/settings) et l'[autorisation Realtime](https://supabase.com/docs/guides/realtime/authorization). Une modification de ces réglages déconnecte les clients ; l'application se réabonne lorsqu'elle est visible.

Il ne faut **pas** activer Postgres Changes pour `auth.users`, `player_profiles` ou `friend_relations`, ni les ajouter à une publication. Le trigger serveur utilise Broadcast sur `realtime.messages`. Il ne faut pas exécuter `ALTER TABLE realtime.messages ENABLE ROW LEVEL SECURITY` : Supabase en gère déjà la RLS et la propriété.

## 3. Vérifier les droits sans toucher aux comptes

Copier [friends_checks.sql](../supabase/friends_checks.sql) dans une nouvelle requête SQL et cliquer **Run**. Ce fichier est en lecture seule.

Résultats attendus :

- `rls_enabled = true` pour les deux tables sociales et `realtime.messages`.
- Les trois RPC `get_friend_snapshot`, `friend_action` et `get_social_profile` ont `security_definer = true`, un `search_path` vide, `authenticated_execute = true` et `anon_execute = false`.
- Les quatre helpers privés ont `authenticated_execute = false` et `anon_execute = false`.
- Les deux tables métier ont `anon_direct_access = false` et `authenticated_direct_access = false`.
- Les trois policies `mariosprint_friends_receive`, `mariosprint_friends_scope`, `mariosprint_friends_no_client_send` existent. La première vise `authenticated` ; les deux dernières sont **RESTRICTIVE** pour `public` (tous les rôles), y compris les appels anonymes.
- Les deux triggers sont activés (`tgenabled = O`), et la table possède une clé primaire `(low_uid, high_uid)`, une contrainte unique sur `id` et la contrainte `low_uid < high_uid`.

Le SQL Editor utilise un rôle d'administration, sans session Google du jeu. Y appeler directement une RPC utilisateur ne constitue pas un test de connexion authentifiée. Ne jamais accorder de droits directs aux clients ou introduire une clé privilégiée dans Android pour faire passer un test.

## 4. Configuration Android

Les valeurs existantes `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` et `GOOGLE_WEB_CLIENT_ID` suffisent. Aucun nouveau client OAuth, scope Google ou fichier `google-services.json` n'est requis.

Room passe de version 2 à 3 avec quatre tables de cache supplémentaires. La migration 1 → 2 reste conservée, puis 2 → 3 s'applique automatiquement lors de la mise à jour. Aucune désinstallation ni suppression des données n'est nécessaire. Les schémas exportés 1, 2 et 3 restent dans Git.

Les amis et demandes sont accessibles depuis l'icône sur l'accueil ou Mon compte. Un joueur du classement peut être invité directement ou son profil ouvert. Le code personnel se copie depuis la page Amis. « À propos » se trouve dans Mon compte.

## Comportement et garanties

- Une paire de joueurs possède au plus une relation : une demande reçue/envoyée ou une amitié. Les demandes croisées conservent la demande initiale ; seul son destinataire peut l'accepter.
- Chaque nouvelle demande reçoit un identifiant propre. Accepter, refuser, annuler ou retirer avec un ancien identifiant ne touche pas une nouvelle relation créée après suppression.
- Le serveur déduit l'appelant du JWT Google, vérifie son existence et n'accepte pas un UID appelant fourni par le client. Le nom vient d'Auth, nettoyé et borné à 40 caractères. Les codes sont générés côté serveur ; ils identifient un joueur et ne valent pas acceptation d'une invitation.
- Les lignes des deux profils sont verrouillées dans le même ordre pour les mutations. La clé de paire et les vérifications sous verrou protègent les appels simultanés et les quotas des deux participants : 200 amis, 100 demandes en attente, 30 nouvelles invitations par jour UTC pour l'émetteur. Les doublons ne consomment pas ce dernier quota.
- Les canaux `friends:<UID>` sont personnels et en réception seule pour le client. Les policies restrictives empêchent une ancienne policy permissive d'autoriser l'écoute d'un autre compte ou l'envoi de fausses alertes dans ce namespace.
- Le serveur diffuse uniquement `{ "version": 1 }` avec l'événement `friends_changed`. Le client relit la RPC, écrit le snapshot dans une transaction Room, puis l'UI l'observe. Un message Realtime ne devient pas une donnée métier fiable à lui seul.
- Le cache des relations est séparé par UID ; celui des profils visités l'est aussi et conserve au plus 50 profils par compte. Une révision serveur empêche un ancien snapshot de remplacer une version plus récente.
- Le compteur correspond aux demandes **reçues en attente**, pas à une permission de notification Android. Une courte snackbar signale les nouvelles demandes détectées après enregistrement ; les demandes restent disponibles même si ce message n'est pas affiché.
- Les mutations n'utilisent pas de mise à jour optimiste ni de file d'actions hors connexion : une erreur garde le cache et permet un réessai explicite. Une réponse perdue après une écriture distante est réconciliée par la récupération suivante.
- Realtime est un flux froid contrôlé par STARTED et le compte courant : arrêt des sockets, tâches et réessais hors visibilité ou à la déconnexion. Aucun service, worker ou récepteur en arrière-plan.
- Une récupération complète a lieu à l'ouverture, après abonnement/reconnexion et lors d'un rafraîchissement manuel. Les rafales d'invalidations sont regroupées tout en conservant une récupération après un appel déjà en cours.
- Le client utilise le [protocole Realtime v1](https://supabase.com/docs/guides/realtime/protocol) avec OkHttp existant : heartbeat de 20 s, contrôle du jeton de 30 s, mise à jour JWT en bande et réessai avec délai croissant de 1 à 30 s uniquement au premier plan. Le fournisseur de jetons existant renouvelle à l'approche de l'expiration ; REST conserve sa relance unique après 401.
- Les profils affichent nom public, personnage et records actifs publiés. Le code ami n'est retourné que pour son propre profil ; aucun email, photo Google ou jeton n'est publié. Un profil est accessible à son propriétaire, aux joueurs liés et aux utilisateurs du classement, sans annuaire global des comptes.

## Vérification réelle à effectuer après configuration

Utiliser deux comptes Google de test choisis par l'utilisateur sur deux installations du jeu. Ces étapes sont manuelles ; ce guide n'autorise pas l'agent à manipuler le téléphone.

1. A copie son code ; B l'utilise pour envoyer une demande. A, application visible, voit une alerte discrète et le compteur augmenter. B voit une demande envoyée.
2. A accepte : les deux listes affichent l'amitié. Ouvrir le profil de l'autre et vérifier les records publiés.
3. Retirer l'ami, puis refaire une demande et la refuser. Vérifier annulation côté émetteur sur une nouvelle demande.
4. Envoyer simultanément de chaque côté : une seule demande doit subsister, sans amitié implicite. Accepter deux fois ou répéter une annulation ne doit créer aucun doublon.
5. Mettre A en arrière-plan, faire une demande depuis B, puis rouvrir A : la demande doit être récupérée. Aucune notification système n'est attendue pendant l'absence.
6. Couper puis rétablir le réseau : conserver les données affichées, montrer les erreurs et récupérer les modifications à la reconnexion.
7. Changer de compte sur A : l'ancien code, les anciennes relations et alertes ne doivent plus être affichés au nouveau compte.
8. Laisser l'application visible au-delà de l'expiration du JWT pour valider le renouvellement sur le vrai service, puis vérifier les logs Realtime du Dashboard sans partager de jeton.

## Message « Le service amis est indisponible » en version 1.6.0

Mettre à jour les deux installations vers **1.6.1**. La version 1.6.0 utilisait GET pour `get_friend_snapshot`, alors que cette RPC peut créer ou actualiser le profil. PostgREST exécute GET en transaction READ ONLY ; l'écriture interne échoue avec le code PostgreSQL `25006`. La version 1.6.1 utilise POST pour permettre la transaction READ WRITE. Voir les [modes de transaction PostgREST](https://docs.postgrest.org/en/stable/references/transactions.html).

Cela explique qu'un envoi de demande en POST puisse réussir tandis que les listes et les alertes ne sont pas actualisées. Realtime déclenche une relecture de cette même RPC ; sans relecture réussie, le client ne dispose pas des nouvelles demandes à enregistrer et à signaler. Aucun SQL supplémentaire, changement de RLS, réinstallation avec effacement ou recréation de compte n'est nécessaire pour cette correction.

Pour vérifier l'alerte, garder l'application du destinataire **visible**, puis envoyer une **nouvelle** demande depuis l'autre compte. Une demande déjà en cache ne déclenche pas un nouveau message à chaque ouverture. Hors visibilité, il n'y a pas de notification système : les demandes sont récupérées au retour dans le jeu.

## Portée des tests locaux

Les tests SQL PGlite exécutent les migrations de production avec Auth, rôles et fonctions Realtime simulés. Ils contrôlent les droits, RLS, règles de relation, codes, quotas, profils, déclencheurs et conservation. Ils ne démarrent pas le service Realtime et ne reproduisent pas des transactions PostgreSQL parallèles sur plusieurs connexions.

Les tests JVM utilisent un serveur HTTP/WebSocket local pour les contrats Retrofit et Phoenix, et des coroutines contrôlées pour le cycle de vie, les comptes et la récupération. Les tests SQLite PC vérifient le DDL contre le schéma Room exporté, les données anciennes, les clés, transactions et bornes de cache. Ils ne remplacent pas l'ouverture d'une ancienne base par Room sur Android.

Les tests Android sont maintenus et compilés ; leur exécution demande un accord spécifique. Les résultats effectivement obtenus pour cette évolution sont consignés dans [COURSE_COMPLIANCE.md](COURSE_COMPLIANCE.md). Rien dans cette livraison ne prouve encore le déploiement de la migration, les paramètres Realtime du projet distant, OAuth réel ou des performances mesurées sur appareil.
