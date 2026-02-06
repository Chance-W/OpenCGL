package com.opencgl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PluginStateServiceTest {

    @Test
    void parsesAndNormalizesDisabledPluginIds() {
        assertEquals(Set.of("alpha.plugin", "beta.plugin"),
            PluginStateService.parseDisabledPluginIds(" beta.plugin ;alpha.plugin; beta.plugin;;"));
    }

    @Test
    void serializesIdsDeterministically() {
        assertEquals("alpha.plugin;beta.plugin",
            PluginStateService.serializeDisabledPluginIds(
                new LinkedHashSet<>(java.util.List.of("beta.plugin", "alpha.plugin"))));
    }

    @Test
    void removesOnlyTheRequestedDisabledPlugin() {
        assertEquals("alpha.plugin;gamma.plugin",
            PluginStateService.removeDisabledPluginId(
                "gamma.plugin;beta.plugin;alpha.plugin", "beta.plugin"));
    }
}
