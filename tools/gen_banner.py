#!/usr/bin/env python3
"""
gen_banner.py — Genera il banner TV 320x180 per iMirror
(richiesto da Amazon Appstore per Fire TV e consigliato da Google TV).

Uso:  tools/pyenv/bin/python tools/gen_banner.py [output.png]
Default output: ../mirror-src/app/src/main/res/drawable-xhdpi/banner.png
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

W, H = 320, 180
BG = (14, 14, 19)          # #0E0E13 (stesso squircle dell'icona)
ICON = os.path.expanduser(
    "~/akai-tv/mirror-src/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.expanduser(
    "~/akai-tv/mirror-src/app/src/main/res/drawable-xhdpi/banner.png")

img = Image.new("RGB", (W, H), BG)

# Icona a sinistra
icon = Image.open(ICON).convert("RGBA").resize((132, 132), Image.LANCZOS)
img.paste(icon, (18, (H - 132) // 2), icon)

# Nome a destra
d = ImageDraw.Draw(img)
font = None
for path in (
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
    "/System/Library/Fonts/Helvetica.ttc",
    "/System/Library/Fonts/SFNS.ttf",
):
    try:
        font = ImageFont.truetype(path, 38)
        break
    except Exception:
        continue
if font is None:
    font = ImageFont.load_default()

text = "iMirror"
tb = d.textbbox((0, 0), text, font=font)
tw = tb[2] - tb[0]
th = tb[3] - tb[1]
d.text((168 + max(0, (320 - 168 - tw) // 2 - 6), (H - th) // 2 - tb[1]),
       text, font=font, fill=(255, 255, 255))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print("banner ->", OUT, img.size)
