# Configuration des notifications push (Firebase Cloud Messaging)

Les rappels de "quart de tour" (et les futures alertes d'apogée) sont envoyés
par notification push via Firebase Cloud Messaging (FCM), le service gratuit
de Google. Comme Firebase lui-même n'est pas auto-hébergeable, il faut créer
un projet Firebase (gratuit) et brancher deux éléments : l'app Android, et le
backend sur Coolify.

Cette configuration est à faire une seule fois.

## 1. Créer le projet Firebase

1. Va sur https://console.firebase.google.com et connecte-toi avec un compte
   Google.
2. Clique sur **Ajouter un projet**, donne-lui un nom (ex. `vin-cave`), et
   suis les étapes (tu peux désactiver Google Analytics, pas nécessaire ici).

## 2. Ajouter l'app Android au projet

1. Dans la console Firebase, sur la page d'accueil du projet, clique sur
   l'icône Android pour ajouter une app.
2. **Nom du package Android** : `com.xothiques.vin` (important, doit être
   exact).
3. Les champs surnom/certificat SHA-1 sont optionnels, tu peux les laisser
   vides.
4. Télécharge le fichier `google-services.json` proposé au téléchargement.
5. ⚠️ Ne remplace **jamais** le fichier directement dans le dépôt git
   (`android/app/google-services.json` n'y existe plus, seul un modèle
   `android/app/google-services.json.example` est versionné — voir
   pourquoi juste en dessous). Place plutôt ton vrai fichier téléchargé sous
   le nom `google-services-real.json`, **à côté de `build14.bat`** (donc en
   dehors du dossier `android`). `build14.bat` le copie désormais
   automatiquement vers `android\app\google-services.json` avant chaque
   build.
6. Reconstruis l'app avec `build14.bat` comme d'habitude.

⚠️ **Important — ne jamais commiter ce fichier.** `google-services.json`
n'est pas un secret critique (il est de toute façon embarqué dans chaque
APK), mais ce n'est pas non plus une bonne pratique de le versionner
publiquement, et surtout : le dépôt de ce projet est un **repo GitHub
public**, utilisé tel quel comme source de déploiement Coolify. Le fichier
`google-services-real.json` et l'ancien `android/app/google-services.json`
(remplacé par le vrai fichier) ont déjà été poussés par erreur une fois —
c'est sans gravité (pas de clé admin exposée), mais le `.gitignore` du
projet bloque maintenant ces deux noms de fichier pour que ça ne se
reproduise pas. Si jamais `git status` te montre l'un de ces fichiers comme
prêt à être commité, ne fais pas `git add -A` dessus.

Sans l'étape 5, l'app compile quand même (le modèle `.example` sert de
gabarit) mais aucun token ne sera généré et aucune notification ne pourra
être reçue.

## 3. Générer la clé de service pour le backend

Le backend a besoin d'une clé de compte de service pour pouvoir envoyer des
notifications au nom du projet Firebase.

