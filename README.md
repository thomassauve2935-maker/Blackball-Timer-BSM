# Blackball Timer — Android

Application Android native, sans publicité, sans compte, sans connexion Internet.

## Fonctions
- Deux joueurs, noms et temps par coup personnalisables (45 et 60 s par défaut).
- Premier coup après la casse : 90 s par défaut. Appliqué une seule fois, au premier lancement du chrono après « Nouvelle partie ».
- Bouton « Coup suivant » qui recharge le temps du joueur actif.
- Bouton « Changer de joueur » qui sélectionne son temps propre.
- « Extension » ajoute une durée personnalisable au temps restant (30 s par défaut, **choix technique, pas affirmation du règlement FFB**).
- Nombre d'Extensions par joueur et par partie personnalisable (1 par défaut), et compteurs indépendants.
- Pause/reprise, alerte sonore, vibration, écran maintenu allumé, paramètres sauvegardés.

## Installation / compilation
1. Installer Android Studio (version récente disposant du SDK Android API 35 et de JDK 17).
2. Ouvrir le dossier `BlackballTimer` avec « Open ».
3. Laisser Android Studio synchroniser Gradle (AGP 8.7.3 / Gradle 8.9).
4. Menu **Build > Build APK(s)**. APK généré : `app/build/outputs/apk/debug/app-debug.apk`.
5. Copier l'APK sur le téléphone et autoriser l'installation depuis cette source.

Le projet ne contient pas de binaire APK précompilé : l'environnement de création ne dispose pas du SDK Android ni de Gradle.

## Point d'ergonomie
Le passage au coup suivant / joueur suivant met le chrono en pause jusqu'à « Démarrer ». L'Extension peut être prise pendant la marche ou en pause, mais pas après expiration.

## Compilation sur GitHub sans installer Android Studio
Le projet contient `.github/workflows/android-apk.yml` : une action GitHub qui compile un APK de test et le fournit en artefact téléchargeable.
1. Créer un dépôt GitHub privé et y déposer **le contenu** du dossier `BlackballTimer` (pas le fichier ZIP lui-même).
2. Dans l'onglet **Actions**, sélectionner « Construire APK Android », puis **Run workflow**.
3. Lorsque la tâche est terminée, ouvrir l'exécution et télécharger l'artefact `BlackballTimer-APK`.
4. Décompresser cet artefact pour récupérer `app-debug.apk`, puis le copier sur son téléphone.

**Attention :** la compilation GitHub n'a pas été exécutée ni validée ici. Le dossier contient les sources Gradle, mais pas le JAR du Gradle Wrapper. Le workflow GitHub installe directement Gradle 8.9. Sur PC, si Android Studio réclame le wrapper manquant, choisir une distribution Gradle installée (8.9), puis faire la synchronisation.
