from __future__ import annotations

import json
import sys
from collections import deque
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont


def components(mask: Image.Image, scale: int = 4) -> list[tuple[int, int, int, int]]:
    small = mask.resize(
        (max(1, mask.width // scale), max(1, mask.height // scale)),
        Image.Resampling.NEAREST,
    )
    pixels = small.load()
    seen: set[tuple[int, int]] = set()
    boxes: list[tuple[int, int, int, int]] = []
    for y in range(small.height):
        for x in range(small.width):
            if pixels[x, y] == 0 or (x, y) in seen:
                continue
            queue = deque([(x, y)])
            seen.add((x, y))
            xs: list[int] = []
            ys: list[int] = []
            while queue:
                cx, cy = queue.popleft()
                xs.append(cx)
                ys.append(cy)
                for nx, ny in (
                    (cx - 1, cy),
                    (cx + 1, cy),
                    (cx, cy - 1),
                    (cx, cy + 1),
                ):
                    if (
                        0 <= nx < small.width
                        and 0 <= ny < small.height
                        and pixels[nx, ny] != 0
                        and (nx, ny) not in seen
                    ):
                        seen.add((nx, ny))
                        queue.append((nx, ny))
            box = (
                min(xs) * scale,
                min(ys) * scale,
                min(mask.width, (max(xs) + 1) * scale),
                min(mask.height, (max(ys) + 1) * scale),
            )
            if (box[2] - box[0]) * (box[3] - box[1]) >= 900:
                boxes.append(box)
    return sorted(
        boxes,
        key=lambda box: (box[2] - box[0]) * (box[3] - box[1]),
        reverse=True,
    )[:12]


def compare(reference_path: Path, candidate_path: Path, output_dir: Path) -> dict:
    reference = Image.open(reference_path).convert("RGB")
    candidate = Image.open(candidate_path).convert("RGB")
    if candidate.size != reference.size:
        candidate = candidate.resize(reference.size, Image.Resampling.LANCZOS)

    output_dir.mkdir(parents=True, exist_ok=True)
    diff = ImageChops.difference(reference, candidate)
    grayscale = diff.convert("L")
    threshold = 24
    mask = grayscale.point(lambda value: 255 if value > threshold else 0)
    histogram = mask.histogram()
    mismatched = histogram[255]
    total = reference.width * reference.height
    mismatch_percentage = mismatched * 100.0 / total
    mean_absolute_error = sum(
        value * count for value, count in enumerate(grayscale.histogram())
    ) / total

    side_by_side = Image.new(
        "RGB",
        (reference.width * 2, reference.height),
        "white",
    )
    side_by_side.paste(reference, (0, 0))
    side_by_side.paste(candidate, (reference.width, 0))
    side_by_side.save(output_dir / "01-initial-side-by-side.png")

    overlay = Image.blend(reference, candidate, 0.5)
    overlay.save(output_dir / "01-initial-alpha-overlay.png")

    thresholded = Image.new("RGB", reference.size, "black")
    thresholded.paste((255, 0, 90), mask=mask)
    thresholded.save(output_dir / "01-initial-thresholded-diff.png")

    boxed = candidate.copy()
    draw = ImageDraw.Draw(boxed)
    boxes = components(mask)
    for index, box in enumerate(boxes, start=1):
        draw.rectangle(box, outline=(255, 0, 90), width=5)
        draw.text((box[0] + 6, box[1] + 4), str(index), fill=(255, 0, 90))
    boxed.save(output_dir / "01-initial-bounding-boxes.png")

    cell_width = 360
    cell_height = 800
    header = 40
    contact = Image.new("RGB", (cell_width * 3, cell_height + header), "white")
    draw = ImageDraw.Draw(contact)
    labels = ("Flutter", "Native", "Diff")
    for index, label in enumerate(labels):
        draw.text((index * cell_width + 12, 12), label, fill="black")
    contact.paste(reference.resize((cell_width, cell_height)), (0, header))
    contact.paste(candidate.resize((cell_width, cell_height)), (cell_width, header))
    contact.paste(
        thresholded.resize((cell_width, cell_height)),
        (cell_width * 2, header),
    )
    contact.save(output_dir.parent.parent / "search-parity-contact-sheet.png")

    metrics = {
        "pair": "01-initial",
        "reference": str(reference_path),
        "candidate": str(candidate_path),
        "width": reference.width,
        "height": reference.height,
        "threshold": threshold,
        "mismatched_pixels": mismatched,
        "total_pixels": total,
        "mismatch_percentage": round(mismatch_percentage, 4),
        "mean_absolute_error": round(mean_absolute_error, 4),
        "bounding_boxes": boxes,
    }
    (output_dir / "metrics.json").write_text(
        json.dumps(metrics, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return metrics


if __name__ == "__main__":
    if len(sys.argv) != 4:
        raise SystemExit(
            "usage: search_visual_compare.py FLUTTER_PNG NATIVE_PNG OUTPUT_DIR"
        )
    result = compare(Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]))
    print(json.dumps(result, ensure_ascii=False))
