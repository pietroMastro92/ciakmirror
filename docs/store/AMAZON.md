# Amazon Appstore — guida al submission (Fire TV)

## 0. Account (gratis, 2 minuti)
1. Vai su **https://developer.amazon.com** e accedi con il **tuo account Amazon** (quello Prime va benissimo: Prime ≠ Developer, ma le credenziali sono le stesse!)
2. "Create your Amazon Developer account" → dati + accettazione termini
3. Per le app **a pagamento** (€1,99): completa il **profilo fiscale** (Tax Interview / W-8BEN) e le coordinate per i pagamenti — serve per ricevere i ricavi (Amazon trattiene ~30%)

## 1. Nuova app (console)
- Console: **https://developer.amazon.com/apps-and-games/console**
- "Add New App" → piattaforma **Android** → nome **iMirror** → contatto = tu
- Devices: spunta **Fire TV** (il nostro APK copre tutti i modelli)

## 2. Upload APK
- File consigliato: `app-armeabi-v7a-release.apk` (6MB — tutti i Fire TV Stick)
  in alternativa `app-universal-release.apk` (11.5MB — tutti gli ABI)
- Entrambi disponibili nella Release: https://github.com/pietroMastro92/ciakmirror/releases
- ⚠️ È firmato con la NOSTRA chiave (`keystore/imirror-release.jks`) — conservala!

## 3. Scheda (Listing)
- Titolo: **iMirror — AirPlay per Fire TV**
- Descrizioni: vedi `LISTING.md` (IT + EN)
- Keyword: airplay, fire tv, screen mirroring, duplica schermo, iphone, mac
- Categoria: la più vicina tra "Video" / "Entertainment"
- Prezzo: **€1,99** una tantum
- Content rating: completa il questionario (nessun contenuto pericoloso — app utility)

## 4. Asset (tutti pronti nel repo)
| Asset | File | Note |
|---|---|---|
| Icona 512×512 | `docs/store/icon-512.png` | ✅ |
| Banner 320×180 | `docs/store/banner-320x180.png` | ✅ (obbligatorio per Fire TV) |
| Screenshot 1920×1080 | `docs/store/screenshots/` | ✅ 2 pronte (attesa + impostazioni), 1 in arrivo (mirroring) |
| Privacy policy URL | `https://pietromastro92.github.io/ciakmirror/store/PRIVACY.html` | ✅ (GitHub Pages) |

## 5. Review
- Amazon testa su dispositivi reali: tipicamente **1-2 settimane**
- Se chiedono chiarimenti su DRM/marchi: abbiamo i disclaimer pronti (vedi `LISTING.md`)

## Note finali
- Marchio: mai "AirPlay"/Apple nel titolo; nelle descrizioni è uso descrittivo ✓
- GPL-3.0: sorgente pubblico già disponibile ✓
- Aggiornamenti futuri: si carica un nuovo APK con `versionCode` più alto (7 → 8…) e stessa firma
