import json, math, os
from PIL import Image

SRC_MAP = {
    ("K","W"): "klt45", ("K","B"): "kdt45",
    ("Q","W"): "qlt45", ("Q","B"): "qdt45",
    ("R","W"): "rlt45", ("R","B"): "rdt45",
    ("B","W"): "blt45", ("B","B"): "bdt45",
    ("N","W"): "nlt45", ("N","B"): "ndt45",
    ("P","W"): "plt45", ("P","B"): "pdt45",
}

CANVAS = 320   # working canvas, larger than the 256 source so rotation/scale never clips
OUT_ROOT = "../pieces_classic"

def load_base(kind, color):
    img = Image.open(f"png/Chess_{SRC_MAP[(kind,color)]}.png").convert("RGBA")
    # Crop tightly to the piece's own opaque content first: transforming (especially
    # rotating) a large mostly-transparent padded square instead of the tight shape risks
    # the padded square's own edge showing up as a faint artifact after rotation/resampling.
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
    return img

def frame(base, scale=1.0, scale_x=None, scale_y=None, angle=0.0, dy=0, dx=0, alpha=1.0):
    sx = scale if scale_x is None else scale_x
    sy = scale if scale_y is None else scale_y
    w, h = base.size
    nw, nh = max(1, round(w * sx)), max(1, round(h * sy))
    piece = base.resize((nw, nh), Image.LANCZOS)
    if angle != 0:
        piece = piece.rotate(angle, resample=Image.BICUBIC, expand=True)
    if alpha < 1.0:
        a = piece.getchannel("A").point(lambda p: int(p * alpha))
        piece.putalpha(a)
    out = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    px = (CANVAS - piece.width) // 2 + dx
    py = (CANVAS - piece.height) // 2 + dy
    out.paste(piece, (px, py), piece)
    return out

STATES = {
    "idle": {
        # Small, quick up-down bounces -- a "breathing" bob rather than a slow scale pulse.
        "frames": lambda base: [
            frame(base, dy=dy, scale=s) for dy, s in [
                (0, 1.00), (-3, 1.01), (-5, 1.02), (-3, 1.01), (0, 1.00)]
        ],
        "config": {"speed_m_per_sec": 0.0, "next_state_when_finished": "idle",
                   "frames_per_sec": 9, "is_loop": True},
    },
    "move": {
        "frames": lambda base: [
            frame(base, angle=a) for a in [-4, -2, 0, 2, 4]
        ],
        "config": {"speed_m_per_sec": 1.5, "next_state_when_finished": "long_rest",
                   "frames_per_sec": 10, "is_loop": True},
    },
    "jump": {
        # Cartoon-style anticipation -> launch -> peak -> fall -> landing squash, with a much
        # taller arc than a plain up-and-down, for a far more dramatic jump.
        "frames": lambda base: [
            frame(base, dy=4,   scale_x=1.10, scale_y=0.88),   # crouch before takeoff
            frame(base, dy=-14, scale_x=0.95, scale_y=1.08),   # launching, stretched tall
            frame(base, dy=-26, scale_x=1.00, scale_y=1.00),   # peak of the jump
            frame(base, dy=-10, scale_x=0.97, scale_y=1.05),   # falling
            frame(base, dy=0,   scale_x=1.12, scale_y=0.85),   # landing squash
        ],
        "config": {"speed_m_per_sec": 3.0, "next_state_when_finished": "short_rest",
                   "frames_per_sec": 10, "is_loop": False},
    },
    "short_rest": {
        "frames": lambda base: [
            frame(base, scale=s, alpha=0.85) for s in [0.97, 0.98, 0.99, 0.98, 0.97]
        ],
        "config": {"speed_m_per_sec": 0.0, "next_state_when_finished": "idle",
                   "frames_per_sec": 4, "is_loop": True},
    },
    "long_rest": {
        "frames": lambda base: [
            frame(base, scale=s, alpha=0.75) for s in [0.95, 0.96, 0.97, 0.96, 0.95]
        ],
        "config": {"speed_m_per_sec": 0.0, "next_state_when_finished": "idle",
                   "frames_per_sec": 3, "is_loop": True},
    },
}

for (kind, color) in SRC_MAP:
    base = load_base(kind, color)
    code = f"{kind}{color}"
    for state_name, state_def in STATES.items():
        out_dir = f"{OUT_ROOT}/{code}/states/{state_name}/sprites"
        os.makedirs(out_dir, exist_ok=True)
        frames = state_def["frames"](base)
        for i, f in enumerate(frames, start=1):
            f.save(f"{out_dir}/{i}.png")
        cfg_dir = f"{OUT_ROOT}/{code}/states/{state_name}"
        with open(f"{cfg_dir}/config.json", "w") as fh:
            json.dump({"physics": {
                            "speed_m_per_sec": state_def["config"]["speed_m_per_sec"],
                            "next_state_when_finished": state_def["config"]["next_state_when_finished"]},
                       "graphics": {
                            "frames_per_sec": state_def["config"]["frames_per_sec"],
                            "is_loop": state_def["config"]["is_loop"]}},
                      fh, indent=2)
    print(f"done {code}")

print("all pieces generated")
