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

## 28 ottobre 2026 — v1.8.2 (versionCode 11) — fix video AirPlay (Safari)
- **Bug segnalato**: AirPlay video da Safari (tasto AirPlay) → schermo nero, nessun audio.
- **Causa**: lo stack HLS del firmware Fire OS (MediaPlayer/NuPlayer) non supporta i playlists
  **fMP4/CMAF** usati dai siti moderni: errore `-1007` ("Unable to parse ID3 tag"), `readyToPlay=false`.
  Verificato anche un crash del demuxer (`PlaylistFetcher CHECK failed`) su alcuni stream TS.
- **Fix**: `AirPlayVideoPlayer` riscritto con **Media3 ExoPlayer 1.8.1**
  (`media3-exoplayer` + `media3-exoplayer-hls`):
  - HLS TS, HLS fMP4/CMAF e MP4 progressivo;
  - Surface riagganciata quando `StreamingScreen` la crea dopo il `prepare()`;
  - messaggio d'errore localizzato (IT/EN/ZH/DE/ES) invece del nero muto.
- **Test su Fire Stick AFTSS (192.168.1.150)**: fMP4 Apple OK, TS Mux OK, MP4 Sintel OK.
- APK store: `iMirror-v1.8.2-amazon-armeabi-v7a.apk` (versionCode 11, permessi invariati).

## 28 ottobre 2026 — v1.8.4 (versionCode 13) — fix definitivo video Safari
- **Sintomo**: AirPlay di un video da Safari → schermo nero e ritorno alla schermata app.
- **Cause trovate e corrette**:
  1. `MediaPlayer` di Fire OS non supporta HLS **fMP4/CMAF** (errore -1007) → sostituito con
     **Media3 ExoPlayer 1.8.1** (v1.8.2).
  2. Il parser di `POST /play` accettava solo plist **binari** o testo: aggiunto supporto a
     **plist XML**, header `Content-Location`, fallback regex sull'URL e echo della `Session` (v1.8.3).
  3. **Causa principale**: alla chiusura della connessione RTSP il receiver chiamava
     `onStreamingStopped()` e uccideva il player. Safari chiude il socket subito dopo `/play`
     → nero/ritorno all'app. Ora il **video URL sopravvive** alla chiusura del control socket
     (il player scarica da solo); il mirroring continua a fermarsi come prima (v1.8.4).
- **Test**: `/play` XML plist + chiusura immediata socket → `/playback-info` su nuova connessione
  riporta position 187s e il video è visibile; `/stop` su terza connessione ferma tutto.
- APK store: `iMirror-v1.8.4-amazon-armeabi-v7a.apk` (versionCode 13).

## 28 ottobre 2026 — v1.9.0 (versionCode 21) — release consolidata
- **Mirroring iPhone/Mac**: SETUP a due fasi (chiavi + stream), parsing numerico tollerante
  (`Number`), server RTSP **multi-connessione**. Verificato su Fire Stick: 1920x1080, 31-54 fps, 0 drop.
- **Video AirPlay**: `/play` (HLS TS/fMP4 via Media3 ExoPlayer), plist XML/binari, `selectedMediaArray`
  iOS 27, sopravvivenza del player alla chiusura del control socket.
- **Cronologia locale** ultime 20 sessioni (Home + "Vedi tutte"/"Cancella"), 5 lingue.
- **Diagnostica locale** su file (RTSP + pipeline mirror), mai trasmessa.
- **Limiti noti**: i video iOS in fullscreen protetti da HDCP restano neri per scelta di Apple;
  la nuova modalità video cifrata iOS 27 (`fp-setup2`/HAP) non è implementata.
- APK store: `iMirror-v1.9.0-amazon-armeabi-v7a.apk` (versionCode 21).

## 28 ottobre 2026 — v1.9.1 (versionCode 22)
- Fix UI: "Vedi tutte" (e quindi "Cancella cronologia") sempre visibile quando esiste almeno una sessione.
- Nessun'altra modifica funzionale rispetto a v1.9.0.
