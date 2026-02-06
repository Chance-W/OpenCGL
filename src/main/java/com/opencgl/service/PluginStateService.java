package com.opencgl.service;

import com.opencgl.base.listener.Config;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Persists plugin enable/disable state independently from plugin JAR files. */
public class PluginStateService {
    public static final String DISABLED_PLUGIN_IDS_KEY = "disabledPluginIds";

    public Set<String> disabledPluginIds() {
        return parseDisabledPluginIds(Config.readExternalConfigure(DISABLED_PLUGIN_IDS_KEY));
    }

    public boolean isDisabled(String pluginId) {
        return pluginId != null && disabledPluginIds().contains(pluginId);
    }

    public void disable(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) return;
        Set<String> ids = new LinkedHashSet<>(disabledPluginIds());
        ids.add(pluginId.trim());
        save(ids);
    }

    public void enableAll() {
        save(Collections.emptySet());
    }

    public void enable(String pluginId) {
        save(parseDisabledPluginIds(removeDisabledPluginId(
            Config.readExternalConfigure(DISABLED_PLUGIN_IDS_KEY), pluginId)));
    }

    private void save(Set<String> ids) {
        Config.updateSingleConfig(DISABLED_PLUGIN_IDS_KEY, serializeDisabledPluginIds(ids));
    }

    static Set<String> parseDisabledPluginIds(String value) {
        if (value == null || value.isBlank()) return Collections.emptySet();
        return Arrays.stream(value.split(";"))
            .map(String::trim)
            .filter(id -> !id.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static String serializeDisabledPluginIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) return "";
        return new TreeSet<>(ids).stream()
            .filter(id -> id != null && !id.isBlank())
            .map(String::trim)
            .collect(Collectors.joining(";"));
    }

    static String removeDisabledPluginId(String value, String pluginId) {
        Set<String> ids = new LinkedHashSet<>(parseDisabledPluginIds(value));
        if (pluginId != null) ids.remove(pluginId.trim());
        return serializeDisabledPluginIds(ids);
    }
}
