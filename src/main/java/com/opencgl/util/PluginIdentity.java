package com.opencgl.util;

import com.opencgl.api.PluginUI;
import java.util.Objects;

/** Resolves the one stable identifier used by every host plugin operation. */
public final class PluginIdentity {
    private PluginIdentity() {
    }

    public static String resolve(PluginUI plugin) {
        Objects.requireNonNull(plugin, "plugin");
        String declaredId = plugin.pluginId();
        return declaredId == null || declaredId.isBlank()
            ? plugin.getClass().getName()
            : declaredId.trim();
    }
}
