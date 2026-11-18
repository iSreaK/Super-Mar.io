# Tests SQL sur PC

Tests exécutés avec Postgres isolé en mémoire (PGlite 0.5.10). Aucun téléphone, projet Supabase distant ou compte réel. Le schéma auth minimal et les claims JWT sont simulés ; cela ne remplace pas la validation OAuth sur le projet configuré.

Depuis ce dossier, avec Node et npm :

    npm ci --ignore-scripts --no-audit --no-fund
    npm test

Le script charge les deux migrations de production et les rejoue. Les 31 scénarios couvrent Google/UID, droits directs refusés, RLS, temps bornés, records par joueur/niveau, amélioration atomique, égalités, limite 50, les neuf parcours, les missions, le choix de skin, les récompenses verrouillées, l'isolation des comptes, la conservation à la migration et la suppression en cascade. La base est fermée à la fin, même en cas d'échec.

Sur le PC de Julie, la dépendance de validation est dans C:/Android/Caches/MarioSprintSqlCheck, hors OneDrive. Pour la réutiliser :

    $env:PGLITE_MODULE = 'C:/Android/Caches/MarioSprintSqlCheck/package/dist/index.js'
    node rankings.test.mjs

Ces tests sont indépendants du build Android et ne s'exécutent pas sur un appareil.
