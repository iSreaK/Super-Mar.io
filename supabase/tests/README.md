# Tests SQL sur PC

Tests exécutés avec Postgres isolé en mémoire (PGlite 0.5.10). Aucun téléphone, projet Supabase distant ou compte réel. Le schéma auth minimal et les claims JWT sont simulés ; cela ne remplace pas la validation OAuth sur le projet configuré.

Depuis ce dossier, avec Node et npm :

    npm ci --ignore-scripts --no-audit --no-fund
    npm test

Les scripts chargent les trois migrations de production et les rejouent. `local-supabase.mjs` fournit uniquement le schéma Auth, les rôles et les fonctions Realtime de test ; aucune migration de production n'est remplacée. Chaque base est fermée à la fin, même en cas d'échec.

- `rankings.test.mjs` : 31 scénarios sur Google/UID, droits directs refusés, RLS, temps bornés, records, égalités, Top 50, neuf parcours, missions, skins, isolation et conservation. Ils s'exécutent aussi avec la migration sociale installée.
- `friends.test.mjs` : 23 scénarios sur profils/codes stables, identité serveur, ajout par code/UID, acceptation/refus/annulation/retrait, doublons, demandes croisées, commandes anciennes, quotas des deux participants, accès aux profils, triggers, canaux privés, refus d'envoi client et conservation à la réexécution.

Le service Realtime et OAuth ne sont pas démarrés par PGlite. Les tests simulent `auth.uid()`, `auth.jwt()`, `realtime.topic()` et `realtime.send()` et vérifient les policies SQL ; ils ne prouvent pas l'autorisation d'un abonnement sur le service hébergé. Les contraintes de paire et les mutations sont testées séquentiellement ; la concurrence PostgreSQL réelle sur plusieurs connexions reste à vérifier séparément. Voir [FRIENDS_SETUP.md](../../docs/FRIENDS_SETUP.md) pour les contrôles distants manuels.

Sur le PC de Julie, la dépendance de validation est dans C:/Android/Caches/MarioSprintSqlCheck, hors OneDrive. Pour la réutiliser :

    $env:PGLITE_MODULE = 'C:/Android/Caches/MarioSprintSqlCheck/package/dist/index.js'
    node rankings.test.mjs
    node friends.test.mjs

Ces tests sont indépendants du build Android et ne s'exécutent pas sur un appareil.
