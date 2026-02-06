package com.opencgl.service;

import com.opencgl.base.listener.Config;
import com.opencgl.base.model.OpenCGLSelfProperties;
import com.opencgl.api.PluginUI;
import com.opencgl.i18n.I18N;
import com.opencgl.util.DialogUtil;
import com.opencgl.util.PluginParserHelper;
import com.opencgl.util.TooltipUtil;
import javafx.application.Platform;
import javafx.scene.Node;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 负责插件管理（扫描、安装、加载）的服务
 */
public class PluginService {
    private static final Logger logger = LoggerFactory.getLogger(PluginService.class);

    /**
     * 加载所有插件
     * @return 插件列表
     */
    public List<PluginUI> loadPlugins() {
        return PluginParserHelper.initPluginInfo();
    }

    /**
     * 将插件按目录分类
     * @param pluginUIList 插件列表
     * @return 分类映射
     */
    public Map<String, List<PluginUI>> groupPluginsByCategory(List<PluginUI> pluginUIList) {
        return pluginUIList.stream().collect(Collectors.groupingBy(pluginUI -> {
            if (StringUtils.isNotBlank(pluginUI.directoryName())) {
                return pluginUI.directoryName();
            } else {
                return "其他";
            }
        }));
    }

    /**
     * 安装插件 Jar 包
     * @param jarFile 拖入的 Jar 文件
     * @param rootPane用于显示提示的 UI 根节点
     * @param onSuccess 安装成功后的回调（通常用于刷新列表）
     */
    public void installPlugin(File jarFile, Node rootPane, Runnable onSuccess) {
        String pluginPath = Config.readExternalConfigure(OpenCGLSelfProperties.PLUGIN_PATH_KEY);
        File pluginDirectory = resolveUserPluginDirectory(pluginPath, System.getProperty("user.home"));
        if (!pluginDirectory.isDirectory() && !pluginDirectory.mkdirs()) {
            DialogUtil.showErrorInfo(I18N.get("opencgl.plugin.install.failed"));
            return;
        }
        if (pluginPath == null || pluginPath.isBlank()) {
            Config.updateSingleConfig(OpenCGLSelfProperties.PLUGIN_PATH_KEY, pluginDirectory.getAbsolutePath());
        }
        File destFile = new File(pluginDirectory, jarFile.getName());

        if (destFile.exists()) {
            boolean confirm = DialogUtil.showConfirm(I18N.get("opencgl.plugin.install.exists"), I18N.get("opencgl.plugin.install.confirm_overwrite", jarFile.getName()));
            if (!confirm) {
                return;
            }
        }

        // 使用异步任务执行文件复制
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                FileUtils.copyFile(jarFile, destFile);
                logger.info("已安装插件: {}", jarFile.getName());
                
                Platform.runLater(() -> {
                    TooltipUtil.showToast(rootPane, I18N.get("opencgl.plugin.install.success"));
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });
            } catch (IOException e) {
                logger.error("安装插件失败", e);
                Platform.runLater(() -> DialogUtil.showErrorInfo(I18N.get("opencgl.plugin.install.failed")));
            }
        });
    }

    static File resolveUserPluginDirectory(String configuredPath, String userHome) {
        if (configuredPath != null && !configuredPath.isBlank()) {
            return new File(configuredPath);
        }
        return Path.of(userHome, ".opencgl", "ext-plugin").toFile();
    }

    /**
     * 卸载插件：关闭 ClassLoader 并删除物理 JAR 文件
     * @param pluginUI 插件实力
     * @param rootPane UI 根节点，用于 Toast 提示
     * @param onSuccess 成功回调
     */
    public void uninstallPlugin(PluginUI pluginUI, Node rootPane, Runnable onSuccess) {
        String jarPath = PluginParserHelper.getJarPathForPlugin(pluginUI);
        if (jarPath == null) {
            DialogUtil.showErrorInfo(I18N.get("opencgl.plugin.uninstall.no_file", pluginUI.name()));
            return;
        }

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                // 1. 先释放该插件的全部实例，再关闭 ClassLoader。
                String pluginId = com.opencgl.util.PluginIdentity.resolve(pluginUI);
                PluginLifecycleManager.CleanupReport cleanup =
                    PluginLifecycleManager.getInstance().disposePlugin(pluginId);
                if (cleanup.failed() > 0) {
                    logger.warn("插件清理存在失败项: id={}, failed={}", pluginId, cleanup.failed());
                }

                // 2. 关闭 ClassLoader 并移除映射
                PluginParserHelper.closeClassLoader(jarPath);
                PluginParserHelper.removePluginMapping(pluginUI);

                // 3. 删除物理文件，不依赖 System.gc() 或固定休眠释放资源。
                Files.deleteIfExists(Path.of(jarPath));

                Platform.runLater(() -> {
                    TooltipUtil.showToast(rootPane, I18N.get("opencgl.plugin.uninstall.success", pluginUI.name()));
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                });
            } catch (Exception e) {
                logger.error("卸载插件失败: {}", jarPath, e);
                Platform.runLater(() -> DialogUtil.showErrorInfo(I18N.get("opencgl.plugin.uninstall.failed")));
            }
        });
    }
}
