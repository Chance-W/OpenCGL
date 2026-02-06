package com.opencgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencgl.api.PluginUI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PluginLifecycleManagerTest {
    @Test
    void disposesEachInstanceOnlyOnce() {
        PluginLifecycleManager manager = new PluginLifecycleManager();
        TestPlugin plugin = new TestPlugin("tools.one", false);
        manager.register(plugin);

        assertTrue(manager.disposeInstance(plugin).successful());
        assertTrue(manager.disposeInstance(plugin).alreadyDisposed());
        assertEquals(1, plugin.disposeCalls.get());
    }

    @Test
    void disposingOnePluginCleansAllItsInstancesOnly() {
        PluginLifecycleManager manager = new PluginLifecycleManager();
        TestPlugin first = new TestPlugin("tools.one", false);
        TestPlugin second = new TestPlugin("tools.one", false);
        TestPlugin other = new TestPlugin("tools.two", false);
        manager.register(first);
        manager.register(second);
        manager.register(other);

        PluginLifecycleManager.CleanupReport report = manager.disposePlugin("tools.one");

        assertEquals(2, report.attempted());
        assertEquals(2, report.succeeded());
        assertEquals(0, report.failed());
        assertEquals(0, other.disposeCalls.get());
        assertEquals(1, manager.registeredInstanceCount());
    }

    @Test
    void oneFailureDoesNotPreventOtherPluginsFromBeingDisposed() {
        PluginLifecycleManager manager = new PluginLifecycleManager();
        TestPlugin broken = new TestPlugin("tools.broken", true);
        TestPlugin healthy = new TestPlugin("tools.healthy", false);
        manager.register(broken);
        manager.register(healthy);

        PluginLifecycleManager.CleanupReport report = manager.disposeAll();

        assertEquals(2, report.attempted());
        assertEquals(1, report.succeeded());
        assertEquals(1, report.failed());
        assertFalse(report.failures().isEmpty());
        assertEquals(1, healthy.disposeCalls.get());
        assertEquals(0, manager.registeredInstanceCount());
    }

    private static final class TestPlugin implements PluginUI {
        private final String id;
        private final boolean fail;
        private final AtomicInteger disposeCalls = new AtomicInteger();

        private TestPlugin(String id, boolean fail) {
            this.id = id;
            this.fail = fail;
        }

        public String pluginId() { return id; }
        public String directoryName() { return "test"; }
        public String name() { return id; }
        public URL iconPath() { return null; }
        public UIType type() { return UIType.JAVAFX; }
        public Object createView() { return null; }

        @Override
        public void dispose() {
            disposeCalls.incrementAndGet();
            if (fail) throw new IllegalStateException("boom");
        }
    }
}
