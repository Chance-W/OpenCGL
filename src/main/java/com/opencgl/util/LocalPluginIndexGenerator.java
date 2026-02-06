package com.opencgl.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.opencgl.base.listener.Config;
import com.opencgl.base.model.OpenCGLSelfProperties;
import com.opencgl.model.RemotePluginInfo;
import com.opencgl.api.PluginUI;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 本地插件索引生成器
 * 用于扫描当前本地已加载的插件，计算 SHA-256 哈希，并生成一个完全符合应用商店标准的 index.json
 */
public class LocalPluginIndexGenerator {
    private static final Logger log = LoggerFactory.getLogger(LocalPluginIndexGenerator.class);

    public static void generateIndexJson(String outputDirPath, Runnable onSuccess, java.util.function.Consumer<String> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                File outDir = new File(outputDirPath);
                if (!outDir.exists()) {
                    outDir.mkdirs();
                }

                // 读取所有已解析拿到的插件（仅取已被系统识别出来的合法载体）
                List<PluginUI> loadedPlugins = PluginParserHelper.initPluginInfo();
                List<RemotePluginInfo> registryList = new ArrayList<>();

                for (PluginUI plugin : loadedPlugins) {
                    try {
                        String jarPath = PluginParserHelper.getJarPathForPlugin(plugin);
                        if (jarPath == null || !new File(jarPath).exists()) {
                            log.warn("无法定位插件 {} 的独立物理 JAR 文件", plugin.name());
                            continue;
                        }

                        File jarFile = new File(jarPath);
                        String sha256 = DigestUtil.sha256Hex(jarFile);

                        RemotePluginInfo info = new RemotePluginInfo();
                        info.setName(plugin.name());
                        info.setVersion(plugin.version());
                        // 打包为 file:/// 并在 Windows 上可能需要确保路径格式规范化
                        String rawPath = jarFile.getAbsolutePath().replace("\\", "/");
                        if (!rawPath.startsWith("/")) {
                            rawPath = "/" + rawPath; // Windows C:/xxx -> /C:/xxx
                        }
                        info.setDownloadUrl("file://" + rawPath);
                        info.setSha256(sha256);
                        // directoryName() 是插件实际覆写的分类方法；category() 为未覆写时的兜底默认值
                        info.setCategory(plugin.directoryName());
                        info.setDescription(plugin.description());
                        
                        // 由于本地生成的本身就是为了当前环境跑，可以设一个低版本门槛，或者暂时置空/写死
                        info.setMinAppVersion("1.0.0");

                        registryList.add(info);
                    } catch (Exception e) {
                        log.error("生成插件 {} 的索引记录时出现异常", plugin.name(), e);
                    }
                }

                File outFile = new File(outDir, "index.json");
                FileUtil.writeUtf8String(JSONUtil.toJsonPrettyStr(registryList), outFile);
                
                log.info("本地源索引构建完毕: {}", outFile.getAbsolutePath());
                if (onSuccess != null) {
                    Platform.runLater(onSuccess);
                }

            } catch (Exception e) {
                log.error("构建本地环境 index.json 时发生全局崩溃: ", e);
                if (onError != null) {
                    Platform.runLater(() -> onError.accept(e.getMessage()));
                }
            }
        });
    }
}
