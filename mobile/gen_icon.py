from PIL import Image, ImageDraw
from pathlib import Path


def draw_icon(size: int = 1024) -> Image.Image:
    bg = (247, 243, 236, 255)  # warm cream
    ink = (38, 37, 30, 255)  # near-black
    orange = (245, 78, 0, 255)
    rose = (207, 45, 86, 255)
    white = (255, 255, 255, 255)

    img = Image.new("RGBA", (size, size), bg)
    d = ImageDraw.Draw(img)

    # Cat head
    cx, cy = size * 0.5, size * 0.52
    r = size * 0.30
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(252, 248, 241, 255), outline=ink, width=24)

    # Cat ears
    ear_w, ear_h = size * 0.16, size * 0.20
    left_ear = [(cx - r * 0.72, cy - r * 0.52), (cx - r * 0.20, cy - r * 1.25), (cx - r * 0.02, cy - r * 0.42)]
    right_ear = [(cx + r * 0.02, cy - r * 0.42), (cx + r * 0.20, cy - r * 1.25), (cx + r * 0.72, cy - r * 0.52)]
    d.polygon(left_ear, fill=(252, 248, 241, 255), outline=ink)
    d.polygon(right_ear, fill=(252, 248, 241, 255), outline=ink)
    d.polygon(
        [(p[0] * 0.92 + cx * 0.08, p[1] * 0.92 + cy * 0.08) for p in left_ear],
        fill=(255, 220, 220, 255),
    )
    d.polygon(
        [(p[0] * 0.92 + cx * 0.08, p[1] * 0.92 + cy * 0.08) for p in right_ear],
        fill=(255, 220, 220, 255),
    )

    # Cat eyes
    eye_y = cy - r * 0.08
    eye_dx = r * 0.34
    eye_w, eye_h = r * 0.18, r * 0.22
    d.ellipse((cx - eye_dx - eye_w, eye_y - eye_h, cx - eye_dx + eye_w, eye_y + eye_h), fill=ink)
    d.ellipse((cx + eye_dx - eye_w, eye_y - eye_h, cx + eye_dx + eye_w, eye_y + eye_h), fill=ink)
    d.ellipse((cx - eye_dx - eye_w * 0.38, eye_y - eye_h * 0.45, cx - eye_dx - eye_w * 0.05, eye_y - eye_h * 0.1), fill=white)
    d.ellipse((cx + eye_dx - eye_w * 0.38, eye_y - eye_h * 0.45, cx + eye_dx - eye_w * 0.05, eye_y - eye_h * 0.1), fill=white)

    # Cat nose + mouth
    nose = [(cx, cy + r * 0.10), (cx - r * 0.08, cy + r * 0.18), (cx + r * 0.08, cy + r * 0.18)]
    d.polygon(nose, fill=rose)
    d.line((cx, cy + r * 0.18, cx, cy + r * 0.25), fill=ink, width=8)
    d.arc((cx - r * 0.12, cy + r * 0.20, cx, cy + r * 0.35), start=250, end=25, fill=ink, width=8)
    d.arc((cx, cy + r * 0.20, cx + r * 0.12, cy + r * 0.35), start=155, end=290, fill=ink, width=8)

    # Whiskers
    for sign in (-1, 1):
        x0 = cx + sign * r * 0.16
        for i, dy in enumerate((-r * 0.02, r * 0.07, r * 0.16)):
            d.line((x0, cy + dy, x0 + sign * r * 0.45, cy + dy + (i - 1) * r * 0.02), fill=ink, width=6)

    # Small dog badge in bottom-right (secondary)
    bx, by = size * 0.77, size * 0.78
    br = size * 0.16
    d.ellipse((bx - br, by - br, bx + br, by + br), fill=orange, outline=(255, 255, 255, 255), width=16)
    d.ellipse((bx - br * 0.62, by - br * 0.48, bx + br * 0.62, by + br * 0.55), fill=(255, 232, 204, 255))
    d.ellipse((bx - br * 0.86, by - br * 0.56, bx - br * 0.45, by - br * 0.10), fill=(255, 210, 160, 255))
    d.ellipse((bx + br * 0.45, by - br * 0.56, bx + br * 0.86, by - br * 0.10), fill=(255, 210, 160, 255))
    d.ellipse((bx - br * 0.30, by - br * 0.05, bx - br * 0.12, by + br * 0.12), fill=ink)
    d.ellipse((bx + br * 0.12, by - br * 0.05, bx + br * 0.30, by + br * 0.12), fill=ink)
    d.ellipse((bx - br * 0.09, by + br * 0.12, bx + br * 0.09, by + br * 0.28), fill=ink)

    return img


def main() -> None:
    root = Path(__file__).resolve().parent
    assets = root / "assets"
    assets.mkdir(parents=True, exist_ok=True)
    icon = draw_icon(1024)
    icon.save(assets / "icon.png")
    icon.save(assets / "adaptive-icon.png")
    print(f"Generated icon files in: {assets}")


if __name__ == "__main__":
    main()
