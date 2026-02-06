package com.opencgl.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.base.listener.Config;
import com.opencgl.base.model.OpenCGLSelfProperties;
import com.opencgl.api.PluginI18n;
import com.opencgl.api.PluginUI;
import com.opencgl.i18n.I18N;
import com.opencgl.service.PluginStateService;
import com.opencgl.service.PluginLifecycleManager;

/**
 * 插件解析助手类
 * 负责扫描、加载和管理插件的 ClassLoader
 * 
 * @author Chance.W
 * @version 9.0
 * @date 2022/8/6 17:17
 */
public class PluginParserHelper {
    private static final Logger logger = LoggerFactory.getLogger(PluginParserHelper.class);

    /**
     * ClassLoader 缓存，避免重复创建和内存泄漏
     * Key: JAR 文件绝对路径
     * Value: 对应的 URLClassLoader
     */
    private static final Map<String, URLClassLoader> classLoaderCache = new ConcurrentHashMap<>();

    /**
     * 插件实例到 JAR 路径的映射，用于卸载时找到对应的 ClassLoader
     * Key: PluginUI 实例
     * Value: JAR 文件绝对路径
     */
    private static final Map<PluginUI, String> pluginToJarMap = new ConcurrentHashMap<>();

    /**
     * 插件名称到 JAR 路径的映射，用于创建新实例时查找 ClassLoader
     * Key: 插件完整类名 (PluginUI.getClass().getName())
     * Value: JAR 文件绝对路径
     */
    private static final Map<String, String> pluginIdToJarMap = new ConcurrentHashMap<>();

    /**
     * 最近一次加载中失败的 JAR 及原因（JAR 文件名 -> 异常信息），用于界面提示
     */
    private static final Map<String, String> lastLoadFailures = new ConcurrentHashMap<>();

    /** 最近一次扫描成功加载的插件信息，按稳定插件 ID 保存。 */
    private static final Map<String, PluginLoadInfo> lastLoadedPlugins = new ConcurrentHashMap<>();
    private static final Map<String, String> lastDisabledPlugins = new ConcurrentHashMap<>();

    public enum PluginSource { BUNDLED, USER }

    public record PluginLoadInfo(String pluginId, String name, String version, String jarPath,
                                 PluginSource source, String replacedJarPath) {
        public boolean overridesBundledPlugin() {
            return replacedJarPath != null && !replacedJarPath.isBlank();
        }

        public boolean uninstallable() {
            return source == PluginSource.USER;
        }
    }

