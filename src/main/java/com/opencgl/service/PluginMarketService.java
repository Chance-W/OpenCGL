package com.opencgl.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.opencgl.model.RemotePluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.base.listener.Config;
import com.opencgl.base.model.OpenCGLSelfProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 插件市场服务
 * 负责从远端同步可用的插件列表及下载所需配置
 */
public class PluginMarketService {

    private static final Logger log = LoggerFactory.getLogger(PluginMarketService.class);

    // 默认的远端插件索引清单 URL 预留点
    private static final String DEFAULT_REGISTRY_URL = "https://upgrade.tool-graphical.top/OpenCGL/index.json";

    /**
     * 同步获取远端的插件大全列表并实现多源仲裁合并
     * @return 远端插件信息集合，包含所有源中取版本最高去重后的结果
     */
    public List<RemotePluginInfo> fetchRemotePlugins() {
        List<String> urls = getRegistryUrls();
        List<RemotePluginInfo> allFetchedPlugins = new ArrayList<>();

        // 遍历所有的源地址获取数据
        for (String url : urls) {
            try {
                String jsonContent = null;
                url = convertToRawUrl(url); // 自动转换由于用户误填的 Web Blob 网址到 Raw 真实数据地址
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    jsonContent = HttpUtil.get(url, 5000); // 5秒线上超时
                } else {
                    // 脱机本地源解析
                    String cleanPath = url;
                    if (url.toLowerCase().startsWith("file:///")) {
                        cleanPath = url.substring(7); // 转换 POSIX 或者 C:/
                    } else if (url.toLowerCase().startsWith("file:/")) {
                        cleanPath = url.substring(5);
                    }
                    java.io.File localFile = new java.io.File(cleanPath);
                    if (localFile.exists() && localFile.isFile()) {
                        jsonContent = cn.hutool.core.io.FileUtil.readUtf8String(localFile);
                    } else {
                        log.warn("本地市场源配置文件未找到或无法读取: {}", cleanPath);
                    }
                }
                
                if (jsonContent != null && !jsonContent.isBlank()) {
                    try {
                        cn.hutool.json.JSONArray jsonArray = JSONUtil.parseArray(jsonContent);
                        List<RemotePluginInfo> list = JSONUtil.toList(jsonArray, RemotePluginInfo.class);
                        if (list != null) {
                            allFetchedPlugins.addAll(list);
                            log.info("成功从源 {} 拉取数据，共 {} 项", url, list.size());
                        }
                    } catch (Throwable parseEx) {
                        log.error("解析远端源 {} 的 JSON 数据失败: {}", url, parseEx.getMessage());
                    }
                }
            } catch (Throwable e) {
                log.error("尝试拉取远端源 {} 发生异常: {}", url, e.getMessage());
            }
        }

        if (allFetchedPlugins.isEmpty()) {
            return Collections.emptyList();
        }

        // 合并所有源中的同名插件，保留版本号最高的
        Map<String, RemotePluginInfo> mergedMap = allFetchedPlugins.stream()
                .filter(p -> p.getName() != null && p.getVersion() != null)
                .collect(Collectors.toMap(
                        RemotePluginInfo::getName,
                        p -> p,
                        (existing, replacement) -> compareVersion(replacement.getVersion(), existing.getVersion()) > 0 ? replacement : existing
                ));

        return new ArrayList<>(mergedMap.values());
    }

    /**
     * 获取用户配置的注册表URL。如果为空，则返回默认源。
     */
    private List<String> getRegistryUrls() {
        String savedUrls = Config.readExternalConfigure(OpenCGLSelfProperties.REGISTRY_URLS_KEY);
        List<String> urls = new ArrayList<>();
        if (savedUrls != null && !savedUrls.trim().isEmpty()) {
            String[] arr = savedUrls.split(",");
            for (String u : arr) {
                if (!u.trim().isEmpty()) {
                    urls.add(u.trim());
                }
            }
        }
        if (urls.isEmpty()) {
            urls.add(DEFAULT_REGISTRY_URL);
        }
        return urls;
    }

    private int compareVersion(String v1, String v2) {
        if (v1 == null) v1 = "";
        if (v2 == null) v2 = "";
        String[] arr1 = v1.split("\\.");
        String[] arr2 = v2.split("\\.");
        int length = Math.max(arr1.length, arr2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < arr1.length ? parseVersionSegment(arr1[i]) : 0;
            int num2 = i < arr2.length ? parseVersionSegment(arr2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    private int parseVersionSegment(String segment) {
        try {
            String clean = segment.replaceAll("[^\\d]", "");
            return clean.isEmpty() ? 0 : Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String convertToRawUrl(String url) {
        if (url == null) return url;
        // Gitee: https://gitee.com/user/repo/blob/branch/path -> https://gitee.com/user/repo/raw/branch/path
        if (url.contains("gitee.com") && url.contains("/blob/")) {
            return url.replace("/blob/", "/raw/");
        }
        // GitHub: https://github.com/user/repo/blob/branch/path -> https://raw.githubusercontent.com/user/repo/branch/path
        if (url.contains("github.com") && url.contains("/blob/")) {
            return url.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/");
        }
        return url;
    }
}
