#!/usr/bin/env python3
"""
gen_store_assets.py — Genera TUTTI gli asset mancanti per la console Amazon:
  - icon-512-transparent.png      (512×512, trasparente)
  - icon-114-transparent.png      (114×114, trasparente)
  - firetv-app-icon-1280x720.png  (Fire TV app icon, opaco)
  - firetv-background-1920x1080.png (Fire TV background, opaco)
  - promo-1024x500.png            (promo image, opaco)

Uso: tools/pyenv/bin/python tools/gen_store_assets.py [out_dir]
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

ICON = os.path.expanduser(
    "~/akai-tv/mirror-src/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.expanduser(
    "~/Desktop/iMirror-Store/assets-amazon")
BG = (14, 14, 19)          # #0E0E13
WHITE = (255, 255, 255)


def load_font(size):
    for path in (
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "/System/Library/Fonts/SFNS.ttf",
    ):
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            continue
    return ImageFont.load_default()


def centered_text(draw, text, font, cx, cy, fill):
    tb = draw.textbbox((0, 0), text, font=font)
    tw, th = tb[2] - tb[0], tb[3] - tb[1]
    draw.text((cx - tw // 2 - tb[0], cy - th // 2 - tb[1]), text, font=font, fill=fill)


def main():
    os.makedirs(OUT, exist_ok=True)
    icon = Image.open(ICON).convert("RGBA")

    # 1) Icona 512 trasparente (margine trasparente attorno)
    c = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    ic = icon.resize((430, 430), Image.LANCZOS)
    c.paste(ic, ((512 - 430) // 2, (512 - 430) // 2), ic)
    c.save(os.path.join(OUT, "icon-512-transparent.png"))
    print("icon-512-transparent.png")

    # 2) Icona 114 trasparente
    c = Image.new("RGBA", (114, 114), (0, 0, 0, 0))
    ic = icon.resize((96, 96), Image.LANCZOS)
    c.paste(ic, ((114 - 96) // 2, (114 - 96) // 2), ic)
    c.save(os.path.join(OUT, "icon-114-transparent.png"))
    print("icon-114-transparent.png")

    # 3) Fire TV app icon 1280x720 (opaco)
    img = Image.new("RGB", (1280, 720), BG)
    ic = icon.resize((400, 400), Image.LANCZOS)
    img.paste(ic, (150, (720 - 400) // 2), ic)
    d = ImageDraw.Draw(img)
    f = load_font(120)
    centered_text(d, "iMirror", f, 150 + 400 + (1280 - 550) // 2, 360, WHITE)
    img.save(os.path.join(OUT, "firetv-app-icon-1280x720.png"))
    print("firetv-app-icon-1280x720.png")

    # 4) Fire TV background 1920x1080 (opaco)
    img = Image.new("RGB", (1920, 1080), BG)
    ic = icon.resize((480, 480), Image.LANCZOS)
    img.paste(ic, ((1920 - 480) // 2, 200), ic)
    d = ImageDraw.Draw(img)
    f = load_font(96)
    centered_text(d, "iMirror", f, 960, 760, WHITE)
    img.save(os.path.join(OUT, "firetv-background-1920x1080.png"))
    print("firetv-background-1920x1080.png")

    # 5) Promo 1024x500 (opaco)
    img = Image.new("RGB", (1024, 500), BG)
    ic = icon.resize((280, 280), Image.LANCZOS)
    img.paste(ic, (90, (500 - 280) // 2), ic)
    d = ImageDraw.Draw(img)
    f = load_font(84)
    centered_text(d, "iMirror", f, 90 + 280 + (1024 - 370) // 2, 250, WHITE)
    img.save(os.path.join(OUT, "promo-1024x500.png"))
    print("promo-1024x500.png")

    print("Fatto ->", OUT)


if __name__ == "__main__":
    main()
