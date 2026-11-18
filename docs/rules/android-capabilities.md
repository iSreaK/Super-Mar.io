# Capacités Android et fonctionnalités conditionnelles

## Périmètre

À lire pour le Manifest, les permissions, intégrations Android, images par URL, futures notifications/configurations distantes et publication. Lire seulement les sections correspondant à la fonctionnalité demandée ; ces chapitres n'autorisent pas leur ajout automatique.

## Références du cours

- Pages PDF 9-10 et 61-66 : ressources, permissions et cycle de vie.
- Pages 110-115 et 261-262 : images par URL et chargement avec Coil.
- Pages 144-153 : bundle signé, tests de distribution et suivi de publication.
- Pages 291-309 : notifications, permission et canal.
- Pages 310-329 : valeurs par défaut, récupération/activation et fréquence de configuration distante.

Le support utilise Firebase pour ses démonstrations cloud. Le choix utilisateur reste Supabase ; aucun de ces exemples ne demande de réinstaller Firebase ou de publier une campagne.

## Règles

### Permissions et intégration

- Demander uniquement les permissions utiles à une fonctionnalité réelle. Distinguer les permissions déclarées dans le Manifest et celles qui demandent aussi une autorisation pendant l'usage.
- Actuellement, l'application utilise INTERNET. Ne pas ajouter caméra, stockage, localisation ou autre permission pour faciliter un diagnostic ou un test.
- Pour une future permission à l'exécution, expliquer son utilité au moment approprié, gérer le refus et garder un état d'interface cohérent.
- Déclarer les composants Android nécessaires avec les attributs d'export et intents adaptés ; ne pas exposer un service interne sans besoin.
- Conserver le cycle de vie et le retour existants lors d'une intégration. Ne pas bloquer Main ni laisser une ressource active hors visibilité.
- Une implémentation qui demande une permission à l'utilisateur de l'app n'autorise pas l'agent à modifier les réglages du téléphone ou à lancer l'app pour essayer.

### Images distantes, si demandées

- Employer la solution de chargement/cache adaptée, telle que Coil selon le cours, avec taille utile, placeholder et erreur explicites.
- Éviter les téléchargements répétés dans une liste et les décodages sur Main. Conserver l'affichage local utile en cas d'absence de réseau.
- Les sprites et portraits du jeu sont déjà locaux/préchargés : ne pas ajouter une dépendance d'images réseau pour les afficher.

### Notifications, si demandées

- Prévoir la permission POST_NOTIFICATIONS à l'exécution pour Android 13+, avec état de refus. Ne pas la demander au démarrage sans contexte fonctionnel.
- Prévoir les canaux sur les versions qui les requièrent, le comportement au premier plan/en arrière-plan et une action de navigation explicite.
- Respecter le principe de service interne illustré par le cours et le fournisseur effectivement choisi pour la fonctionnalité ; Firebase Messaging n'est pas un prérequis automatique du jeu.
- Éviter un polling d'arrière-plan destiné à simuler un service de notifications. L'envoi d'un message à un appareil réel nécessite une demande couvrant cette action.

### Configuration distante, si demandée

- Fournir des valeurs locales de repli pour le premier démarrage et le mode hors ligne. Distinguer récupération des valeurs et activation selon le contrat retenu.
- Contrôler la fréquence et traiter l'échec sans bloquer le jeu. Un intervalle de démonstration de 60 s n'est pas une valeur à imposer par défaut en production.
- Passer par une source/repository et un State observé, sans gestionnaire distant appelé depuis le Screen.
- Ne pas modifier à distance, sous un identifiant classé inchangé, la physique, les bonus ou les conditions qui déterminent le chrono ; appliquer le versionnement des [règles du jeu](game-and-performance.md).

### Distribution, si demandée

- Préparer le type d'artefact et sa signature selon la cible : mise à jour locale du projet ou distribution en boutique. Ne pas exposer clé de signature ou mot de passe dans Git.
- Préserver package, compatibilité et données lors d'une mise à jour ; un changement de signature ne se résout pas en désinstallant le jeu de l'utilisateur.
- Une publication en boutique est une action distincte d'un build, d'un push Git ou d'une installation locale. Le projet est destiné à un usage personnel local par défaut.
- Le suivi de crash/ANR montré dans le cours n'autorise pas l'ajout de télémétrie, Analytics ou collecte de données sans besoin demandé.

## Adaptations MarioSprint

- Supabase, configuration locale facultative et jeu hors connexion restent les choix actuels. Aucun service Firebase, notification ou configuration distante n'est à ajouter pour cocher un chapitre du cours.
- Les tests et diagnostics sur téléphone respectent toujours les limites d'[AGENTS.md](../../AGENTS.md#téléphone), même si un exemple de slide les inclut dans sa procédure.
- La charte fixe du jeu reste conservée ; la démonstration de couleurs dynamiques du support n'impose pas de changer la palette de l'application.

## Exemples et pièges

- Le [Manifest](../../app/src/main/AndroidManifest.xml) décrit les permissions/composants réellement présents. Vérifier ce fichier et le build fusionné quand une capacité évolue.
- L'exemple de notification du cours demande parfois d'activer un réglage à la main : il ne donne aucune permission à l'agent pour le faire sur le Redmi.
- Les étapes « Publier » d'une console sont des illustrations ; elles ne deviennent pas des actions à exécuter lors de la lecture du support.

## Vérifications

- Pour une fonctionnalité demandée : compatibilité minSdk/targetSdk, cas de refus, absence de réseau, comportement lifecycle et sauvegardes préservées.
- Compiler les chemins concernés et tester localement les contrats isolables ; indiquer ce qui nécessite encore un essai réel autorisé.
- Suivre [Kotlin et build](kotlin-and-build.md) pour les dépendances et [Tests et validation](testing-and-validation.md) pour les preuves de livraison.
