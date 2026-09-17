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
5. Remplace le fichier placeholder du dépôt par celui-ci :
   `android/app/google-services.json`
6. Reconstruis l'app avec `build14.bat` comme d'habitude.

Sans cette étape, l'app compile quand même (le placeholder est valide
structurellement) mais aucun token ne sera généré et aucune notification ne
pourra être reçue.

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

1. Ouvre le contenu du fichier JSON téléchargé à l'étape précédente avec un
   éditeur de texte, et copie tout son contenu (c'est un objet JSON sur
   plusieurs lignes).
2. Dans Coolify, ouvre le service backend du projet Vin → onglet
   **Environment Variables**.
3. Ajoute une nouvelle variable :
   - Nom : `FIREBASE_SERVICE_ACCOUNT_JSON`
   - Valeur : colle tout le contenu du fichier JSON (en une seule ligne si
     Coolify n'accepte pas les sauts de ligne dans une valeur -- la plupart
     des interfaces gèrent très bien le JSON multi-lignes collé tel quel,
     sinon minifie-le d'abord, par exemple avec un outil en ligne "JSON
     minify").
4. Sauvegarde, puis relance un déploiement du service (redeploy) pour que la
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
3. Le rappel de quart de tour se déclenche automatiquement chaque jour à
   9h (heure du serveur) pour toute bouteille en cave non tournée depuis 90
   jours ou plus, avec un rappel maximum tous les 7 jours par bouteille
   pour éviter le spam.
4. Tu peux aussi voir directement dans l'app, sur la fiche d'une bouteille,
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