1. Dans la console Firebase, va dans **Paramètres du projet** (icône
   d'engrenage) → onglet **Comptes de service**.
2. Clique sur **Générer une nouvelle clé privée**, confirme.
3. Un fichier JSON est téléchargé (ex. `vin-cave-firebase-adminsdk-xxxxx.json`).
   Garde-le précieusement, il donne un accès complet au projet Firebase --
   ne le partage pas, ne le commite pas dans le dépôt.

## 4. Configurer le backend sur Coolify

⚠️ **Important : encoder la clé en base64 avant de la coller dans Coolify.**
Coolify construit l'image en injectant chaque variable d'environnement du
service directement dans le `Dockerfile` sous forme d'une ligne `ARG NOM=VALEUR`
brute, non protégée par des guillemets. Le JSON de cette clé contient des
guillemets, des accolades et des retours à la ligne (dans `private_key`), ce
qui casse cette ligne et fait échouer le build avec une erreur du style
`failed to solve: ... unexpected end of statement while looking for matching
double-quote`. Le backend (depuis cette version) accepte la clé encodée en
base64 pour éviter complètement ce problème -- l'encodage ne change rien à la
clé elle-même, juste sa représentation en texte.

1. Ouvre le fichier JSON téléchargé à l'étape précédente.
2. Encode-le en base64, sur une seule ligne. Le plus simple sans rien
   installer : ouvre les outils de développement de n'importe quel
   navigateur (touche F12), onglet **Console**, et tape (tout reste en
   local, rien n'est envoyé nulle part) :
   ```js
   btoa(JSON.stringify(/* colle ici tout le contenu du fichier JSON */))
   ```
   Appuie sur Entrée : la console affiche la chaîne encodée entre guillemets
   -- c'est elle qu'il faut copier (sans les guillemets qui l'entourent).
   (Alternative en ligne de commande si tu préfères : `base64 -w0
   fichier.json` sous Linux, `base64 -i fichier.json | tr -d '\n'` sous
   macOS.)
3. Dans Coolify, ouvre le service backend du projet Vin → onglet
   **Environment Variables**.
4. Ajoute une nouvelle variable :
   - Nom : `FIREBASE_SERVICE_ACCOUNT_JSON`
   - Valeur : la chaîne base64 obtenue à l'étape 2 (une seule ligne, sans
     guillemets ni retour à la ligne).
5. Sauvegarde, puis relance un déploiement du service (redeploy) pour que la
   variable soit prise en compte.

Sans cette variable, le backend démarre normalement mais n'enverra aucune
notification (aucune erreur, juste un message dans les logs au premier envoi
tenté).

## 5. Déployer la migration de base de données

Cette fonctionnalité ajoute de nouvelles colonnes/tables (`device_tokens`,
`last_turned_at`, `last_turn_reminder_sent_at`). Comme pour toute mise à
jour du backend, les migrations Drizzle s'appliquent automatiquement au
démarrage -- rien de spécial à faire au-delà du redeploy habituel.

## 6. Vérifier que ça fonctionne

1. Ouvre l'app sur ton téléphone (avec le nouveau `google-services.json`),
   connecte-toi. L'app demande la permission d'envoyer des notifications
   (Android 13+) -- accepte-la.
2. Le backend enregistre normalement le token de ton téléphone au démarrage
   de l'app.
3. **Le plus rapide pour vérifier que tout est branché, sans attendre le
   cron du lendemain** : Réglages → carte "Notifications" → bouton "Envoyer
   une notification de test". La réponse te dit précisément où ça coince le
   cas échéant :
   - *"Le serveur n'a pas encore de Firebase configuré"* → revoir l'étape 4
     (variable `FIREBASE_SERVICE_ACCOUNT_JSON`), puis redeployer.
   - *"Aucun appareil enregistré pour ce foyer"* → reconnecte-toi dans
     l'app (avec le vrai `google-services.json` installé) pour qu'un token
     soit envoyé au serveur.
   - *"Notification envoyée à N/N appareil(s)"* → tout fonctionne, la
     notification devrait apparaître sur ton téléphone dans la foulée.
4. Le rappel de quart de tour se déclenche automatiquement chaque jour à
   9h (heure du serveur) pour toute bouteille en cave non tournée depuis 90
   jours ou plus, avec un rappel maximum tous les 7 jours par bouteille
   pour éviter le spam.
5. Tu peux aussi voir directement dans l'app, sur la fiche d'une bouteille,
   depuis quand elle n'a pas été tournée, avec un bouton "Tournée
   aujourd'hui" pour réinitialiser le compteur -- et un petit repère dans la
   liste des bouteilles pour repérer d'un coup d'œil celles qui ont besoin
   d'un tour.

## En cas de souci

- **Pas de notification reçue mais tout semble configuré** : vérifie dans
  les logs du backend Coolify qu'il n'y a pas d'erreur au démarrage liée à
  `FIREBASE_SERVICE_ACCOUNT_JSON` (JSON mal collé/invalide est la cause la
  plus fréquente).
- **L'app ne demande jamais la permission de notification** : vérifie que
  tu es bien sur Android 13 ou plus récent pour ce test (en dessous, la
  permission est accordée automatiquement par le système).
- **Tu veux changer de projet Firebase plus tard** : remplace simplement le
  `google-services.json` et la variable d'environnement par les nouveaux,
  puis reconstruis/redeploy -- les anciens tokens enregistrés en base
  redeviendront simplement invalides et seront nettoyés automatiquement par
  le backend au prochain envoi raté.
