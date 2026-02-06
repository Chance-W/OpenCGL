package com.opencgl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.opencgl.api.PluginUI;
import java.net.URL;
import org.junit.jupiter.api.Test;

class PluginIdentityTest {
    @Test
    void usesDeclaredStableId() {
        assertEquals("tools.redis", PluginIdentity.resolve(plugin(" tools.redis ")));
    }

    @Test
    void fallsBackToImplementationClassForBlankId() {
        PluginUI plugin = plugin("  ");
        assertEquals(plugin.getClass().getName(), PluginIdentity.resolve(plugin));
    }

    private PluginUI plugin(String id) {
        return new PluginUI() {
            public String pluginId() { return id; }
            public String directoryName() { return "test"; }
            public String name() { return "test"; }
            public URL iconPath() { return null; }
            public UIType type() { return UIType.JAVAFX; }
            public Object createView() { return null; }
        };
    }
}
