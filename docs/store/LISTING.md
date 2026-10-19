# iMirror — Bozze per gli store (Google Play + Amazon Appstore)

## Nome
**iMirror — AirPlay per Fire TV & Android TV**

## Descrizione breve (max ~80 caratteri)
- IT: `Trasforma Fire TV e Android TV in un ricevitore AirPlay per iPhone, iPad e Mac.`
- EN: `Turn your Fire TV & Android TV into an AirPlay receiver for iPhone, iPad and Mac.`

## Descrizione lunga (IT)
iMirror trasforma la tua Fire TV o Android TV in un ricevitore AirPlay: iPhone, iPad e Mac
vedono la TV nel menu "Duplica schermo" e proiettano schermo e audio in un attimo.

**Niente da installare sugli altri dispositivi** — usi il menu nativo di Apple.

• Mirroring schermo fino a 1440p con decodifica hardware (fluido su Fire TV Stick 4K)
• Audio sincronizzato con coda ridotta (~120 ms)
• Interfaccia in italiano, con telecomando
• Nome dispositivo personalizzabile e PIN di abbinamento opzionale
• Leggero: gira anche su TV economiche (1 GB di RAM)

**Come si usa**: apri iMirror sulla TV, poi dal Mac/iPhone: Centro di Controllo → Duplica schermo → iMirror.

**Nota**: i contenuti protetti da DRM (Netflix, Apple TV+, Disney+) non possono essere
proiettati (protezione di sistema). App non affiliata ad Apple.

## Descrizione lunga (EN)
Turn your Fire TV or Android TV into an AirPlay receiver: your iPhone, iPad and Mac will
see the TV in the "Screen Mirroring" menu and stream screen + audio instantly.

**Nothing to install on your Apple devices** — just use the native menu.

• Screen mirroring up to 1440p with hardware decoding (smooth on Fire TV Stick 4K)
• Synchronized audio with low-latency buffer (~120 ms)
• Remote-friendly UI
• Custom device name and optional pairing PIN
• Lightweight: runs even on budget TVs (1 GB RAM)

**How to use**: open iMirror on your TV, then on your Mac/iPhone: Control Center → Screen
Mirroring → iMirror.

**Note**: DRM-protected content (Netflix, Apple TV+, Disney+) cannot be mirrored (system
protection). Not affiliated with Apple.

## Keyword (per ricerca)
airplay, fire tv, android tv, screen mirroring, duplica schermo, iphone, ipad, mac,
mirror to tv, airscreen alternative, ricevitore airplay

## Prezzo
€1,99 (acquisto una tantum, entrambi gli store)

## Asset necessari
| Asset | Specifiche | Stato |
|---|---|---|
| Icona store | 512×512 PNG | ⬜ da generare (tools/gen_banner.py estendibile) |
| Banner Fire TV | 320×180 PNG | ✅ fatto (`drawable-xhdpi/banner.png`) |
| Feature graphic Play | 1024×500 PNG | ⬜ da generare |
| Screenshot TV | 1920×1080 (3-6) | ⬜ da scattare con TV accesa (attesa + mirroring + impostazioni) |
| Privacy policy | URL pubblico | ✅ `docs/store/PRIVACY.md` (da pubblicare, es. GitHub Pages o repo) |

## Checklist account
- Google Play Console ($25 una tantum): creare app "iMirror", categoria "Video players", TV ✓
  - Upload: **AAB** (`./gradlew bundleRelease`), arm64 obbligatorio ✓
  - Data safety: nessun dato raccolto
- Amazon Developer (gratis): Fire TV app, upload **APK** firmato, banner 320×180 obbligatorio ✓
  - Test su dispositivo: ~1-2 settimane

## Note legali (già verificate)
- GPL-3.0: vendita consentita, sorgente pubblico obbligatorio ✓ (repo pubblico)
- Nessun marchio Apple nel nome/icona; disclaimer presente ✓
- FairPlay: area grigia nota (precedenti commerciali: AirScreen, AirMirror, jqssun)

## Lingue dell'app (v1.7.0)
**Italiano · Inglese · Cinese (semplificato) · Tedesco · Spagnolo**

## Descrizioni extra per la scheda store (facoltative)
### Cinese (zh)
iMirror 将你的 Fire TV 变成 AirPlay 接收器：iPhone、iPad 和 Mac 可在“屏幕镜像”中直接看到电视并投射屏幕与声音。
• 最高 1440p 硬件解码，流畅清晰
• 低延迟音频（约 120 毫秒）
• 支持遥控器操作，可自定义设备名称与配对 PIN
使用方法：在电视上打开 iMirror，然后在 Mac/iPhone：控制中心 → 屏幕镜像 → iMirror。
注意：受 DRM 保护的内容（Netflix、Apple TV+、Disney+）无法投射。本应用与 Apple 无关联。

### Tedesco (de)
Verwandle deinen Fire TV in einen AirPlay-Empfänger: iPhone, iPad und Mac sehen den
Fernseher im Menü „Bildschirm spiegeln“.
• Bis zu 1440p mit Hardware-Decodierung
• Audio mit geringer Latenz (~120 ms)
• Fernbedienungsfreundlich, Gerätename und PIN optional
Anleitung: iMirror am TV öffnen, dann am Mac/iPhone: Kontrollzentrum → Bildschirm spiegeln → iMirror.
Hinweis: DRM-geschützte Inhalte (Netflix, Apple TV+, Disney+) können nicht gespiegelt werden.
Nicht mit Apple verbunden.

### Spagnolo (es)
Convierte tu Fire TV en un receptor AirPlay: iPhone, iPad y Mac ven la TV en el menú
«Duplicar pantalla».
• Hasta 1440p con decodificación por hardware
• Audio con baja latencia (~120 ms)
• Compatible con el mando, nombre de dispositivo y PIN opcionales
Cómo usarlo: abre iMirror en la TV y, en tu Mac/iPhone: Centro de Control → Duplicar pantalla → iMirror.
Nota: el contenido protegido con DRM (Netflix, Apple TV+, Disney+) no se puede duplicar.
No está afiliado a Apple.
