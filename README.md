# iMirror — AirPlay per Android TV & Fire TV

Trasforma la tua Android TV / Fire TV in un ricevitore **AirPlay**: iPhone, iPad e Mac
vedono la TV nel menu **"Duplica schermo"** e proiettano schermo + audio — senza installare
nulla sui dispositivi Apple.

- **Target**: Android TV (API 25+) e Fire TV — testato su Akai AKTV4330 (Android 9, MStar,
  2.4GHz Wi-Fi) e Amazon Fire TV Stick 4K (Fire OS 6 / Android 7.1.2, 5GHz)
- **Sorgenti**: macOS 26.x, iOS/iPadOS
- **Versione**: v1.6.0

## Funzionalità
- 🖥️ Mirroring schermo H.264/HEVC fino a **1440p** (hardware decode via MediaCodec)
- 🔊 Audio **AAC-ELD** con **trim low-latency** (coda ~120ms: il pre-buffer di macOS viene tagliato)
- ⚡ Pipeline a bassa latenza: drop frame intelligente, watchdog keyframe (5s), Wi-Fi High-Perf lock
- 🖥️ **Keep-screen-on** durante lo streaming (niente screensaver che congela l'immagine)
- 🇮🇹 UI **in italiano** (e inglese)
- 🔔 **Self-updater**: controlla le Release di questo repo e propone l'aggiornamento
  (le installazioni da store si aggiornano da sole)
- 🔒 PIN di abbinamento opzionale, nome dispositivo configurabile, overlay di debug (FPS/code/drop)
- 📱 Supporto "video streaming" (HLS) e telecomando DACP

## Installazione (sideload)
1. Abilita il debug ADB (Fire TV: *Impostazioni → My Fire TV → Info → 7 click → Opzioni sviluppatore*)
2. Dal computer: `adb connect <IP-TV>:5555` → `adb install imirror-universal.apk`
3. Apri **iMirror** sulla TV e dal Mac/iPhone: *Centro di Controllo → Duplica schermo → iMirror*

## Aggiornamenti
- **Sideload**: l'app controlla le [Release](https://github.com/pietroMastro92/ciakmirror/releases)
  e propone il download + installazione con un tap.
- **Store** (prossimamente su Google Play / Amazon Appstore): aggiornamenti automatici.

## Compilazione
Il progetto ha **due flavor** di distribuzione:

| Flavor | Uso | Self-updater |
|---|---|---|
| `amazon` | build per **Amazon Appstore** | ❌ nessuno (aggiornamenti solo dallo store) |
| `sideload` | APK su GitHub Releases (installazione manuale) | ✅ controlla le Release GitHub |

```bash
# Firma release (keystore NON nel repo):
export KEYSTORE_PATH=.../keystore/imirror-release.jks KEYSTORE_PASSWORD=... KEY_ALIAS=imirror KEY_PASSWORD=...
./gradlew assembleAmazonRelease    # APK per lo store
./gradlew assembleSideloadRelease  # APK per GitHub (con updater)
```
Richiede JDK 17+, Android SDK con **NDK 28.2.13676358** e **CMake 3.22.1**.

## Limitazioni note
- Contenuti **DRM** (Netflix, Apple TV+, Disney+): schermo nero — protezione FairPlay di sistema,
  vale per ogni ricevitore AirPlay open source.
- Latenza del puntatore ~0.3–1s (strutturale di AirPlay; il video resta fluido).
- Solo stessa sottorete (niente AWDL). Per la fluidità massima: Ethernet o Wi-Fi 5GHz.

## Licenza e crediti
**GPL-3.0** — basato su [iMirror](https://github.com/prat3ik/iMirror) di prat3ik (GPL-3.0),
sul server [UxPlay](https://github.com/FDH2/UxPlay) (LGPL-2.1+), su
[playfair](https://github.com/EstebanKubata/playfair) (GPL) e su FFmpeg.
Vedi [NOTICE.md](NOTICE.md). Apple, AirPlay, iPhone, iPad, Mac e macOS sono marchi di Apple Inc.:
questo progetto non è affiliato né approvato da Apple.
