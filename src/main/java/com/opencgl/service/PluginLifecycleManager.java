package com.opencgl.service;

import com.opencgl.api.PluginUI;
import com.opencgl.util.PluginIdentity;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tracks plugin instances and invokes their existing dispose contract exactly once. */
public final class PluginLifecycleManager {
    private static final Logger logger = LoggerFactory.getLogger(PluginLifecycleManager.class);
    private static final PluginLifecycleManager INSTANCE = new PluginLifecycleManager();

    private final Map<PluginUI, String> registered = new IdentityHashMap<>();

    public static PluginLifecycleManager getInstance() {
        return INSTANCE;
    }

    public synchronized void register(PluginUI plugin) {
        if (plugin != null) {
            registered.put(plugin, PluginIdentity.resolve(plugin));
        }
    }

    public synchronized DisposeResult disposeInstance(PluginUI plugin) {
        if (plugin == null) {
            return new DisposeResult(false, true, null);
        }
        if (!registered.containsKey(plugin)) {
            return new DisposeResult(false, true, null);
        }
        registered.remove(plugin);
        try {
            plugin.dispose();
            return new DisposeResult(true, false, null);
        } catch (Throwable error) {
            logger.error("Failed to dispose plugin: id={}, class={}",
                PluginIdentity.resolve(plugin), plugin.getClass().getName(), error);
            return new DisposeResult(false, false, error);
        }
    }

    public CleanupReport disposePlugin(String pluginId) {
        List<PluginUI> targets;
        synchronized (this) {
            targets = registered.entrySet().stream()
                .filter(entry -> entry.getValue().equals(pluginId))
                .map(Map.Entry::getKey)
                .toList();
        }
        return dispose(targets);
    }

    public CleanupReport disposeAll() {
        List<PluginUI> targets;
        synchronized (this) {
            targets = new ArrayList<>(registered.keySet());
        }
        return dispose(targets);
    }

    public synchronized int registeredInstanceCount() {
        return registered.size();
    }

    private CleanupReport dispose(List<PluginUI> targets) {
        int succeeded = 0;
        Map<String, Throwable> failures = new LinkedHashMap<>();
        for (PluginUI plugin : targets) {
            DisposeResult result = disposeInstance(plugin);
            if (result.successful()) {
                succeeded++;
            } else if (result.failure() != null) {
                failures.put(PluginIdentity.resolve(plugin) + "@" +
                    Integer.toHexString(System.identityHashCode(plugin)), result.failure());
            }
        }
        return new CleanupReport(targets.size(), succeeded, failures.size(), Map.copyOf(failures));
    }

    public record DisposeResult(boolean successful, boolean alreadyDisposed, Throwable failure) {
    }

    public record CleanupReport(int attempted, int succeeded, int failed,
                                Map<String, Throwable> failures) {
    }
}
