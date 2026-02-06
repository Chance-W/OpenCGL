package com.opencgl.service;

import com.opencgl.api.PluginUI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PluginService}.
 */
@ExtendWith(MockitoExtension.class)
class PluginServiceTest {

    @Mock
    private PluginUI pluginA1;
    @Mock
    private PluginUI pluginA2;
    @Mock
    private PluginUI pluginB1;

    @Test
    void groupPluginsByCategory_groupsByDirectoryName() {
        when(pluginA1.directoryName()).thenReturn("A");
        when(pluginA2.directoryName()).thenReturn("A");
        when(pluginB1.directoryName()).thenReturn("B");

        PluginService service = new PluginService();
        Map<String, List<PluginUI>> grouped = service.groupPluginsByCategory(
                List.of(pluginA1, pluginA2, pluginB1));

        assertEquals(2, grouped.size());
        assertTrue(grouped.containsKey("A"));
        assertTrue(grouped.containsKey("B"));
        assertEquals(2, grouped.get("A").size());
        assertEquals(1, grouped.get("B").size());
    }

    @Test
    void defaultsPluginInstallationToTheUserOpenCglDirectory() {
        assertEquals(
            Path.of("/users/test", ".opencgl", "ext-plugin").toFile(),
            PluginService.resolveUserPluginDirectory("", "/users/test")
        );
    }

    @Test
    void keepsAnExplicitPluginInstallationDirectory() {
        assertEquals(
            Path.of("/plugins/custom").toFile(),
            PluginService.resolveUserPluginDirectory("/plugins/custom", "/users/test")
        );
    }
}
