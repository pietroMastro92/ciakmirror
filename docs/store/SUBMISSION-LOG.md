# Storico submission Amazon Appstore — iMirror

## 22 ottobre 2026 — RESUBMISSION v1.8.1 (versionCode 10)
- **Fix Content Policy**: rimosso ogni meccanismo di self-update dalla build Amazon.
  - Nuova organizzazione a **flavor**: `sideload` (self-updater GitHub) e `amazon` (nessun updater).
  - La build `amazon` non ha: permesso `REQUEST_INSTALL_PACKAGES`, FileProvider, codice
    `UpdateChecker`, URL `api.github.com` (verificato a livello di dex: 0 occorrenze).
- APK inviato: `app-amazon-armeabi-v7a-release.apk` (versionCode 10, firma release
  SHA-256 `cbf0a264…`).
- DRM Amazon: **No** (nessun wrapper: l'APK resta quello firmato da noi).
- Nota inviata al reviewer con la spiegazione tecnica completa.

## 21 ottobre 2026 — REJECT v1.8.0 (Content Policy)
- Motivo (template): "ad network libraries / unexpected action".
- Causa reale accertata: `REQUEST_INSTALL_PACKAGES` + meccanismo di self-update
  (download APK dalle Release GitHub e avvio dell'installer di sistema).
- Verificato che l'app **non** contiene SDK pubblicitari/tracker (scan del dex: 0).

## Build & firma
- Keystore: `keystore/imirror-release.jks` (NON nel repo) — **conservare con backup!**
- Flavor:
  - `sideload` → GitHub Releases (updater attivo)
  - `amazon` → build per lo store (nessun self-update)
- Comandi:
  ```bash
  # build Amazon per lo store
  ./gradlew assembleAmazonRelease
  # build sideload (tue TV + GitHub Release)
  ./gradlew assembleSideloadRelease
  ```
