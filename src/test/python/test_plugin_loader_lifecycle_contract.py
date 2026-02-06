from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[3]
SOURCE = (ROOT / "src/main/java/com/opencgl/util/PluginParserHelper.java").read_text()


class PluginLoaderLifecycleContractTest(unittest.TestCase):
    def test_rejected_service_loader_instances_are_disposed(self):
        self.assertIn("discardPluginInstance(pluginUI", SOURCE)

    def test_overridden_descriptor_instance_is_disposed(self):
        self.assertIn("discardPluginInstance(replacedPlugin", SOURCE)

    def test_unmatched_factory_instances_are_disposed(self):
        self.assertIn("discardPluginInstance(pluginUI, null)", SOURCE)
