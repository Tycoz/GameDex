# GameDex

**GameDex** est une application Android de gestion de collection de jeux vidéo physiques inspirée de *Discogs*. Elle permet de cataloguer vos jeux possédés et votre wishlist, d'évaluer l'état physique de chaque exemplaire, de suivre la valeur marchande de votre ludothèque et d'être alerté des bonnes affaires.

---

## Fonctionnalités principales

### 1. Gestion de la Collection (onglet *Collection*)
- **Catalogue personnel synchronisé dans le cloud** : affichage des jeux possédés avec jaquette, plateforme, région, complétude, année, note et cote estimée.
- **Recherche, tri et filtrage avancés** :
  - Barre de recherche instantanée (titre, console, éditeur, code-barres).
  - Tri par titre (`A-Z`), valeur estimée (`↓`), note (`↓`), année de sortie (`↓`) ou derniers ajouts.
  - Filtres par console (raccourcis populaires + sélecteur complet multi-générations couvrant de la 1re à la 9e génération) et par complétude (`CIB`, `Loose`, `Boîte sans notice`, `Neuf scellé`).
- **En-tête rétractable au défilement** : lorsque vous faites défiler la liste vers le bas, les lignes de filtres se replient pour ne conserver que la barre de recherche ; elles réapparaissent automatiquement dès que vous remontez.
- **Sélection multiple & changement de console en lot** : un appui long sur n'importe quelle carte active le mode sélection multiple afin de sélectionner plusieurs jeux (ou tout sélectionner/désélectionner) et modifier leur console cible en une seule opération.

### 2. Wishlist & Alertes de prix (onglet *Wishlist*)
- **Suivi des jeux recherchés** avec calcul du budget total estimé et filtres par plateforme (en-tête également rétractable au défilement).
- **Surveillance Vinted & eBay** :
  - Configuration des préférences et domaines Vinted (`vinted.fr`, `vinted.be`, etc.) et eBay (`ebay.fr`, `ebay.com`, etc., option *Achat Immédiat*).
  - Définition d'un **prix d'achat maximum souhaité** par jeu.
  - **Vérification automatique quotidienne** en arrière-plan (`WorkManager`) ou scan manuel à la demande, avec bannières d'offres détectées et notifications push contenant un lien direct vers l'annonce.

### 3. Statistiques & Valorisation (onglet *Stats*)
- **Tableau de bord financier** : valeur marchande estimée totale, total investi, plus-value estimée et rendement en pourcentage.
- **Actualisation des cotes via PriceCharting** :
  - Mise à jour manuelle de toute la collection (ou par jeu depuis sa fiche) selon la complétude (`Loose`, `CIB`, `Neuf/Scellé`, `Gradé`).
  - Mise à jour automatique mensuelle en arrière-plan avec historique de la dernière passe et notification récapitulative.
- **Analyses visuelles** : compteurs Collection vs Wishlist, répartition par console/plateforme, répartition par complétude physique et **Top 5** des pièces les plus cotées.

