from __future__ import annotations

import json
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageEnhance, ImageFont


ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "docs" / "evidence" / "2026-07-29-feed-profile-parity"
FLUTTER = EVIDENCE / "flutter"
NATIVE = EVIDENCE / "native"
OUTPUT = EVIDENCE / "comparison"

GROUPS = {
    "feed": [
        "01-feed-initial-loading.png", "02-feed-first-content.png",
        "03-feed-card-image.png", "04-feed-card-video.png",
        "05-feed-caption-short.png", "06-feed-caption-long.png",
        "07-feed-refreshing.png", "08-feed-pagination-loading.png",
        "09-feed-append-error.png", "10-feed-offline-cached.png",
        "11-feed-empty.png", "12-feed-initial-error.png",
    ],
    "post-detail": [
        "13-post-detail-image.png", "14-post-detail-video-thumbnail.png",
        "15-post-detail-long-caption.png", "16-post-detail-counts-actions.png",
        "17-post-detail-loading.png", "18-post-detail-error.png",
    ],
    "own-profile": [
        "19-own-profile-normal.png", "20-own-profile-long-bio.png",
        "21-own-profile-no-bio.png", "22-own-profile-posts-content.png",
        "23-own-profile-empty-posts.png", "24-own-profile-offline.png",
        "25-own-profile-refreshing.png", "26-own-profile-error.png",
    ],
    "other-profile": [
        "27-other-profile-not-following.png", "28-other-profile-pending-follow.png",
        "29-other-profile-following.png", "30-other-profile-requested-private.png",
        "31-other-profile-pending-unfollow.png", "32-other-profile-follow-error-rollback.png",
        "33-other-profile-long-bio.png", "34-other-profile-empty-posts.png",
        "35-other-profile-offline-cached.png", "36-other-profile-not-found-error.png",
    ],
}

INVALID_FLUTTER: dict[str, str] = {}

PANEL_SIZE = (324, 720)
HEADER_HEIGHT = 52
ROW_GAP = 10
BACKGROUND = (245, 246, 250)
MISSING = (225, 228, 235)


def font(size: int) -> ImageFont.ImageFont:
    try:
        return ImageFont.truetype("arial.ttf", size)
    except OSError:
        return ImageFont.load_default()


def normalized(path: Path) -> Image.Image:
    return Image.open(path).convert("RGB").resize((1080, 2400), Image.Resampling.LANCZOS)


def metric(flutter: Image.Image, native: Image.Image) -> dict[str, float | int]:
    difference = ImageChops.difference(flutter, native)
    histogram = difference.histogram()
    pixels = flutter.width * flutter.height
    absolute_sum = sum((index % 256) * count for index, count in enumerate(histogram))
    mask = difference.convert("L").point(lambda value: 255 if value > 16 else 0)
    mismatch_pixels = mask.histogram()[255]
    return {
        "width": flutter.width,
        "height": flutter.height,
        "threshold": 16,
        "mismatch_pixels": mismatch_pixels,
        "mismatch_percentage": round(mismatch_pixels * 100 / pixels, 4),
        "mean_absolute_error": round(absolute_sum / (pixels * 3), 4),
    }


def compare_pair(name: str, flutter_path: Path, native_path: Path) -> dict[str, float | int]:
    pair_dir = OUTPUT / Path(name).stem
    pair_dir.mkdir(parents=True, exist_ok=True)
    flutter = normalized(flutter_path)
    native = normalized(native_path)
    difference = ImageChops.difference(flutter, native)
    side = Image.new("RGB", (3240, 2460), "white")
    side.paste(flutter, (0, 60))
    side.paste(native, (1080, 60))
    side.paste(ImageEnhance.Contrast(difference).enhance(4), (2160, 60))
    draw = ImageDraw.Draw(side)
    for index, label in enumerate(("Flutter", "Native", "Diff x4")):
        draw.text((index * 1080 + 24, 16), label, fill="black", font=font(30))
    side.save(pair_dir / "flutter-native-diff.png", optimize=True)
    Image.blend(flutter, native, 0.5).save(pair_dir / "alpha-overlay.png", optimize=True)
    ImageEnhance.Contrast(difference).enhance(4).save(pair_dir / "pixel-diff-x4.png", optimize=True)
    values = metric(flutter, native)
    (pair_dir / "metrics.json").write_text(
        json.dumps(values, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return values


def thumbnail(path: Path | None, missing_note: str) -> Image.Image:
    if path is None:
        panel = Image.new("RGB", PANEL_SIZE, MISSING)
        ImageDraw.Draw(panel).multiline_text(
            (18, PANEL_SIZE[1] // 2 - 38),
            "MISSING EVIDENCE\n" + missing_note,
            fill=(70, 74, 84), font=font(18), spacing=8,
        )
        return panel
    return Image.open(path).convert("RGB").resize(PANEL_SIZE, Image.Resampling.LANCZOS)


def contact_sheet(group: str, names: list[str], metrics: dict[str, dict[str, float | int]]) -> None:
    row_height = HEADER_HEIGHT + PANEL_SIZE[1] + ROW_GAP
    sheet = Image.new("RGB", (PANEL_SIZE[0] * 3, row_height * len(names)), BACKGROUND)
    draw = ImageDraw.Draw(sheet)
    for row, name in enumerate(names):
        top = row * row_height
        flutter_path, native_path = FLUTTER / name, NATIVE / name
        flutter_valid = flutter_path.exists() and name not in INVALID_FLUTTER
        pair_metric = metrics.get(name)
        mismatch = f" mismatch={pair_metric['mismatch_percentage']:.2f}%" if pair_metric else ""
        draw.text((12, top + 8), Path(name).stem + mismatch, fill="black", font=font(20))
        flutter_panel = thumbnail(
            flutter_path if flutter_valid else None,
            INVALID_FLUTTER.get(name, "Flutter reference unavailable."),
        )
        native_panel = thumbnail(
            native_path if native_path.exists() else None, "Native state not captured."
        )
        if flutter_valid and native_path.exists():
            diff_panel = ImageEnhance.Contrast(ImageChops.difference(
                Image.open(flutter_path).convert("RGB").resize(PANEL_SIZE, Image.Resampling.LANCZOS),
                Image.open(native_path).convert("RGB").resize(PANEL_SIZE, Image.Resampling.LANCZOS),
            )).enhance(4)
        else:
            diff_panel = thumbnail(None, "Comparable pair unavailable.")
        y = top + HEADER_HEIGHT
        sheet.paste(flutter_panel, (0, y))
        sheet.paste(native_panel, (PANEL_SIZE[0], y))
        sheet.paste(diff_panel, (PANEL_SIZE[0] * 2, y))
    sheet.save(OUTPUT / f"{group}-contact-sheet.png", optimize=True)


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    all_metrics: dict[str, dict[str, float | int]] = {}
    unavailable: list[str] = []
    for names in GROUPS.values():
        for name in names:
            flutter_path, native_path = FLUTTER / name, NATIVE / name
            if flutter_path.exists() and native_path.exists() and name not in INVALID_FLUTTER:
                all_metrics[name] = compare_pair(name, flutter_path, native_path)
            else:
                unavailable.append(name)
    for group, names in GROUPS.items():
        contact_sheet(group, names, all_metrics)
    summary = {
        "valid_pairs": len(all_metrics),
        "requested_states": sum(map(len, GROUPS.values())),
        "pairs": all_metrics,
        "unavailable_pairs": unavailable,
        "invalid_flutter_captures": INVALID_FLUTTER,
    }
    (OUTPUT / "comparison-summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    main()
