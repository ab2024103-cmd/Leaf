#!/usr/bin/env python3
"""Generate Leaf's legacy launcher PNGs (API 23-25) with stdlib only.

The adaptive icon (API 26+) uses the exact mockup leaf paths as a vector;
these PNGs approximate the same mark geometrically: brand-seed green with two
white blades. Run from the repo root:  python3 tools/gen_launcher_icon.py
"""
import math
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GREEN = (0x2B, 0x7A, 0x5B, 255)
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def write_png(path, size, pixels):
    # 4 bytes per pixel (RGBA); the row slice must account for that.
    row = size * 4
    raw = b"".join(b"\x00" + bytes(pixels[y * row:(y + 1) * row]) for y in range(size))

    def chunk(ctype, data):
        out = struct.pack(">I", len(data)) + ctype + data
        return out + struct.pack(">I", zlib.crc32(ctype + data) & 0xFFFFFFFF)

    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )
    with open(path, "wb") as f:
        f.write(png)


def blade(dist2, rx, ry, alpha):
    """Alpha (0..1) of a rotated ellipse; dist2 is the normalised distance²."""
    if dist2 >= 1.0:
        return 0.0
    edge = min(1.0, (1.0 - dist2) * 6.0)  # cheap antialiasing
    return alpha * edge


def render(size):
    px = []
    r = size * 0.16  # corner radius
    blades = [
        # (cx, cy, rx, ry, rotation_deg, alpha)
        (0.53, 0.44, 0.135, 0.30, -24.0, 0.85),
        (0.43, 0.50, 0.115, 0.26, 28.0, 0.50),
    ]
    pre = []
    for cx, cy, rx, ry, rot, alpha in blades:
        t = math.radians(rot)
        pre.append((cx * size, cy * size, rx * size, ry * size, math.cos(t), math.sin(t), alpha))
    for y in range(size):
        for x in range(size):
            # Rounded-rect mask.
            dx = min(x, size - 1 - x)
            dy = min(y, size - 1 - y)
            if dx < r and dy < r and (dx - r) ** 2 + (dy - r) ** 2 > r * r:
                px += [0, 0, 0, 0]
                continue
            cr, cg, cb = GREEN[0], GREEN[1], GREEN[2]
            for cx, cy, rx, ry, cos_t, sin_t, alpha in pre:
                ex, ey = x + 0.5 - cx, y + 0.5 - cy
                lx, ly = ex * cos_t + ey * sin_t, -ex * sin_t + ey * cos_t
                a = blade((lx / rx) ** 2 + (ly / ry) ** 2, rx, ry, alpha)
                cr += (255 - cr) * a
                cg += (255 - cg) * a
                cb += (255 - cb) * a
            px += [int(cr), int(cg), int(cb), 255]
    return px


for name, size in DENSITIES.items():
    d = os.path.join(ROOT, "app", "src", "main", "res", "mipmap-" + name)
    os.makedirs(d, exist_ok=True)
    write_png(os.path.join(d, "ic_launcher.png"), size, render(size))
    print("wrote", name, size)
