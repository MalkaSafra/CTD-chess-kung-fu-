import os
from collections import deque

import numpy as np
from PIL import Image
from svglib.svglib import svg2rlg
from reportlab.graphics import renderPM

SIZE = 256
WHITE_THRESHOLD = 245  # a pixel counts as "background-like" if all RGB channels are >= this

NAMES = ["kdt45", "qdt45", "rdt45", "bdt45", "ndt45", "pdt45",
         "klt45", "qlt45", "rlt45", "blt45", "nlt45", "plt45"]


def rasterize(name):
    drawing = svg2rlg(f"svg/Chess_{name}.svg")
    scale = SIZE / max(drawing.width, drawing.height)
    drawing.width *= scale
    drawing.height *= scale
    drawing.scale(scale, scale)
    tmp = f"png/_raw_{name}.png"
    renderPM.drawToFile(drawing, tmp, fmt="PNG", bg=0xffffff)
    return tmp


def punch_background_alpha(rgb):
    # White pieces have a genuine white FILL inside their own outline (e.g. a queen's crown),
    # so a global "make every white pixel transparent" would punch holes in the piece itself.
    # Flood-filling inward from the image border only reaches the actual background.
    h, w, _ = rgb.shape
    is_bgish = np.all(rgb >= WHITE_THRESHOLD, axis=2)
    visited = np.zeros((h, w), dtype=bool)
    alpha = np.full((h, w), 255, dtype=np.uint8)

    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            if is_bgish[y, x] and not visited[y, x]:
                visited[y, x] = True
                q.append((x, y))
    for y in range(h):
        for x in (0, w - 1):
            if is_bgish[y, x] and not visited[y, x]:
                visited[y, x] = True
                q.append((x, y))

    while q:
        x, y = q.popleft()
        alpha[y, x] = 0
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < w and 0 <= ny < h and not visited[ny, nx] and is_bgish[ny, nx]:
                visited[ny, nx] = True
                q.append((nx, ny))
    return alpha


os.makedirs("png", exist_ok=True)
for name in NAMES:
    raw_path = rasterize(name)
    rgb = np.array(Image.open(raw_path).convert("RGB"))
    alpha = punch_background_alpha(rgb)
    Image.fromarray(np.dstack([rgb, alpha])).save(f"png/Chess_{name}.png")
    os.remove(raw_path)
    print(f"{name}: {int((alpha == 0).sum())} background px made transparent")
