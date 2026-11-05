#!/usr/bin/env python3
"""
Sync modern emoji images and mapping from official Telegram Android repository (DrKLO/Telegram).
Decodes EPK3 emoji.pack into individual modern 66x66 RGBA PNG assets and generates modern_emoji_map.json.
"""

import os
import sys
import json
import struct
import zlib
import io
import datetime
import urllib.request
import re
from PIL import Image

sys.stdout.reconfigure(encoding='utf-8')

TELEGRAM_EMOJIDATA_URL = "https://raw.githubusercontent.com/DrKLO/Telegram/master/TMessagesProj/src/main/java/org/telegram/messenger/EmojiData.java"
TELEGRAM_EMOJIPACK_URL = "https://raw.githubusercontent.com/DrKLO/Telegram/master/TMessagesProj/src/main/assets/emoji.pack"
TELEGRAM_MASTER_COMMIT_URL = "https://api.github.com/repos/DrKLO/Telegram/commits/master"

WORKSPACE_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))

FEATURE_CHAT_ASSETS = os.path.join(WORKSPACE_ROOT, "feature", "chat", "src", "main", "assets", "emoji")
CORE_DESIGN_ASSETS = os.path.join(WORKSPACE_ROOT, "core", "designsystem", "src", "main", "assets", "emoji")

CACHE_DIR = os.path.join(WORKSPACE_ROOT, ".gradle", "emoji_cache")