### 4. Fiche descriptive & Formulaire d'ajout / édition
- **Fiche descriptive du jeu** :
  - Affichage complet des métadonnées (titre, console, éditeur, année, région, format, note en étoiles, code-barres, date d'achat, notes).
  - **Gradation physique** : état de la boîte, de la notice et du support (cartouche/disque).
  - **Actualisation unitaire PriceCharting** et **enrichissement automatique** des informations/jaquette.
  - Pour les jeux en wishlist : panneau de surveillance Vinted/eBay et bouton **« Transférer dans ma Collection »** lors de l'acquisition.
- **Formulaire complet** : création et modification de jeux avec sélecteur de consoles multi-générations, notation, gradation, alerte de prix et gestion de la jaquette.

### 5. Scanner photo & Éditeur de jaquette (Recadrage / Réorientation)
- **Scan par photo ou galerie** : capture via l'appareil photo ou sélection sans permission large via le sélecteur photo Android (`PickVisualMedia`), avec identification assistée et préremplissage des champs.
- **Éditeur interactif de jaquette** (accessible après capture/scan ou depuis le formulaire via *Photo* / *Ajuster*) :
  - **Réorientation** : rotation `-90°`, `+90°` et effet **miroir horizontal**.
  - **Recadrage** : auto-recadrage intelligent des bordures, préréglages de ratio (`Libre`, `3:4` Switch/PS/Xbox, `4:3` SNES/GBA rétro, `1:1` Game Boy/PS1 CD) et ajustement fin des marges (côtés et haut/bas).
  - Stockage local sécurisé des jaquettes recadrées dans le stockage privé de l'application.

### 6. Scraper, Import & Export multi-formats
- **Scraper intégré** : recherche par nom de jeu et console optionnelle pour récupérer automatiquement jaquette, éditeur, année, genre, description et cotes PriceCharting (`Loose`, `CIB`, `Neuf`).
- **Import universel** :
  - Fichiers pris en charge : `.csv`, `.tsv`, `.xlsx`, `.xls`, `.ods`, `.odt`, `.vcs` / `.ics` / `.vcf`.
  - Import direct via un **lien Google Sheets partagé** ou par **collage de texte CSV/TSV**.
  - Détection automatique du séparateur, correspondance souple des en-têtes (FR/EN) et aperçu avant import en lot dans Firestore.
- **Export CSV structuré** :
  - Encodage UTF-8 avec BOM (compatible accents français sous Excel, Google Sheets et Numbers) et choix du séparateur (`,` ou `;`).
  - Filtrage par périmètre (*Jeux possédés*, *Wishlist*, *Tous les jeux*) et par console.
  - Enregistrement local sur l'appareil (Storage Access Framework), envoi vers Google Drive, partage système ou copie dans le presse-papiers.

---

## Architecture technique

- **Langage & UI** : Kotlin, Jetpack Compose, Material Design 3 (affichage edge-to-edge).
- **Architecture** : MVVM (`GameDexViewModel` avec `StateFlow` / `collectAsStateWithLifecycle` et états scellés `UiState`).
- **Authentification** : Google Sign-In via Jetpack `CredentialManager` et Firebase Authentication.
- **Base de données cloud** : Firebase Firestore avec synchronisation temps réel et isolation stricte par utilisateur (`users/{uid}/games`) contrôlée par les règles de sécurité (`firestore.rules`).
- **Réseau & médias** : OkHttp (scraping et requêtes API), Coil (chargement et mise en cache mémoire/disque des jaquettes via `ImageLoaderFactory`), `FileProvider` pour le partage de fichiers CSV.
- **Traitements en arrière-plan** : `WorkManager` (`WishlistPriceCheckerWorker` quotidien et `MonthlyPriceUpdateWorker` mensuel) avec canaux de notifications dédiés.

### Structure principale du projet

- `app/src/main/java/com/example/MainActivity.kt` & `GameDexApplication.kt` : point d'entrée, gestion de l'état d'authentification et configuration globale du chargeur d'images.
- `app/src/main/java/com/example/ui/` :
  - `GameDexApp.kt` & `GameDexViewModel.kt` : navigation principale par onglets, coordination des modales et logique de filtrage/tri/batch.
  - `tabs/` : `CollectionTab.kt`, `WishlistTab.kt`, `StatsTab.kt`.
  - `components/` : fiches, formulaires, éditeur de jaquette (`CoverPhotoEditorDialog.kt`), scanner photo, scraper, import/export, gestion des comptes marketplace et sélecteur de consoles.
  - `auth/` : `SignInScreen.kt`, `AuthManager.kt`.
- `app/src/main/java/com/example/data/` : modèles (`GameItem`, `ConsoleCatalog`, `PriceAlertData`), dépôt Firestore (`GameRepository.kt`), stockage local des jaquettes (`GameCoverStorage.kt`) et préférences (`MarketplacePreferences.kt`, `PriceChartingPreferenceManager.kt`).
- `app/src/main/java/com/example/ai/` : reconnaissance photo (`GameImageRecognitionService.kt`), scraping jeu (`GameScraperService.kt`) et cotes (`PriceChartingScraper.kt`).
- `app/src/main/java/com/example/util/` : utilitaires de recadrage (`GameCoverCropUtil.kt`), import (`CsvImportUtil.kt`, `MultiFormatImportUtil.kt`) et export (`CsvExportUtil.kt`).
- `app/src/main/java/com/example/worker/` & `notification/` : planification périodique et notifications locales.

---

## Configuration et exécution

### Prérequis
- Android SDK (compileSdk 36, minSdk 24, targetSdk 36) et JDK 11+.
- Projet Firebase configuré (`google-services.json`, Authentification Google activée et Firestore provisionné).
- Clé API configurée via les variables d'environnement (`GEMINI_API_KEY` dans `.env` — voir `.env.example`) pour l'identification photo avancée.

### Commandes utiles

```bash
# Compiler l'application (APK debug)
gradle assembleDebug

# Lancer la suite de tests unitaires et Robolectric
gradle :app:testDebugUnitTest

# Exécuter l'analyse statique Android Lint
gradle lintDebug
```

---

## Télécharger l'APK directement depuis GitHub

Un workflow **GitHub Actions** (`.github/workflows/build-apk.yml`) compile automatiquement l'application, exécute les tests unitaires et publie le fichier **`GameDex.apk`** dans les **GitHub Releases** à chaque push sur `main`/`master`, à chaque tag `v*`, ou sur déclenchement manuel.

### Lien de téléchargement direct

Une fois le code poussé sur votre dépôt GitHub et le workflow exécuté :

- **Téléchargement direct du dernier APK** :
  ```text
  https://github.com/<VOTRE_COMPTE>/<VOTRE_DEPOT>/releases/latest/download/GameDex.apk
  ```
- **Via l'interface GitHub** : ouvrez l'onglet **Releases** à droite du dépôt et cliquez directement sur **`GameDex.apk`** (aucune archive `.zip` à décompresser).
- **Déclenchement manuel** : dans l'onglet **Actions** → **Build & Release APK** → **Run workflow**.

> **Installation sur Android** : ouvrez le fichier `GameDex.apk` téléchargé sur votre smartphone et autorisez l'installation depuis des sources inconnues si Android le demande (Android 7.0 / API 24 minimum).

### Secrets GitHub optionnels

Le workflow fonctionne immédiatement sans configuration supplémentaire pour générer `GameDex.apk` (le keystore de debug est décodé automatiquement depuis `debug.keystore.base64`). Vous pouvez toutefois ajouter ces secrets dans **Settings → Secrets and variables → Actions** :

- `GEMINI_API_KEY` : clé API injectée dans `.env` lors du build CI.
- `GOOGLE_SERVICES_JSON_BASE64` : version encodée en Base64 de `app/google-services.json` (si vous choisissez de ne pas versionner ce fichier).
- `RELEASE_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` : si tous trois sont définis, le workflow génère et publie en plus un APK signé en mode release (`GameDex-release.apk`).

