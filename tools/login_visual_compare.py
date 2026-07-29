"""Generate deterministic Flutter/Native login visual-parity evidence."""

from __future__ import annotations

import csv
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageEnhance, ImageFont


ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "docs" / "evidence" / "2026-07-28-login-parity"
FLUTTER = EVIDENCE / "flutter"
NATIVE = EVIDENCE / "native"
OUTPUT = EVIDENCE / "comparison"
CONTACT_SHEET = EVIDENCE / "login-parity-contact-sheet.png"
THRESHOLD = 16


def label(draw: ImageDraw.ImageDraw, x: int, y: int, text: str) -> None:
    draw.rectangle((x, y, x + 350, y + 38), fill=(17, 17, 27))
    draw.text((x + 10, y + 9), text, fill="white", font=ImageFont.load_default())


def mismatch_mask(left: Image.Image, right: Image.Image) -> Image.Image:
    difference = ImageChops.difference(left.convert("RGB"), right.convert("RGB"))
    channels = difference.split()
    mask = channels[0].point(lambda value: 255 if value > THRESHOLD else 0)
    for channel in channels[1:]:
        mask = ImageChops.lighter(
            mask,
            channel.point(lambda value: 255 if value > THRESHOLD else 0),
        )
    return mask


def heatmap(mask: Image.Image, difference: Image.Image) -> Image.Image:
    intensity = ImageEnhance.Contrast(difference.convert("L")).enhance(3.0)
    red = Image.new("RGB", mask.size, (255, 20, 60))
    black = Image.new("RGB", mask.size, "black")
    result = Image.composite(red, black, ImageChops.multiply(mask, intensity))
    draw = ImageDraw.Draw(result)
    global_box = mask.getbbox()
    if global_box:
        draw.rectangle(global_box, outline=(255, 255, 0), width=4)
    band_height = 240
    for top in range(0, mask.height, band_height):
        band = mask.crop((0, top, mask.width, min(top + band_height, mask.height)))
        box = band.getbbox()
        if box and sum(band.histogram()[128:]) > mask.width * 4:
            draw.rectangle(
                (box[0], box[1] + top, box[2], box[3] + top),
                outline=(0, 255, 255),
                width=2,
            )
    return result


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    metrics: list[dict[str, str]] = []
    contact_rows: list[Image.Image] = []

    for index in range(1, 10):
        flutter_path = next(FLUTTER.glob(f"flutter-{index:02d}-*.png"))
        native_path = next(NATIVE.glob(f"native-{index:02d}-*.png"))
        flutter = Image.open(flutter_path).convert("RGB")
        native = Image.open(native_path).convert("RGB")
        if flutter.size != native.size:
            raise ValueError(
                f"State {index:02d} dimensions differ: {flutter.size} != {native.size}"
            )

        side = Image.new("RGB", (flutter.width * 2, flutter.height), "white")
        side.paste(flutter, (0, 0))
        side.paste(native, (flutter.width, 0))
        side_draw = ImageDraw.Draw(side)
        label(side_draw, 0, 0, f"{index:02d} Flutter")
        label(side_draw, flutter.width, 0, f"{index:02d} Native")
        side.save(OUTPUT / f"{index:02d}-side-by-side.png", optimize=True)

        overlay = Image.blend(flutter, native, 0.5)
        label(ImageDraw.Draw(overlay), 0, 0, f"{index:02d} alpha 50%")
        overlay.save(OUTPUT / f"{index:02d}-overlay.png", optimize=True)

        difference = ImageChops.difference(flutter, native)
        mask = mismatch_mask(flutter, native)
        diff = heatmap(mask, difference)
        label(ImageDraw.Draw(diff), 0, 0, f"{index:02d} diff > {THRESHOLD}")
        diff.save(OUTPUT / f"{index:02d}-diff.png", optimize=True)

        mismatch_pixels = sum(mask.histogram()[128:])
        total_pixels = flutter.width * flutter.height
        mismatch_percent = mismatch_pixels / total_pixels * 100
        metrics.append(
            {
                "state": f"{index:02d}",
                "flutter": flutter_path.name,
                "native": native_path.name,
                "width": str(flutter.width),
                "height": str(flutter.height),
                "threshold": str(THRESHOLD),
                "mismatch_percent": f"{mismatch_percent:.4f}",
                "difference_bbox": str(mask.getbbox() or ""),
            }
        )

        thumb_size = (270, 600)
        row = Image.new("RGB", (thumb_size[0] * 3, thumb_size[1] + 36), "white")
        for column, image in enumerate((flutter, native, diff)):
            thumb = image.copy()
            thumb.thumbnail(thumb_size, Image.Resampling.LANCZOS)
            row.paste(thumb, (column * thumb_size[0], 36))
        row_draw = ImageDraw.Draw(row)
        row_draw.text((8, 10), f"{index:02d} Flutter", fill="black")
        row_draw.text((278, 10), f"{index:02d} Native", fill="black")
        row_draw.text(
            (548, 10),
            f"{index:02d} Diff {mismatch_percent:.2f}%",
            fill="black",
        )
        contact_rows.append(row)

    with (OUTPUT / "metrics.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=metrics[0].keys())
        writer.writeheader()
        writer.writerows(metrics)

    contact = Image.new(
        "RGB",
        (contact_rows[0].width, sum(row.height for row in contact_rows)),
        "white",
    )
    top = 0
    for row in contact_rows:
        contact.paste(row, (0, top))
        top += row.height
    contact.save(CONTACT_SHEET, optimize=True)


if __name__ == "__main__":
    main()
