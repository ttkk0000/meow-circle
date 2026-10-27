"""Read-only city/source and image checks; does not execute application code or tests."""
from pathlib import Path
import ast
import hashlib
import json
import re
import struct

root = Path(__file__).resolve().parents[1]
catalog = json.loads((root / 'internal/region/mainland-cities.json').read_text(encoding='utf-8'))
assert len(catalog['provinces']) == 31 and len(catalog['cities']) == 745
assert sum(c['name'].endswith('市') for c in catalog['cities']) == 694
assert catalog == json.loads((root / 'web-client/src/core/mainland-cities.json').read_text(encoding='utf-8'))
assert catalog['metadata']['coordinateSystem'] == 'gcj02'
ui = root / 'kmp/clientUi/src/commonMain/kotlin/com/ttkk0000/meowcircle/clientui'
shared = root / 'kmp/shared/src/commonMain/kotlin/com/ttkk0000/meowcircle/client'
assert 'city.contains("上海")' not in (shared / 'LostMap.kt').read_text(encoding='utf-8')
assert 'mainlandCity(city)' in (shared / 'LostMap.kt').read_text(encoding='utf-8')
assert 'country-center' in (shared / 'LostMap.kt').read_text(encoding='utf-8')
assert not any(re.search(r'FormField\(.*?"city"', p.read_text(encoding='utf-8')) for p in ui.glob('*.kt'))
authority = (root / 'internal/mobile/authority.go').read_text(encoding='utf-8')
assert 'region.Lookup(in.City)' in authority and 'region.Normalize(city)' in authority
assert '"武汉", "上海"' not in authority
for guard in ['city_rule_unavailable', 'city_action_unavailable', 'ValidUntil.After', 'HasString(v.Actions, action)']:
    assert guard in authority
for path in ['web-client/src/maps/AmapView.tsx', 'kmp/iosApp/ComposeApp/AmapNativeMap.swift', 'kmp/androidApp/src/main/java/com/ttkk0000/meowcircle/kmpapp/mobile/AndroidNativeMap.kt']:
    assert 'country-center' in (root / path).read_text(encoding='utf-8')

def jpeg_size(data):
    assert data[:2] == b'\xff\xd8'
    offset = 2
    while offset < len(data):
        assert data[offset] == 255
        marker = data[offset + 1]
        length = int.from_bytes(data[offset + 2:offset + 4], 'big')
        if marker in (192, 193, 194):
            height, width = struct.unpack('>HH', data[offset + 5:offset + 9])
            return width, height
        offset += length + 2
    raise AssertionError('JPEG size missing')

hashes = {}
for name in ['garfield-portrait', 'golden-portrait']:
    photo = root / f'web-client/public/assets/cats/{name}.jpg'
    data = photo.read_bytes()
    assert data == (root / f'kmp/clientUi/src/commonMain/composeResources/files/cats/{name}.jpg').read_bytes()
    assert jpeg_size(data) == (960, 960)
    assert b'Exif\x00\x00' not in data and len(data) < 250_000
    hashes[name] = hashlib.sha256(data).hexdigest()
for path in [root / 'scripts/sync_city_catalog.py', Path(__file__)]:
    ast.parse(path.read_text(encoding='utf-8'))
for path in [root / 'internal/region/README.md', root / 'docs/client/MAINLAND_CITIES_AND_CAT_ASSETS_2026_10_26.md']:
    text = path.read_text(encoding='utf-8')
    assert not text.startswith('\ufeff')
    for link in re.findall(r'\]\(([^)]+)\)', text):
        if not link.startswith('https://'):
            assert (path.parent / link.split('#')[0]).exists(), (path, link)
print(json.dumps({'static_only': True, 'provinces': 31, 'cities': 694, 'selectable_regions': 745, 'cat_assets_sha256': hashes}, ensure_ascii=False))
