package com.opencgl.service;

import cn.hutool.core.util.StrUtil;
import com.opencgl.api.PluginUI;
import com.opencgl.model.PluginUpdateInfo;
import com.opencgl.model.RemotePluginInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 插件版本更新探测服务
 * 负责比对已安装的插件与市场最新插件的差异，推断出哪些插件存在可用更新
 */
public class PluginUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PluginUpdateService.class);

    private PluginMarketService pluginMarketService = new PluginMarketService();

    /**
     * 检查给定的本地插件列表中，哪些存在远端的高版本可供更新。
     * @param localPlugins 当前已加载的本地插件集合
     * @return 存在新版本的插件包装对象集合
     */
    public List<PluginUpdateInfo> checkForUpdates(List<PluginUI> localPlugins) {
        if (localPlugins == null || localPlugins.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 获取远端索引清单
        List<RemotePluginInfo> remotePlugins = pluginMarketService.fetchRemotePlugins();
        if (remotePlugins.isEmpty()) {
            return new ArrayList<>(); // 获取失败或为空时自然没有更新
        }

        // 2. 为了提升比对效率，将远端插件按 name 进行 Map 映射
        Map<String, RemotePluginInfo> remotePluginMap = remotePlugins.stream()
                .filter(rp -> StrUtil.isNotBlank(rp.getName()) && StrUtil.isNotBlank(rp.getVersion()))
                .collect(Collectors.toMap(
                        rp -> rp.getName(),
                        rp -> rp,
                        (existing, replacement) -> {
                            // 若远端数据异常有重复名字，优先保留高版本
                            return compareVersion(replacement.getVersion(), existing.getVersion()) > 0 ? replacement : existing;
                        }
                ));

        List<PluginUpdateInfo> updateableList = new ArrayList<>();

        // 3. 逐个比对版本号
        for (PluginUI localPlugin : localPlugins) {
            String pluginName = localPlugin.getClass().getSimpleName();
            String localVersion = localPlugin.version();

            RemotePluginInfo remoteInfo = remotePluginMap.get(pluginName);
            // 如果远端包含这个插件，且远端版本 > 本地版本
            if (remoteInfo != null && compareVersion(remoteInfo.getVersion(), localVersion) > 0) {
                updateableList.add(new PluginUpdateInfo(localPlugin, remoteInfo));
            }
        }

        return updateableList;
    }

    /**
     * 版本号比较核心方法 (e.g. "1.1.0" vs "1.0.5")
     * @param v1 第一个版本号
     * @param v2 第二个版本号
     * @return 正数代表 v1 > v2，负数代表 v1 < v2，0代表相等
     */
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
        return 0; // completely matching
    }

    private int parseVersionSegment(String segment) {
        try {
            // Remove non-numeric characters like "v", "-beta" etc. loosely for basic comparison
            String clean = segment.replaceAll("[^\\d]", "");
            return clean.isEmpty() ? 0 : Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
