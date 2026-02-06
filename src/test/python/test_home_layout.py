import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
FXML = ROOT / "src/main/resources/com/opencgl/view/HomePane.fxml"
CSS = ROOT / "src/main/resources/com/opencgl/css/MainWindow.css"


class HomeLayoutTest(unittest.TestCase):
    def test_home_uses_scrollable_dashboard_without_editor_controls(self):
        source = FXML.read_text()
        ET.parse(FXML)

        self.assertIn("<ScrollPane", source)
        self.assertNotIn("<TextArea", source)
        self.assertNotIn('prefWidth="10000"', source)
        self.assertNotIn('left="-15.0"', source)

    def test_home_declares_clear_dashboard_sections(self):
        source = FXML.read_text()

        for style_class in (
            "home-dashboard",
            "home-hero",
            "home-card",
            "home-tech-chip",
            "home-contact-row",
        ):
            self.assertIn(style_class, source)

    def test_home_styles_are_scoped_and_theme_token_based(self):
        source = CSS.read_text()

        self.assertIn(".rootPane .home-dashboard", source)
        self.assertIn(".rootPane .home-card", source)
        self.assertIn("-theme-bg-secondary", source)
        self.assertIn("-theme-text-secondary", source)
        self.assertNotIn(".rootPane .label {\n    -fx-padding: 0 0 0 10px;\n}", source)


if __name__ == "__main__":
    unittest.main()