    /**
     * 获取最近一次 initPluginInfo 中加载失败的 JAR 及原因（只读）
     */
    public static Map<String, String> getLastLoadFailures() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(lastLoadFailures));
    }

    public static Map<String, PluginLoadInfo> getLastLoadedPlugins() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(lastLoadedPlugins));
    }

    public static Map<String, String> getLastDisabledPlugins() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(lastDisabledPlugins));
    }

    /** 插件目录无效时的特殊键，用于 UI 提示「请在设置中配置有效路径」 */
    public static final String KEY_PLUGIN_PATH_INVALID = "__plugin_path_invalid";

    /**
     * 扫描并加载所有插件
     *
     * @return 加载成功的 PluginUI 列表
     */
    public static List<PluginUI> initPluginInfo() {
        lastLoadFailures.clear();
        lastLoadedPlugins.clear();
        lastDisabledPlugins.clear();
        PluginStateService pluginStateService = new PluginStateService();
        String pathStr = Config.readExternalConfigure(OpenCGLSelfProperties.PLUGIN_PATH_KEY);
        List<File> directories = resolvePluginDirectories(pathStr, System.getProperty("jpackage.app-path"));
        if (directories.isEmpty()) {
            lastLoadFailures.put(KEY_PLUGIN_PATH_INVALID, "empty");
            return new ArrayList<>();
        }
        Map<String, PluginUI> pluginsById = new LinkedHashMap<>();
        Map<String, String> pathsById = new LinkedHashMap<>();
        Map<String, PluginLoadInfo> reportsById = new LinkedHashMap<>();
        File configuredDirectory = validConfiguredDirectory(pathStr);
        for (File directory : directories) {
            PluginSource source = sameDirectory(directory, configuredDirectory)
                ? PluginSource.USER : PluginSource.BUNDLED;
            File[] files = directory.listFiles(file -> file.getName().endsWith(".jar"));
            if (files == null) {
                lastLoadFailures.put(directory.getAbsolutePath(), "not_readable");
                continue;
            }
            Arrays.sort(files, java.util.Comparator.comparing(File::getName));
            for (File file : files) {
                    try {
                        String jarPath = file.getAbsolutePath();
                        URLClassLoader cl = classLoaderCache.computeIfAbsent(jarPath, key -> {
                            try {
                                return new URLClassLoader(new URL[] { file.toPath().toUri().toURL() },
                                        PluginParserHelper.class.getClassLoader());
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to create ClassLoader for: " + key, e);
                            }
                        });
                        ServiceLoader<PluginUI> sl = ServiceLoader.load(PluginUI.class, cl);
                        for (PluginUI pluginUI : sl) {
                            // ServiceLoader has already constructed the object. Track it before
                            // any filtering so rejected descriptors are not silently leaked.
                            PluginLifecycleManager.getInstance().register(pluginUI);
                            String hostVersion = Config.readInternalConfigure(OpenCGLSelfProperties.CURRENT_VERSION_KEY);
                            PluginCompatibility.Result compatibility = PluginCompatibility.check(
                                pluginUI, hostVersion == null ? "0.0.0" : hostVersion);
                            if (!compatibility.compatible()) {
                                lastLoadFailures.put(file.getName(), "incompatible: " + compatibility.reason());
                                logger.warn("Skipping incompatible plugin {}: {}", file.getName(), compatibility.reason());
                                discardPluginInstance(pluginUI, jarPath);
                                continue;
                            }
                            String pluginId = PluginIdentity.resolve(pluginUI);
                            if (pluginStateService.isDisabled(pluginId)) {
                                lastDisabledPlugins.put(pluginId, pluginUI.name());
                                logger.info("Skipping disabled plugin: {} ({})", pluginUI.name(), pluginId);
                                discardPluginInstance(pluginUI, jarPath);
                                continue;
                            }
                            logger.info("init plugin: {} ({})", pluginUI.name(), pluginId);
                            // 目录顺序为内置、用户；后加载的用户插件覆盖同实现类的内置版本。
                            String replacedJarPath = pathsById.get(pluginId);
                            PluginUI replacedPlugin = pluginsById.put(pluginId, pluginUI);
                            if (replacedPlugin != null && replacedPlugin != pluginUI) {
                                discardPluginInstance(replacedPlugin, replacedJarPath);
                            }
                            pathsById.put(pluginId, jarPath);
                            reportsById.put(pluginId, new PluginLoadInfo(
                                pluginId, pluginUI.name(), pluginUI.version(), jarPath, source, replacedJarPath));
                        }
                    } catch (Throwable t) {
                        // 捕获 Error（如 ServiceConfigurationError）和 Exception，单 JAR 失败不影响其他插件
                        String jarName = file.getName();
                        String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                        lastLoadFailures.put(jarName, msg);
                        logger.warn("Failed to load plugin from file: {} - {}", jarName, msg, t);
                    }
            }
        }
        pluginToJarMap.clear();
        pluginIdToJarMap.clear();
        lastLoadedPlugins.putAll(reportsById);
        pluginsById.forEach((pluginId, plugin) -> {
            String jarPath = pathsById.get(pluginId);
            pluginToJarMap.put(plugin, jarPath);
            pluginIdToJarMap.put(pluginId, jarPath);
            if (plugin instanceof PluginI18n pluginI18n) {
                I18N.registerPlugin(pluginI18n);
            }
        });
        return new ArrayList<>(pluginsById.values());
    }

    /**
     * Resolve the plugin directory without hard-coding one operating-system layout.
     * A valid user directory always wins; packaged plugins are only a fallback for
     * new installations or stale configuration.
     */
    static File resolvePluginDirectory(String configuredPath, String applicationPath) {
        List<File> directories = resolvePluginDirectories(configuredPath, applicationPath);
        return directories.isEmpty() ? null : directories.get(directories.size() - 1);
    }

    static List<File> resolvePluginDirectories(String configuredPath, String applicationPath) {
        List<File> directories = new ArrayList<>();
        File bundled = findBundledPluginDirectory(applicationPath);
        if (bundled != null) {
            directories.add(bundled);
        }
        if (configuredPath != null && !configuredPath.isBlank()) {
            File configured = new File(configuredPath);
            if (configured.isDirectory() && !directories.contains(configured)) {
                directories.add(configured);
            }
        }
        return directories;
    }

    private static File validConfiguredDirectory(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) return null;
        File configured = new File(configuredPath);
        return configured.isDirectory() ? configured : null;
    }

    private static boolean sameDirectory(File left, File right) {
        if (left == null || right == null) return false;
        try {
            return left.getCanonicalFile().equals(right.getCanonicalFile());
        } catch (IOException ignored) {
            return left.getAbsoluteFile().equals(right.getAbsoluteFile());
        }
    }

    private static File findBundledPluginDirectory(String applicationPath) {
        if (applicationPath != null && !applicationPath.isBlank()) {
            Path executable = Path.of(applicationPath).toAbsolutePath();
            Path parent = executable.getParent();
            if (parent != null) {
                List<Path> candidates = new ArrayList<>();
                candidates.add(parent.resolve("app/ext-plugin")); // Windows
                Path installation = parent.getParent();
                if (installation != null) {
                    candidates.add(installation.resolve("app/ext-plugin")); // macOS Contents/app
                    candidates.add(installation.resolve("lib/app/ext-plugin")); // Linux
                }
                for (Path candidate : candidates) {
                    if (candidate.toFile().isDirectory()) {
                        return candidate.toFile();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 清理所有缓存的 ClassLoader（用于热重载或应用关闭时）
     */
    public static void closeAllClassLoaders() {
        PluginLifecycleManager.CleanupReport cleanup =
            PluginLifecycleManager.getInstance().disposeAll();
        logger.info("Plugin cleanup completed: attempted={}, succeeded={}, failed={}",
            cleanup.attempted(), cleanup.succeeded(), cleanup.failed());
        I18N.clearRegisteredPlugins();
        lastLoadFailures.clear();
        lastLoadedPlugins.clear();
        lastDisabledPlugins.clear();
        classLoaderCache.forEach((path, cl) -> {
            try {
                cl.close();
                logger.debug("Closed ClassLoader for: {}", path);
            } catch (IOException e) {
                logger.error("Failed to close ClassLoader for: {}", path, e);
            }
        });
        classLoaderCache.clear();
        pluginToJarMap.clear();
        pluginIdToJarMap.clear();
        logger.info("All ClassLoaders closed, cache cleared");
    }

    /**
     * 清理指定插件的 ClassLoader（用于单个插件热更新）
     * 
     * @param jarPath 插件 JAR 文件的绝对路径
     */
    public static void closeClassLoader(String jarPath) {
        URLClassLoader cl = classLoaderCache.remove(jarPath);
        if (cl != null) {
            try {
                cl.close();
                logger.info("Closed ClassLoader for: {}", jarPath);
            } catch (IOException e) {
                logger.error("Failed to close ClassLoader for: {}", jarPath, e);
            }
        }
    }

    /**
     * 获取缓存的 ClassLoader 数量（用于调试）
     */
    public static int getCachedClassLoaderCount() {
        return classLoaderCache.size();
    }

    /**
     * 获取插件对应的 JAR 路径
     * 
     * @param pluginUI 插件实例
     * @return JAR 文件绝对路径，如果未找到返回 null
     */
    public static String getJarPathForPlugin(PluginUI pluginUI) {
        return pluginToJarMap.get(pluginUI);
    }

    /**
     * 移除插件映射（在插件卸载后调用）
     * 
     * @param pluginUI 插件实例
     */
    public static void removePluginMapping(PluginUI pluginUI) {
        String jarPath = pluginToJarMap.remove(pluginUI);
        if (jarPath != null) {
            logger.debug("Removed plugin mapping: {} -> {}", pluginUI.name(), jarPath);
        }
    }

    /**
     * 根据插件标识（类名）创建新的 PluginUI 实例（用于支持多 Tab 同时打开）
     * 
     * @param pluginId 插件完整类名
     * @return 新创建的 PluginUI 实例，如果未找到返回 null
     */
    public static PluginUI createPluginInstance(String pluginId) {
        String jarPath = pluginIdToJarMap.get(pluginId);
        if (jarPath == null) {
            logger.warn("Plugin ID not found in mapping: {}", pluginId);
            return null;
        }

        URLClassLoader cl = classLoaderCache.get(jarPath);
        if (cl == null) {
            logger.error("ClassLoader not found for JAR: {}", jarPath);
            return null;
        }

        try {
            ServiceLoader<PluginUI> sl = ServiceLoader.load(PluginUI.class, cl);
            // 找到对应的类实例
            for (PluginUI pluginUI : sl) {
                PluginLifecycleManager.getInstance().register(pluginUI);
                if (PluginIdentity.resolve(pluginUI).equals(pluginId)) {
                    pluginToJarMap.put(pluginUI, jarPath); // 记录新实例
                    // 注册国际化监听
                    if (pluginUI instanceof PluginI18n) {
                        I18N.registerPlugin((PluginI18n) pluginUI);
                    }
                    logger.debug("Created new instance of plugin: {}", pluginId);
                    return pluginUI;
                }
                // ServiceLoader constructs every provider it iterates over. Providers that do
                // not match the requested ID must be disposed immediately.
                discardPluginInstance(pluginUI, null);
            }
            logger.warn("Plugin implementation not found in JAR: {}", pluginId);
            return null;
        } catch (Exception e) {
            logger.error("Failed to create plugin instance: {}", pluginId, e);
            return null;
        }
    }

    private static void discardPluginInstance(PluginUI pluginUI, String jarPath) {
        PluginLifecycleManager.DisposeResult result =
            PluginLifecycleManager.getInstance().disposeInstance(pluginUI);
        pluginToJarMap.remove(pluginUI);
        if (result.failure() != null) {
            logger.warn("Discarded plugin instance failed to dispose: jar={}, class={}",
                jarPath, pluginUI.getClass().getName(), result.failure());
        }
    }
}
