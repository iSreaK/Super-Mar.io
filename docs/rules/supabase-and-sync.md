# Supabase, authentification et synchronisation

## Périmètre

À lire pour Auth/session, Credential Manager, DTO/Retrofit, `data/remote/`, synchronisation, cache distant, comptes et `supabase/`. Compléter avec [Room](room-and-local-data.md) si la persistance change et [Coroutines](coroutines-and-lifecycle.md) pour annulation/concurrence.

## Références du cours

- Pages PDF 237-260 : API REST, interface Retrofit suspend, DTO, mapping et erreurs.
- Pages 252-255 : la réponse réseau alimente la base observée par l'UI.
- Pages 267-268 : configurations locales hors Git et HTTPS.
- Pages 274-290 : fournisseur cloud de l'exemple pédagogique.

Le cours montre Firebase ; l'utilisateur a explicitement choisi **Supabase**. Google Auth, nonce, chiffrement, RLS, RPC et règles d'envoi ci-dessous sont les adaptations de sécurité et de cohérence du projet, pas des API Supabase décrites dans les slides.

## Règles

- Exposer des contrats métier depuis les repositories ; garder Retrofit, les détails HTTP et DTO dans les sources de données.
- Le DTO reflète les champs JSON nécessaires, avec une conversion explicite. Ne pas réutiliser le DTO comme State UI ou entité de base pour éviter le mapping.
- Utiliser le client natif existant, HTTPS, délais bornés et erreurs de réseau/HTTP distinctes des erreurs de sauvegarde. Aucun appel depuis un Screen ou une Preview.
- Enregistrer les résultats dans Room, puis les observer via le repository. Conserver le cache disponible hors connexion et signaler quand il s'agit d'un résultat enregistré.
- La configuration locale et les secrets ne sont pas versionnés. Aucun mot de passe, secret OAuth, jeton de session ou clé privilégiée dans les sources, l'APK, les erreurs ou logs HTTP.
- Une clé publique de configuration ne remplace pas les contrôles d'autorisation du serveur.
- Les données d'un appel restent rattachées au propriétaire et au parcours capturés ; un changement de session ne les réattribue pas au nouveau compte.
- Les fonctions réseau suspend respectent l'annulation et les limites de réessai ; ne pas ajouter de polling ou service en arrière-plan pour cacher une erreur à l'utilisateur.

## Adaptations MarioSprint

### Authentification et session

- Google est déclenché par Credential Manager à la frontière Android, via le coordinateur assemblé au Root. Le ViewModel ne garde ni Activity ni client Android de connexion.
- Conserver le nonce : empreinte transmise à Google, valeur brute à Supabase pour la vérification. Ne pas désactiver ce contrôle pour résoudre une configuration OAuth incorrecte.
- Garder la session chiffrée AES/GCM avec une clé Android Keystore non exportable. Une session invalide est supprimée localement correctement, même si l'annulation du demandeur intervient pendant la déconnexion.
- Renouveler à la demande, avec un seul renouvellement/réessai après 401 pour la requête. Vérifier encore l'UID après renouvellement ; ne pas utiliser la session du nouveau compte pour finir un appel de l'ancien.
- Les durées actuelles sont bornées à 15 s pour Auth et 12 s pour les opérations de classement/synchronisation ; maintenir une durée explicite et justifiée si une opération évolue.

### Autorisation et cohérence serveur

- RLS activée sur les tables, droits directs refusés aux clients et RPC avec droits d'exécution explicites. L'appelant et le fournisseur sont contrôlés côté serveur.
- Une fonction privilégiée conserve un `search_path` maîtrisé ; les accès aux objets et privilèges ne doivent pas permettre un contournement par le client.
- L'UID vient de la session ; le serveur n'accepte pas un UID, un compteur de missions ou une déclaration de déblocage comme autorité client.
- Mise à jour du meilleur temps atomique par joueur/parcours ; Top 50 borné et ordre stable en cas d'égalité. Les récompenses en ligne sont dérivées des records autorisés.
- Le chrono client peut être falsifié par un client modifié. Ne pas présenter les contrôles de bornes comme une preuve serveur du parcours réellement joué.

### Hors connexion et concurrence

- Les courses invitées restent locales et ne sont pas envoyées après une connexion. Un changement de compte en cours de course ne réattribue ni record ni récompense.
- Conserver un envoi échoué et proposer un réessai explicite. Un acquittement ne supprime pas un meilleur temps arrivé pendant l'appel.
- Après les envois, récupérer la progression du compte et sauvegarder le choix de personnage selon le contrat existant. La réponse ne doit pas écraser une sélection locale plus récente.
- Après un changement de compte, ne pas exposer ses données au nouveau propriétaire, même temporairement pendant un chargement.
- Toute modification de schéma, RPC ou catalogue déjà déployé passe par une nouvelle migration. Ne pas réécrire les migrations appliquées pour simplifier le développement local.

## Exemples et pièges

- Suivre [SUPABASE_SETUP.md](../SUPABASE_SETUP.md) pour la configuration ; ses instructions ne constituent pas une demande de modifier un compte réel automatiquement.
- [AuthorizedRequests](../../app/src/main/kotlin/fr/julie/mariosprint/data/remote/AuthorizedRequests.kt) centralise l'autorisation/réessai ; [DefaultPlayerSyncRepository](../../app/src/main/kotlin/fr/julie/mariosprint/data/repository/DefaultPlayerSyncRepository.kt) coordonne les envois et la progression.
- [SessionStore](../../app/src/main/kotlin/fr/julie/mariosprint/data/local/SessionStore.kt) est le stockage de session ; ne pas dupliquer un jeton dans un champ UI pour le rendre accessible ailleurs.
- Un test HTTP simulé qui voit un header Authorization ne valide pas la connexion Google réelle. Un refus anonyme distant ne valide pas à lui seul tous les droits authentifiés.

## Vérifications

- Tester droits/UID, fournisseur, bornes, atomicité du meilleur temps, déblocages, migrations et isolation dans les [tests Postgres locaux](../../supabase/tests/README.md).
- Vérifier les contrats HTTP avec [SupabaseTest](../../app/src/test/kotlin/fr/julie/mariosprint/data/SupabaseTest.kt), les envois avec [RecordSyncTest](../../app/src/test/kotlin/fr/julie/mariosprint/data/RecordSyncTest.kt) et les choix concurrents avec [ProgressMappingTest](../../app/src/test/kotlin/fr/julie/mariosprint/data/ProgressMappingTest.kt).
- Ne pas créer de compte ou record réel pour valider automatiquement une évolution. Distinguer les preuves selon [Tests et validation](testing-and-validation.md).
