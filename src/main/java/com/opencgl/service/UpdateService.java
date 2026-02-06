package com.opencgl.service;

import com.opencgl.base.listener.Config;
import com.opencgl.base.model.OpenCGLSelfProperties;
import com.opencgl.util.DialogUtil;
import com.opencgl.i18n.I18N;
import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * 负责检查应用更新的服务
 */
public class UpdateService {
    private static final Logger logger = LoggerFactory.getLogger(UpdateService.class);

    /**
     * 检查更新
     * @param isManualCheck 是否为手动检查（如果是手动检查，无更新时会弹窗提示）
     * @return CompletableFuture<Void> 异步任务
     */
    public CompletableFuture<Void> checkUpdate(boolean isManualCheck) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(Config.readInternalConfigure(OpenCGLSelfProperties.DEFAULT_UPGRADE_URL)))
                .GET()
                .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            byte[] fileContent = response.body();
                            Properties properties = new Properties();
                            properties.load(new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8));

                            String version = properties.getProperty("version");
                            String description = properties.getProperty("description");
                            String downloadAddress = properties.getProperty("downloadAddress");
                            String currentVersion = Config.readInternalConfigure(OpenCGLSelfProperties.CURRENT_VERSION_KEY);

                            Platform.runLater(() -> {
                                if (compareVersions(currentVersion, version) < 0) {
                                    DialogUtil.showCustomTextInfo(I18N.getOrDefault("opencgl.main.info.upgradeInfo"), description, downloadAddress);
                                } else {
                                    if (isManualCheck) {
                                        DialogUtil.showCustomTextInfo(I18N.getOrDefault("opencgl.main.info.noNeedUpgradeInfo"));
                                    }
                                }
                            });
                        } catch (Exception e) {
                            logger.error("Error parsing update info", e);
                            handleError(isManualCheck, e);
                            throw new RuntimeException(e);
                        }
                    } else {
                        logger.error("Failed to retrieve updates. Server returned status code: {}", response.statusCode());
                        if (isManualCheck) {
                            String msg = I18N.getOrDefault("opencgl.update.error.serverCode", String.valueOf(response.statusCode()));
                            Platform.runLater(() -> DialogUtil.showErrorInfo(msg));
                            throw new RuntimeException(msg);
                        }
                    }
                })
                .exceptionally(e -> {
                    logger.error("Update check failed", e);
                    handleError(isManualCheck, e);
                    throw new RuntimeException(e);
                });
        } catch (URISyntaxException e) {
            logger.error("Invalid URI", e);
            handleError(isManualCheck, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void handleError(boolean isManualCheck, Throwable e) {
        if (isManualCheck) {
            Platform.runLater(() -> DialogUtil.showErrorInfo(I18N.getOrDefault("opencgl.update.error.checkFailed") + ": " + e.getMessage()));
        }
    }

    /**
     * 语义化版本比较
     * @return 负数表示 v1 < v2, 正数表示 v1 > v2, 0 表示相等
     */
    private int compareVersions(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return v1 == null ? (v2 == null ? 0 : -1) : 1;
        }
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }

    /**
     * 解析版本号部分，处理如 "24-ea" 这样的版本
     */
    private int parseVersionPart(String part) {
        try {
            String numPart = part.split("-")[0];
            return Integer.parseInt(numPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
