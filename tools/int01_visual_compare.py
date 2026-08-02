"""Generate reproducible Flutter | Native | Diff evidence from an explicit pair manifest."""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageEnhance, ImageFont


def safe_name(value: str) -> str:
    return "".join(character if character.isalnum() or character in "-_" else "-" for character in value)


def add_label(image: Image.Image, text: str) -> None:
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 430, 42), fill=(17, 17, 27))
    draw.text((10, 11), text, fill="white", font=ImageFont.load_default())


def mismatch_mask(left: Image.Image, right: Image.Image, threshold: int) -> Image.Image:
    difference = ImageChops.difference(left.convert("RGB"), right.convert("RGB"))
    mask = difference.getchannel("R").point(lambda value: 255 if value > threshold else 0)
    for channel in ("G", "B"):
        mask = ImageChops.lighter(
            mask,
            difference.getchannel(channel).point(
                lambda value: 255 if value > threshold else 0,
            ),
        )
    return mask


def mae(left: Image.Image, right: Image.Image) -> float:
    difference = ImageChops.difference(left.convert("RGB"), right.convert("RGB"))
    histogram = difference.histogram()
    weighted = sum((index % 256) * count for index, count in enumerate(histogram))
    return weighted / (left.width * left.height * 3)


def heatmap(mask: Image.Image, difference: Image.Image) -> Image.Image:
    intensity = ImageEnhance.Contrast(difference.convert("L")).enhance(3.0)
    result = Image.composite(
        Image.new("RGB", mask.size, (255, 20, 60)),
        Image.new("RGB", mask.size, "black"),
        ImageChops.multiply(mask, intensity),
    )
    box = mask.getbbox()
    if box:
        ImageDraw.Draw(result).rectangle(box, outline=(255, 255, 0), width=4)
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--contact-sheet", type=Path, required=True)
    parser.add_argument("--threshold", type=int, default=16)
    args = parser.parse_args()

    manifest_path = args.manifest.resolve()
    manifest = json.loads(manifest_path.read_text(encoding="utf-8-sig"))
    pairs = manifest["pairs"]
    if not pairs:
        raise ValueError("Pair manifest is empty")

    args.output.mkdir(parents=True, exist_ok=True)
    args.contact_sheet.parent.mkdir(parents=True, exist_ok=True)
    metrics: list[dict[str, object]] = []
    contact_rows: list[Image.Image] = []
    base = manifest_path.parent

    for index, pair in enumerate(pairs, start=1):
        state = str(pair["state"])
        slug = safe_name(str(pair.get("slug") or f"{index:02d}-{state}"))
        flutter_path = (base / pair["flutter"]).resolve()
        native_path = (base / pair["native"]).resolve()
        flutter = Image.open(flutter_path).convert("RGB")
        native = Image.open(native_path).convert("RGB")
        if flutter.size != native.size:
            raise ValueError(
                f"{state}: dimensions differ: Flutter {flutter.size}, Native {native.size}",
            )

        pair_output = args.output / slug
        pair_output.mkdir(parents=True, exist_ok=True)
        difference = ImageChops.difference(flutter, native)
        mask = mismatch_mask(flutter, native, args.threshold)
        mismatch_pixels = sum(mask.histogram()[128:])
        total_pixels = flutter.width * flutter.height
        mismatch_percent = mismatch_pixels / total_pixels * 100
        mean_absolute_error = mae(flutter, native)
        difference_box = mask.getbbox()

        side = Image.new("RGB", (flutter.width * 2, flutter.height), "white")
        side.paste(flutter, (0, 0))
        side.paste(native, (flutter.width, 0))
        add_label(side, f"{state} | Flutter")
        side_label = side.crop((flutter.width, 0, side.width, side.height))
        add_label(side_label, f"{state} | Native")
        side.paste(side_label, (flutter.width, 0))
        side.save(pair_output / "side-by-side.png", optimize=True)

        overlay = Image.blend(flutter, native, 0.5)
        add_label(overlay, f"{state} | alpha 50%")
        overlay.save(pair_output / "alpha-overlay.png", optimize=True)

        threshold_diff = heatmap(mask, difference)
        add_label(threshold_diff, f"{state} | diff > {args.threshold}")
        threshold_diff.save(pair_output / "threshold-diff.png", optimize=True)

        boxes = native.copy()
        if difference_box:
            ImageDraw.Draw(boxes).rectangle(difference_box, outline=(255, 0, 0), width=5)
        add_label(boxes, f"{state} | mismatch bbox")
        boxes.save(pair_output / "bounding-boxes.png", optimize=True)

        metrics.append(
            {
                "state": state,
                "slug": slug,
                "flutter": str(flutter_path),
                "native": str(native_path),
                "width": flutter.width,
                "height": flutter.height,
                "threshold": args.threshold,
                "mismatch_percent": round(mismatch_percent, 4),
                "mae": round(mean_absolute_error, 4),
                "difference_bbox": list(difference_box) if difference_box else None,
                "product_mismatch_notes": pair.get("product_mismatch_notes", ""),
                "renderer_residual_notes": pair.get("renderer_residual_notes", ""),
            },
        )

        thumb_size = (270, 600)
        row = Image.new("RGB", (810, 644), "white")
        for column, image in enumerate((flutter, native, threshold_diff)):
            thumb = image.copy()
            thumb.thumbnail(thumb_size, Image.Resampling.LANCZOS)
            row.paste(thumb, (column * 270, 44))
        row_draw = ImageDraw.Draw(row)
        row_draw.text((8, 10), f"{state} | Flutter", fill="black")
        row_draw.text((278, 10), f"{state} | Native", fill="black")
        row_draw.text(
            (548, 10),
            f"Diff {mismatch_percent:.2f}% | MAE {mean_absolute_error:.2f}",
            fill="black",
        )
        contact_rows.append(row)

    report = {
        "threshold": args.threshold,
        "pairCount": len(metrics),
        "missingEvidenceCount": 0,
        "pairs": metrics,
    }
    (args.output / "metrics.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    with (args.output / "metrics.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=metrics[0].keys())
        writer.writeheader()
        writer.writerows(metrics)

    contact = Image.new("RGB", (810, 644 * len(contact_rows)), "white")
    for row_index, row in enumerate(contact_rows):
        contact.paste(row, (0, row_index * 644))
    contact.save(args.contact_sheet, optimize=True)


if __name__ == "__main__":
    main()
