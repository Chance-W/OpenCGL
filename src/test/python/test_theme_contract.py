import re
import unittest
from pathlib import Path


HOST_ROOT = Path(__file__).resolve().parents[3]
PLUGIN_ROOT = HOST_ROOT.parent / "OpenCGL-Plugin-New"
BASE_CSS = PLUGIN_ROOT / "OpenCGL-Base/src/main/resources/com/opencgl/base/css"


class ThemeContractTest(unittest.TestCase):
    def test_light_and_dark_define_the_same_semantic_tokens(self):
        token_pattern = re.compile(r"(?m)^\s*(-oc-[a-z0-9-]+)\s*:")
        light = (BASE_CSS / "themes/ThemeTokens-light.css").read_text(encoding="utf-8")
        dark = (BASE_CSS / "themes/ThemeTokens-dark.css").read_text(encoding="utf-8")

        self.assertEqual(set(token_pattern.findall(light)), set(token_pattern.findall(dark)))

    def test_default_theme_uses_neutral_sidebar_and_modern_surface_colors(self):
        css = (BASE_CSS / "themes/ThemeColors-default.css").read_text(encoding="utf-8")

        self.assertIn("-theme-bg-primary: #F5F7FA", css)
        self.assertIn("-theme-bg-secondary: #FFFFFF", css)
        self.assertIn("-theme-sidebar-bg: #FFFFFF", css)
        self.assertIn("-theme-sidebar-selected-bg: #E5F5F2", css)
        self.assertIn("-theme-accent: #0F9D8A", css)

    def test_host_sidebar_is_flat_and_uses_selection_indicator(self):
        css = (HOST_ROOT / "src/main/resources/com/opencgl/css/MainWindow.css").read_text(encoding="utf-8")

        self.assertNotIn("-fx-background-radius: 0 20 20 0", css)
        self.assertRegex(
            css,
            r"sidebar[\s\S]*selected[\s\S]*-fx-border-width:\s*0\s+0\s+0\s+3",
            "missing 3px selected indicator",
        )

    def test_theme_manager_loads_mode_tokens_before_accent(self):
        manager = (
            PLUGIN_ROOT
            / "OpenCGL-Base/src/main/java/com/opencgl/base/theme/ThemeManager.java"
        ).read_text(encoding="utf-8")

        self.assertIn("ThemeTokens-", manager)
        self.assertIn("Accent-", manager)
        self.assertLess(manager.index("ThemeTokens-"), manager.index("Accent-"))

    def test_host_menu_exposes_modes_and_accents(self):
        controller = (
            HOST_ROOT / "src/main/java/com/opencgl/controller/NewMainController.java"
        ).read_text(encoding="utf-8")

        for value in ("ThemeMode.SYSTEM", "ThemeMode.LIGHT", "ThemeMode.DARK"):
            self.assertIn(value, controller)
        for value in (
            "AccentColor.TEAL",
            "AccentColor.BLUE",
            "AccentColor.PURPLE",
            "AccentColor.OCEAN",
        ):
            self.assertIn(value, controller)

    def test_host_css_does_not_use_unsupported_transition_property(self):
        css = (HOST_ROOT / "src/main/resources/com/opencgl/css/MainWindow.css").read_text(encoding="utf-8")

        self.assertNotIn("-fx-transition", css)

    def test_cards_do_not_render_nested_background_blocks(self):
        css = (HOST_ROOT / "src/main/resources/com/opencgl/css/MainWindow.css").read_text(encoding="utf-8")
        home_fxml = (HOST_ROOT / "src/main/resources/com/opencgl/view/HomePane.fxml").read_text(encoding="utf-8")

        self.assertRegex(css, r"\.opencgl-vbox\s*\{[\s\S]*?-fx-background-insets:\s*0;")
        self.assertNotIn("<TextArea", home_fxml)

    def test_plugin_card_flows_own_the_spacing(self):
        for relative in (
            "src/main/resources/com/opencgl/view/GeneralComponentsPane.fxml",
            "src/main/resources/com/opencgl/view/CustomPluginsPane.fxml",
        ):
            fxml = (HOST_ROOT / relative).read_text(encoding="utf-8")
            self.assertRegex(fxml, r"<FlowPane[^>]*hgap=\"12(?:\.0)?\"[^>]*vgap=\"12(?:\.0)?\"")


if __name__ == "__main__":
    unittest.main()
