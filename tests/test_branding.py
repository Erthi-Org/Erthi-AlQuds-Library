from pathlib import Path
import re
import unittest


class BrandingTest(unittest.TestCase):
    def test_android_user_facing_sources_exclude_legacy_branding(self):
        source = Path(__file__).resolve().parents[1] / "app/src/main"
        for path in source.rglob("*"):
            if path.suffix in {".java", ".xml", ".html", ".json"}:
                with self.subTest(path=path.relative_to(source)):
                    self.assertIsNone(
                        re.search(r"nexvary|نيكسفاري", path.read_text(encoding="utf-8"), re.I)
                    )
