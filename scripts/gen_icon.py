"""
Generates the byte!track pixel-art "B" app icon: black background, orange chunky pixels,
crisp nearest-neighbor scaling for a true pixelated look. Produces legacy square/round
launcher PNGs at every density plus adaptive-icon foreground/background layers.
"""
from PIL import Image
import os

BLACK = (10, 10, 11, 255)
ORANGE = (255, 122, 26, 255)
GRID = 16  # 16x16 pixel-art grid

# Simple centered dot, 16x16 grid — the "maarga" mark
GLYPH = [
    "0011110",
    "0111111",
    "1111111",
    "1111111",
    "1111111",
    "0111111",
    "0011110",
]
OFFSET_X, OFFSET_Y = 5, 5

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

RES_DIR = "app/src/main/res"


def build_grid(round_mask: bool) -> Image.Image:
    img = Image.new("RGBA", (GRID, GRID), BLACK)
    px = img.load()
    for gy, row in enumerate(GLYPH):
        for gx, ch in enumerate(row):
            if ch == "1":
                px[OFFSET_X + gx, OFFSET_Y + gy] = ORANGE
    if round_mask:
        # simple pixel-art circular vignette: corners forced to black to suggest a round chip
        for y in range(GRID):
            for x in range(GRID):
                cx, cy = GRID / 2 - 0.5, GRID / 2 - 0.5
                if (x - cx) ** 2 + (y - cy) ** 2 > (GRID / 2) ** 2:
                    px[x, y] = (0, 0, 0, 0)
    return img


def export_launcher(round_mask: bool, filename: str):
    base = build_grid(round_mask)
    for folder, size in DENSITIES.items():
        out_dir = os.path.join(RES_DIR, folder)
        os.makedirs(out_dir, exist_ok=True)
        scaled = base.resize((size, size), Image.NEAREST)
        scaled.save(os.path.join(out_dir, filename))


def export_adaptive_layers():
    # Foreground: glyph only, transparent bg, on a 16x16 grid inset a bit so it isn't cropped
    fg = Image.new("RGBA", (GRID, GRID), (0, 0, 0, 0))
    px = fg.load()
    for gy, row in enumerate(GLYPH):
        for gx, ch in enumerate(row):
            if ch == "1":
                px[OFFSET_X + gx, OFFSET_Y + gy] = ORANGE
    bg = Image.new("RGBA", (GRID, GRID), BLACK)

    out_dir = "app/src/main/res/drawable"
    os.makedirs(out_dir, exist_ok=True)
    fg.resize((432, 432), Image.NEAREST).save(os.path.join(out_dir, "ic_launcher_foreground.png"))
    bg.resize((432, 432), Image.NEAREST).save(os.path.join(out_dir, "ic_launcher_background.png"))


if __name__ == "__main__":
    export_launcher(round_mask=False, filename="ic_launcher.png")
    export_launcher(round_mask=True, filename="ic_launcher_round.png")
    export_adaptive_layers()
    print("Icon export complete.")
