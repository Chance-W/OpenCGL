package com.opencgl.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencgl.api.PluginUI;
import java.net.URL;
import org.junit.jupiter.api.Test;

class PluginCompatibilityTest {

    @Test
    void acceptsLegacyPluginDefaults() {
        assertTrue(PluginCompatibility.check(plugin("1", "", ""), "v2.2.3-20260907").compatible());
    }

    @Test
    void rejectsNewerApiAndOutOfRangeHostVersions() {
        assertFalse(PluginCompatibility.check(plugin("2", "", ""), "2.2.3").compatible());
        assertFalse(PluginCompatibility.check(plugin("1", "3.0.0", ""), "2.2.3").compatible());
        assertFalse(PluginCompatibility.check(plugin("1", "", "2.0.0"), "2.2.3").compatible());
    }

    @Test
    void rejectsMalformedVersionDeclarationsWithActionableReasons() {
        PluginCompatibility.Result badApi = PluginCompatibility.check(plugin("banana", "", ""), "2.2.3");
        assertFalse(badApi.compatible());
        assertEquals("invalid plugin API version: banana", badApi.reason());

        PluginCompatibility.Result badMinimum = PluginCompatibility.check(plugin("1", "next", ""), "2.2.3");
        assertFalse(badMinimum.compatible());
        assertEquals("invalid minimum OpenCGL version: next", badMinimum.reason());

        PluginCompatibility.Result badHost = PluginCompatibility.check(plugin("1", "2.0", ""), "snapshot");
        assertFalse(badHost.compatible());
        assertEquals("invalid host OpenCGL version: snapshot", badHost.reason());
    }

    private PluginUI plugin(String apiVersion, String minimumHost, String maximumHost) {
        return new PluginUI() {
            public String pluginId() { return "test.plugin"; }
            public String apiVersion() { return apiVersion; }
            public String minimumHostVersion() { return minimumHost; }
            public String maximumHostVersion() { return maximumHost; }
            public String directoryName() { return "test"; }
            public String name() { return "test"; }
            public URL iconPath() { return null; }
            public UIType type() { return UIType.JAVAFX; }
            public Object createView() { return null; }
        };
    }
}
