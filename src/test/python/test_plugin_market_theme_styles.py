import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]


class PluginMarketThemeStylesTest(unittest.TestCase):
    def test_market_does_not_use_theme_tokens_in_inline_styles(self):
        paths = (
            ROOT / "src/main/java/com/opencgl/controller/PluginMarketController.java",
            ROOT / "src/main/resources/com/opencgl/view/PluginMarketPane.fxml",
        )
        for path in paths:
            text = path.read_text(encoding="utf-8")
            inline_styles = re.findall(r'(?:setStyle\(|style=")[^\n>]*', text)
            self.assertFalse(
                any("-theme-" in style for style in inline_styles),
                f"theme token found in inline style: {path}",
            )


if __name__ == "__main__":
    unittest.main()
