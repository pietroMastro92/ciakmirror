# CiakMirror

Ricevitore **AirPlay 2** per Android TV: i Mac (e iPhone/iPad) vedono la TV nel menu
"Duplica schermo" / "Screen Mirroring" — nessun software da installare sui dispositivi Apple.

- **Target**: Akai AKTV4330 — Android 9 (API 28), MStar, armeabi-v7a, ~915MB RAM, 1920x1080, Wi-Fi
- **Sorgenti test**: macOS 26/27 (primario), iOS/iPadOS
- **Stato**: Fase 0 — verifica di fattibilità sulla TV

## Architettura
- **Motore**: fork di [jqssun/android-airplay-server](https://github.com/jqssun/android-airplay-server)
  (base [UxPlay](https://github.com/FDH2/UxPlay), JNI): H.264/HEVC via MediaCodec → SurfaceView,
  audio AAC-ELD/LC/ALAC. Riserva: [iMirror](https://github.com/prat3ik/iMirror) (più leggero, H.264).
- **UI CiakMirror**: schermata d'attesa, nome device, stato live, PIN opzionale,
  cap risoluzione/audio, impostazioni col telecomando.

## Fasi
| Fase | Contenuto | Stato |
|------|-----------|-------|
| 0 | Probe TV + test APK precompilati (iMirror, jqssun) da macOS 26/27 | 🟡 in corso |
| 1 | Fork + build NDK + APK ricevente (UI grezza) | ⬜ |
| 2 | UX CiakMirror completa (schermata, stato, impostazioni, PIN, audio) | ⬜ |
| 3 | Tuning hardware (1GB RAM, fps, latenza, sessioni lunghe) | ⬜ |
| 4 | Icona, APK finale, guida Mac, licenze | ⬜ |

## Hardware TV (verificato 18/10/2026)
- `ro.product.cpu.abilist` = `armeabi-v7a,armeabi`
- Android 9, MemTotal ≈ 915 MB (disponibile ≈ 320 MB), display 1920x1080
- Rete: Wi-Fi · IP 192.168.1.56

## Limitazioni note
- Contenuti **DRM** (Netflix, Apple TV+, Disney+) → schermo nero: limite di tutti i
  ricevitori AirPlay open source (protezione FairPlay di sistema).
- Latenza tipica ~0.5–1.5 s (buffering AirPlay): ok per desktop/presentazioni/video.
- Solo **stessa sottorete** (niente AWDL).
- Audio con macOS è il percorso più delicato → toggle rapido nelle impostazioni.

## Struttura
- `apk/` — build APK di CiakMirror (quando pronta)
- `docs/` — appunti di test e guida
- (`vendor-test/` locale, non versionata: APK di terze parti per i confronti)

## Licenza
**GPL-3.0** — l'app deriva da codice GPL (UxPlay/iMirror). Vedi [NOTICE.md](NOTICE.md).
