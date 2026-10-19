#!/usr/bin/env python3
"""
gen_ciak_icons.py — Genera le icone PNG di CiakMirror (tutte le densità mipmap)
disegnando lo stesso design del vettoriale ciak_icon.xml:
  squircle scuro + triangolo play bianco + onde "cast".

Serve perché su Android 7 (Fire OS 6 / Fire TV Stick 4K) le icone vettoriali
del launcher non sono affidabili: le PNG funzionano ovunque.

Uso:  python3 tools/gen_ciak_icons.py [res_dir]
Default res_dir: ../mirror-src/app/src/main/res  (relativo a questo file)
"""
import os
import struct
import sys
import zlib
import math

# ── Design (coordinate 108x108, come il vettoriale) ──────────────────────────
BASE = (0x0E, 0x0E, 0x13, 255)      # sfondo squircle
BORDER = (0x2E, 0x2F, 0x38, 255)    # bordo
PLAY = (0xFF, 0xFF, 0xFF, 255)      # triangolo play
WAVE = (0xA9, 0xB7, 0xC6, 255)      # onde
CLEAR = (0, 0, 0, 0)

CENTER = 54.0
HALF = 50.0          # 4..104
RADIUS = 16.0        # raggio squircle
TRI = ((43.0, 33.0), (78.0, 54.0), (43.0, 75.0))
WAVES_CY = 88.0
WAVES = ((24.0, 1.5), (16.0, 1.5))  # (raggio, semi-spessore)

SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
SS = 4  # supersampling


def rounded_rect_sdf(px, py):
    qx = abs(px - CENTER) - (HALF - RADIUS)
    qy = abs(py - CENTER) - (HALF - RADIUS)
    ax = qx if qx > 0 else 0.0
    ay = qy if qy > 0 else 0.0
    return math.sqrt(ax * ax + ay * ay) + min(max(qx, qy), 0.0) - RADIUS


def in_triangle(px, py):
    (ax, ay), (bx, by), (cx, cy) = TRI
    d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by)
    d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy)
    d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay)
    neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
    pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
    return not (neg and pos)


def on_wave(px, py):
    if py > WAVES_CY:
        return False
    dx = px - CENTER
    dy = py - WAVES_CY
    dist = math.sqrt(dx * dx + dy * dy)
    for r, hw in WAVES:
        if abs(dist - r) <= hw:
            return True
    return False


def sample(px, py):
    d = rounded_rect_sdf(px, py)
    if d >= 0.0:
        return CLEAR
    if in_triangle(px, py):
        return PLAY
    if on_wave(px, py):
        return WAVE
    if -1.0 <= d <= 1.0:
        return BORDER
    return BASE


def render(size):
    step = 108.0 / size
    sub = step / SS
    rows = []
    for y in range(size):
        row = bytearray()
        for x in range(size):
            r = g = b = a = 0
            for sy in range(SS):
                py = y * step + (sy + 0.5) * sub
                for sx in range(SS):
                    px = x * step + (sx + 0.5) * sub
                    cr, cg, cb, ca = sample(px, py)
                    r += cr
                    g += cg
                    b += cb
                    a += ca
            n = SS * SS
            row += bytes((r // n, g // n, b // n, a // n))
        rows.append(bytes(row))
    return rows


def write_png(path, size, rows):
    def chunk(typ, data):
        c = struct.pack(">I", len(data)) + typ + data
        c += struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)
        return c

    raw = b"".join(b"\x00" + row for row in rows)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    res = sys.argv[1] if len(sys.argv) > 1 else os.path.join(here, "..", "app-src", "app", "src", "main", "res")
    res = os.path.abspath(res)
    print("res dir:", res)
    for bucket, size in SIZES.items():
        out_dir = os.path.join(res, "mipmap-" + bucket)
        os.makedirs(out_dir, exist_ok=True)
        rows = render(size)
        path = os.path.join(out_dir, "ic_launcher.png")
        write_png(path, size, rows)
        print(f"  {bucket:9s} {size:3d}x{size} -> {path}")
    print("Fatto.")


if __name__ == "__main__":
    main()
