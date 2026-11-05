"""Fetch Noto Animated Emoji (Lottie, CC BY 4.0) into the chat module assets.

Usage: python tools/emoji/fetch_animated_emoji.py [limit]
Downloads the `limit` most popular animated emoji (default 400), minifies the
Lottie JSON and writes an index consumed by AnimatedEmojiCatalog.
"""
import json
import os
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor

sys.stdout.reconfigure(encoding="utf-8")

API = "https://googlefonts.github.io/noto-emoji-animation/data/api.json"
OUT = os.path.join("feature", "chat", "src", "main", "assets", "emoji", "animated")
LIMIT = int(sys.argv[1]) if len(sys.argv) > 1 else 400
HEADERS = {"User-Agent": "Mozilla/5.0"}


def get(url: str) -> bytes:
    req = urllib.request.Request(url, headers=HEADERS)
    return urllib.request.urlopen(req, timeout=30).read()


def main() -> None:
    api = json.loads(get(API))
    icons = sorted(api["icons"], key=lambda i: -i["popularity"])[:LIMIT]
    os.makedirs(OUT, exist_ok=True)

    def work(icon):
        cp = icon["codepoint"]
        url = f"https://fonts.gstatic.com/s/e/notoemoji/latest/{cp}/lottie.json"
        try:
            data = json.loads(get(url))
        except Exception as exc:  # noqa: BLE001
            print("FAIL", cp, exc)
            return None
        with open(os.path.join(OUT, cp + ".json"), "w", encoding="utf-8") as fh:
            json.dump(data, fh, separators=(",", ":"))
        return cp

    with ThreadPoolExecutor(8) as pool:
        done = [c for c in pool.map(work, icons) if c]

    with open(os.path.join(OUT, "index.json"), "w", encoding="utf-8") as fh:
        json.dump(sorted(done), fh, separators=(",", ":"))
    total = sum(os.path.getsize(os.path.join(OUT, f)) for f in os.listdir(OUT))
    print("emoji:", len(done), "total bytes:", total)


if __name__ == "__main__":
    main()
