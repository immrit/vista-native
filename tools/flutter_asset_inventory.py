"""Create a deterministic, per-file inventory of Flutter visual assets."""

from __future__ import annotations

import csv
import hashlib
from pathlib import Path

from PIL import Image


FLUTTER_ROOT = Path(r"E:\vista")
NATIVE_ROOT = Path(__file__).resolve().parents[1]
OUTPUT = (
    NATIVE_ROOT
    / "docs"
    / "audits"
    / "2026-07-28-flutter-visual-assets-index.csv"
)


def dimensions(path: Path) -> str:
    try:
        with Image.open(path) as image:
            return f"{image.width}x{image.height}"
    except (OSError, ValueError):
        return "vector/audio/font/unknown"


def consumer(relative: str) -> str:
    if relative.startswith("assets/emoji/modern/"):
        return "modern_emoji_map.json / emoji picker"
    if relative.startswith("assets/images/onboarding/"):
        return "onboarding"
    if relative.startswith("assets/sounds/"):
        return "chat notification"
    if relative.endswith("logo/black-logo.png"):
        return "AuthWizardScreen light"
    if relative.endswith("logo/logo-white.png"):
        return "AuthWizardScreen dark"
    if relative.startswith("lib/utils/fonts/Vazirmatn"):
        return "AppTheme / Login typography"
    if relative.startswith("lib/utils/fonts/"):
        return "non-Login or legacy typography"
    return "non-Login visual asset"


def migration(relative: str) -> tuple[str, str, str]:
    if relative.endswith("logo/black-logo.png"):
        destination = (
            "core/designsystem/src/main/res/drawable-nodpi/"
            "vista_auth_logo_light.png"
        )
        return "yes", destination, "byte-identical; current Login consumer"
    if relative.endswith("logo/logo-white.png"):
        destination = (
            "core/designsystem/src/main/res/drawable-nodpi/"
            "vista_auth_logo_dark.png"
        )
        return "yes", destination, "byte-identical; current Login consumer"
    vazirmatn = {
        "VazirmatnBlack.ttf": "vazirmatn_black.ttf",
        "VazirmatnExtraBold.ttf": "vazirmatn_extrabold.ttf",
        "VazirmatnBold.ttf": "vazirmatn_bold.ttf",
        "VazirmatnSemiBold.ttf": "vazirmatn_semibold.ttf",
        "VazirmatnMedium.ttf": "vazirmatn_medium.ttf",
        "VazirmatnRegular.ttf": "vazirmatn_regular.ttf",
        "VazirmatnLight.ttf": "vazirmatn_light.ttf",
    }
    name = Path(relative).name
    if name in vazirmatn:
        destination = (
            "core/designsystem/src/main/res/font/" + vazirmatn[name]
        )
        return "already present", destination, "byte-identical"
    return "no", "", "outside Login/shared-foundation scope"


def main() -> None:
    roots = (
        FLUTTER_ROOT / "assets",
        FLUTTER_ROOT / "lib" / "utils" / "images",
        FLUTTER_ROOT / "lib" / "utils" / "fonts",
    )
    paths = sorted(
        (path for root in roots for path in root.rglob("*") if path.is_file()),
        key=lambda path: path.as_posix().lower(),
    )
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", newline="", encoding="utf-8-sig") as handle:
        fieldnames = (
            "flutter_path",
            "file_type",
            "dimensions",
            "sha256",
            "consumer",
            "migrate_to_native",
            "native_destination",
            "notes",
        )
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        for path in paths:
            relative = path.relative_to(FLUTTER_ROOT).as_posix()
            migrate, destination, notes = migration(relative)
            writer.writerow(
                {
                    "flutter_path": relative,
                    "file_type": path.suffix.lstrip(".").upper(),
                    "dimensions": dimensions(path),
                    "sha256": hashlib.sha256(path.read_bytes()).hexdigest().upper(),
                    "consumer": consumer(relative),
                    "migrate_to_native": migrate,
                    "native_destination": destination,
                    "notes": notes,
                }
            )
    print(f"{len(paths)} assets indexed at {OUTPUT}")


if __name__ == "__main__":
    main()
