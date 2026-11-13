import hashlib
import json
from pathlib import Path
import struct
import unittest

ROOT = Path(__file__).resolve().parents[1]


class IconAssetsTests(unittest.TestCase):
    def test_bundled_color_icons_match_pinned_source_and_valid_png_dimensions(self):
        registry = json.loads((ROOT / 'third_party/fluent-emoji/assets.json').read_text())
        self.assertEqual('MIT', registry['license'])
        for asset in registry['assets']:
            with self.subTest(path=asset['file']):
                data = (ROOT / asset['file']).read_bytes()
                self.assertEqual(asset['sha256'], hashlib.sha256(data).hexdigest())
                self.assertIn(registry['commit'], asset['url'])
                self.assertEqual(b'\x89PNG\r\n\x1a\n', data[:8])
                width, height = struct.unpack('>II', data[16:24])
                self.assertTrue(16 <= width <= 1024 and 16 <= height <= 1024)


if __name__ == '__main__':
    unittest.main()