class EmojiPackReader:
    HEADER_SIZE = 32
    ENTRY_SIZE = 20
    SIDE = 64
    PIXELS = SIDE * SIDE
    NO_MASK = 0xFFFF

    DEFLATE = 0
    INDEX_WEBP = 1
    RGBA_WEBP = 2
    FULL_IMAGE = 3
    MAP_REFERENCE = 4

    def __init__(self, data_bytes):
        self.data = data_bytes
        header = self.data[:32]
        magic, v, entry_size, side1, side2, self.count, self.emoji_count, cap, zero = struct.unpack('<4sHHIIIIII', header)
        if magic != b'EPK3':
            raise ValueError(f"Invalid EPK3 magic: {magic}")
        self.entries = []
        for i in range(self.count):
            e_offset = self.HEADER_SIZE + i * self.ENTRY_SIZE
            raw = self.data[e_offset : e_offset + self.ENTRY_SIZE]
            id_val, mask_id, offset, length, a, b, colors, codec, pred, ch_byte, t_byte = struct.unpack('<HHIHHHHBBBB', raw)
            self.entries.append({
                'id': id_val,
                'mask_id': mask_id,
                'offset': offset,
                'length': length,
                'a': a,
                'b': b,
                'colors': colors,
                'codec': codec,
                'prediction': pred,
                'channels': ch_byte & 127,
                'compressed_palette': (ch_byte & 128) != 0,
                'packed_7bit': (t_byte & 1) != 0,
                'a_transform': (t_byte >> 1) & 7,
                'b_transform': (t_byte >> 4) & 7,
            })
        self.root_cache = {}

    def find(self, key, first, end):
        low = first
        high = end - 1
        while low <= high:
            mid = (low + high) >> 1
            val = self.entries[mid]['id']
            if val < key:
                low = mid + 1
            elif val > key:
                high = mid - 1
            else:
                return mid
        return -1

    def find_emoji(self, x, y):
        if x < 0 or x > 15 or y < 0 or y >= 4096:
            return -1
        return self.find(x * 4096 + y, 0, self.emoji_count)

    def palette_length(self, e):
        if e['compressed_palette']:
            offset = e['offset']
            length = struct.unpack('<H', self.data[offset : offset + 2])[0]
            return length + 2
        n = e['colors'] * e['channels']
        return (n * 7 + 7) // 8 if e['packed_7bit'] else n

    def decode_webp(self, raw_bytes):
        length = len(raw_bytes)
        pad = length & 1
        header = struct.pack('<4sI4s4sI', b'RIFF', 12 + length + pad, b'WEBP', b'VP8L', length)
        full_data = header + raw_bytes
        if pad != 0:
            full_data += b'\x00'
        return Image.open(io.BytesIO(full_data))

    def decode_indices(self, record):
        e = self.entries[record]
        pred = e['prediction']
        a_map = None
        b_map = None
        a_colors = 0
        b_colors = 0
        if pred != 0:
            a_map = self.root_map(e['a'])
            a_colors = self.entries[e['a']]['colors']
            if pred in (3, 5, 6):
                b_map = self.root_map(e['b'])
                b_colors = self.entries[e['b']]['colors']

        if pred == 4: # MAP_REFERENCE
            result = bytearray(self.PIXELS)
            for i in range(self.PIXELS):
                result[i] = a_map[self.source_index(i, e['a_transform'])]
            return result

        lut_length = a_colors + b_colors
        pal_len = self.palette_length(e)
        offset = e['offset'] + pal_len
        length = e['length'] - pal_len
        cut = 32
        if pred in (5, 6):
            cut = self.data[offset]
            offset += 1
            length -= 1

        payload = self.data[offset : offset + length]
        result = bytearray(self.PIXELS)

        if e['codec'] == self.DEFLATE:
            decomp = zlib.decompress(payload, -zlib.MAX_WBITS)
            lut = decomp[:lut_length]
            result[:] = decomp[lut_length : lut_length + self.PIXELS]
        else: # INDEX_WEBP
            lut = payload[:lut_length]
            webp_data = payload[lut_length:]
            im = self.decode_webp(webp_data)
            pixels = list(im.getdata())
            for i in range(self.PIXELS):
                p = pixels[i]
                if isinstance(p, tuple):
                    p = p[0]
                result[i] = p & 255

        if pred in (1, 2):
            for i in range(self.PIXELS):
                idx = a_map[self.source_index(i, e['a_transform'])]
                p = lut[idx]
                if pred == 1:
                    result[i] = (result[i] ^ p) & 255
                else:
                    result[i] = (result[i] + p) & 255
        elif pred in (3, 5, 6):
            for i in range(self.PIXELS):
                left = (i >> 6) < cut if pred == 6 else (i & 63) < cut
                idx = a_map[self.source_index(i, e['a_transform'])] if left else b_map[self.source_index(i, e['b_transform'])]
                lut_idx = idx if left else (a_colors + idx)
                p = lut[lut_idx]
                result[i] = (result[i] + p) & 255

        return result

    def root_map(self, record):
        if record not in self.root_cache:
            self.root_cache[record] = self.decode_indices(record)
        return self.root_cache[record]

    @staticmethod
    def source_index(i, transform):
        x = i & 63
        y = i >> 6
        if transform & 1: x = 63 - x
        if transform & 2: y = 63 - y
        return (x << 6) | y if (transform & 4) else (y << 6) | x

    def decode_pixels(self, record):
        e = self.entries[record]
        offset = e['offset']
        length = e['length']
        codec = e['codec']

        if codec in (self.RGBA_WEBP, self.FULL_IMAGE):
            raw = self.data[offset : offset + length]
            if codec == self.RGBA_WEBP:
                im = self.decode_webp(raw).convert('RGBA')
            else:
                im = Image.open(io.BytesIO(raw)).convert('RGBA')
            pixels = list(im.getdata())
            return [((a << 24) | (r << 16) | (g << 8) | b) for r, g, b, a in pixels]

        indices = self.decode_indices(record)
        colors = e['colors']
        channels = e['channels']
        components = colors * channels

        if e['compressed_palette']:
            pal_comp_len = struct.unpack('<H', self.data[offset : offset + 2])[0]
            raw_comp = self.data[offset + 2 : offset + 2 + pal_comp_len]
            decomp_pal = zlib.decompress(raw_comp, -zlib.MAX_WBITS)
            raw_pal = decomp_pal
        else:
            raw_pal = self.data[offset : offset + (components * 7 + 7) // 8 if e['packed_7bit'] else offset + components]

        palette_bytes = bytearray(components)
        if e['packed_7bit']:
            for i in range(components):
                bit = i * 7
                shift = bit & 7
                pos = bit >> 3
                v = raw_pal[pos] >> shift
                if shift > 1:
                    v |= (raw_pal[pos + 1]) << (8 - shift)
                v &= 127
                palette_bytes[i] = ((v << 1) | (v >> 6)) & 255
        else:
            palette_bytes[:] = raw_pal[:components]

        palette = [0] * colors
        cursor = 0
        for i in range(colors):
            if channels == 0:
                palette[i] = 0xFF000000 | (i * 0x010101)
            elif channels == 1:
                palette[i] = 0xFF000000 | (palette_bytes[cursor] * 0x010101)
                cursor += 1
            else:
                r = palette_bytes[cursor]
                g = palette_bytes[cursor + 1]
                b = palette_bytes[cursor + 2]
                cursor += 3
                a = palette_bytes[cursor] if channels == 4 else 255
                if channels == 4:
                    cursor += 1
                palette[i] = (a << 24) | (r << 16) | (g << 8) | b

        out = [0] * self.PIXELS
        for i in range(self.PIXELS):
            out[i] = palette[indices[i]]
        return out

    def get_emoji_image(self, x, y):
        record = self.find_emoji(x, y)
        if record < 0:
            return None
        mask_id = self.entries[record]['mask_id']
        if mask_id == self.NO_MASK:
            pixels = self.decode_pixels(record)
        else:
            emoji_pixels = self.decode_pixels(record)
            mask_record = self.find(mask_id, self.emoji_count, self.count)
            mask_pixels = self.decode_pixels(mask_record)
            pixels = [0] * self.PIXELS
            for i in range(self.PIXELS):
                c = emoji_pixels[i]
                c_a = (c >> 24) & 255
                m_a = mask_pixels[i] & 255
                alpha = ((c_a * m_a) + 127) // 255
                pixels[i] = (alpha << 24) | (c & 0x00FFFFFF)

        im = Image.new('RGBA', (self.SIDE, self.SIDE))
        rgba_data = []
        for p in pixels:
            a = (p >> 24) & 255
            r = (p >> 16) & 255
            g = (p >> 8) & 255
            b = p & 255
            rgba_data.append((r, g, b, a))
        im.putdata(rgba_data)
        return im


def fetch_file(url, local_path):
    os.makedirs(os.path.dirname(local_path), exist_ok=True)
    if os.path.exists(local_path) and os.path.getsize(local_path) > 0:
        with open(local_path, 'rb') as f:
            return f.read()
    print(f"Downloading {url}...")
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = resp.read()
    with open(local_path, 'wb') as f:
        f.write(data)
    print(f"Saved {local_path} ({len(data)} bytes)")
    return data


def parse_emoji_data(java_source):
    idx = java_source.find('String[][] data =')
    if idx == -1:
        raise ValueError("Could not find 'String[][] data =' in EmojiData.java")
    end_idx = java_source.find('};', idx)
    block = java_source[idx:end_idx]
    pages = []
    parts = block.split('new String[]')[1:]
    for part in parts:
        brace_end = part.find('}')
        if brace_end != -1:
            part = part[:brace_end]
        items = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', part)
        items = [it.encode('utf-8').decode('unicode_escape') if '\\u' in it else it for it in items]
        pages.append(items)
    return pages


def get_telegram_master_sha():
    try:
        req = urllib.request.Request(TELEGRAM_MASTER_COMMIT_URL, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            return data.get('sha', 'master')
    except Exception:
        return 'master'


def sync():
    os.makedirs(CACHE_DIR, exist_ok=True)
    pack_cache = os.path.join(CACHE_DIR, "emoji.pack")
    emoji_data_cache = os.path.join(CACHE_DIR, "EmojiData.java")

    # Fetch sources
    pack_bytes = fetch_file(TELEGRAM_EMOJIPACK_URL, pack_cache)
    emoji_data_text = fetch_file(TELEGRAM_EMOJIDATA_URL, emoji_data_cache).decode('utf-8')
    master_sha = get_telegram_master_sha()

    data_pages = parse_emoji_data(emoji_data_text)
    total_emojis = sum(len(p) for p in data_pages)
    print(f"Parsed {len(data_pages)} pages, total emojis: {total_emojis}")

    reader = EmojiPackReader(pack_bytes)
    print(f"EPK3 reader initialized: total records={reader.count}, emoji_count={reader.emoji_count}")

    # Prepare target directories
    feat_modern_dir = os.path.join(FEATURE_CHAT_ASSETS, "modern")
    core_modern_dir = os.path.join(CORE_DESIGN_ASSETS, "modern")
    os.makedirs(feat_modern_dir, exist_ok=True)
    os.makedirs(core_modern_dir, exist_ok=True)

    mapping = {}
    print("Exporting modern emoji PNGs...")

    for p, page in enumerate(data_pages):
        print(f"Processing page {p} ({len(page)} items)...")
        for i, emoji_char in enumerate(page):
            file_name = f"{p}_{i}.png"
            rel_path = f"assets/emoji/modern/{file_name}"
            mapping[emoji_char] = rel_path

            im = reader.get_emoji_image(p, i)
            if im is None:
                print(f"Warning: could not decode emoji {p}_{i} ({emoji_char})")
                continue

            # Format to 66x66 RGBA with 1px transparent border to match existing assets
            canvas = Image.new('RGBA', (66, 66), (0, 0, 0, 0))
            canvas.paste(im, (1, 1))

            feat_file = os.path.join(feat_modern_dir, file_name)
            core_file = os.path.join(core_modern_dir, file_name)

            canvas.save(feat_file, format='PNG', optimize=False)
            canvas.save(core_file, format='PNG', optimize=False)

    print("Emoji PNG export completed.")

    # Write modern_emoji_map.json
    now_iso = datetime.datetime.now(datetime.timezone.utc).isoformat()
    metadata = {
        "generated_by": "tools/emoji/sync_modern_emoji.py",
        "source_repo": "https://github.com/DrKLO/Telegram",
        "commit_sha": master_sha,
        "generated_at": now_iso,
        "asset_count": reader.count,
        "map": mapping
    }

    json_str = json.dumps(metadata, ensure_ascii=False, indent=2)

    feat_json_path = os.path.join(FEATURE_CHAT_ASSETS, "modern_emoji_map.json")
    core_json_path = os.path.join(CORE_DESIGN_ASSETS, "modern_emoji_map.json")

    with open(feat_json_path, 'w', encoding='utf-8') as f:
        f.write(json_str)
    with open(core_json_path, 'w', encoding='utf-8') as f:
        f.write(json_str)

    print(f"Successfully generated modern_emoji_map.json in {feat_json_path} and {core_json_path}")
    print(f"Total mapped emojis: {len(mapping)}")


if __name__ == '__main__':
    sync()
