package com.opencgl.icon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.palexdev.mfxresources.fonts.fontawesome.FontAwesomeSolid;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.opencgl.base.model.Base;
import com.opencgl.base.model.CategoryIconInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 侧边栏分类图标管理器
 * 负责图标的生成、固定、持久化
 *
 * @author Chance.W
 */
public class CategoryIconManager {

    private static final Logger logger = LoggerFactory.getLogger(CategoryIconManager.class);

    private static final Map<String, CategoryIconInfo> iconCache = new HashMap<>();
    private static volatile List<String> availableIcons;
    private static final Random random = new Random();
    private static final File iconConfigFile = new File(new File(Base.BASE_CONF_FILE).getParent(), "category_icons.json");

    /**
     * 加载图标配置
     */
    public static void loadIconConfig() {
        if (!iconConfigFile.exists()) {
            return;
        }
        try {
            String json = FileUtils.readFileToString(iconConfigFile, StandardCharsets.UTF_8);
            if (json != null && !json.isEmpty()) {
                Map<String, CategoryIconInfo> loaded = JSON.parseObject(json, new TypeReference<>() {
                });
                if (loaded != null) {
                    iconCache.putAll(loaded);
                }
            }
        }
        catch (IOException e) {
            logger.error("加载图标配置失败", e);
        }
    }

    /**
     * 获取分类图标
     *
     * @param categoryName 分类名称
     * @return 图标名称
     */
    public static String getIconForCategory(String categoryName) {
        CategoryIconInfo info = iconCache.get(categoryName);
        if (info != null && info.getIconName() != null && !info.getIconName().isEmpty()) {
            // 验证图标是否有效
            if (isValidIcon(info.getIconName())) {
                return info.getIconName();
            }
        }

        // 关键区域加锁，防止多线程为同一个分类生成不同的随机图标
        synchronized (CategoryIconManager.class) {
            // 再次检查，防止等待锁的过程中已被其他线程生成
            info = iconCache.get(categoryName);
            if (info != null && info.getIconName() != null && !info.getIconName().isEmpty() && isValidIcon(info.getIconName())) {
                return info.getIconName();
            }

            String newIcon = generateRandomIcon();
            iconCache.put(categoryName, new CategoryIconInfo(newIcon, false));
            saveIconConfig();
            logger.debug("为分类 [{}] 生成新图标: {}", categoryName, newIcon);
            return newIcon;
        }
    }

    /**
     * 刷新分类图标（除非已固定）
     *
     * @param categoryName 分类名称
     * @return 如果图标已固定返回 false，否则返回 true
     */
    public static synchronized boolean refreshIcon(String categoryName) {
        CategoryIconInfo info = iconCache.get(categoryName);
        if (info != null && info.isPinned()) {
            logger.debug("分类 [{}] 的图标已固定，无法刷新", categoryName);
            return false;
        }

        String newIcon = generateRandomIcon();
        iconCache.put(categoryName, new CategoryIconInfo(newIcon, false));
        saveIconConfig();
        logger.debug("刷新分类 [{}] 图标为: {}", categoryName, newIcon);
        return true;
    }

    /**
     * 获取分类当前的图标（不会生成新的）
     *
     * @param categoryName 分类名称
     * @return 当前图标名称，如果没有则返回 null
     */
    public static String getCurrentIcon(String categoryName) {
        CategoryIconInfo info = iconCache.get(categoryName);
        return info != null ? info.getIconName() : null;
    }

    /**
     * 固定图标
     *
     * @param categoryName 分类名称
     */
    public static synchronized void pinIcon(String categoryName) {
        CategoryIconInfo info = iconCache.get(categoryName);
        if (info != null) {
            info.setPinned(true);
            saveIconConfig();
            logger.debug("固定分类 [{}] 图标", categoryName);
        }
    }

    /**
     * 取消固定图标
     *
     * @param categoryName 分类名称
     */
    public static synchronized void unpinIcon(String categoryName) {
        CategoryIconInfo info = iconCache.get(categoryName);
        if (info != null) {
            info.setPinned(false);
            saveIconConfig();
            logger.debug("取消固定分类 [{}] 图标", categoryName);
        }
    }

    /**
     * 判断图标是否已固定
     *
     * @param categoryName 分类名称
     * @return 是否固定
     */
    public static boolean isIconPinned(String categoryName) {
        CategoryIconInfo info = iconCache.get(categoryName);
        return info != null && info.isPinned();
    }

    /**
     * 保存图标配置
     * (Called from synchronized methods above)
     */
    private static void saveIconConfig() {
        Map<String, CategoryIconInfo> toSave = new HashMap<>(iconCache);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String json = JSON.toJSONString(toSave);
                FileUtils.write(iconConfigFile, json, StandardCharsets.UTF_8);
            }
            catch (IOException e) {
                logger.error("保存图标配置失败", e);
            }
        });
    }

    /**
     * 生成随机图标
     *
     * @return 随机 FontAwesome 图标名称
     */
    private static String generateRandomIcon() {
        initAvailableIcons();
        if (availableIcons == null || availableIcons.isEmpty()) {
            return "fas-question"; // Fallback
        }
        return availableIcons.get(random.nextInt(availableIcons.size()));
    }

    /**
     * 验证图标名称是否有效
     */
    private static boolean isValidIcon(String iconName) {
        initAvailableIcons();
        return availableIcons != null && availableIcons.contains(iconName);
    }

    /**
     * 初始化可用图标列表 (Thread-Safe Lazy Init)
     */
    private static void initAvailableIcons() {
        if (availableIcons == null) {
            synchronized (CategoryIconManager.class) {
                if (availableIcons == null) {
                    List<String> icons = new ArrayList<>();
                    // 使用 FontAwesome Solid 图标
                    Arrays.stream(FontAwesomeSolid.values())
                        .forEach(icon -> icons.add(icon.getDescription()));
                    availableIcons = icons;
                    logger.debug("已加载 {} 个可用图标", availableIcons.size());
                }
            }
        }
    }

    /**
     * 获取图标缓存
     */
    public static Map<String, CategoryIconInfo> getIconCache() {
        return new HashMap<>(iconCache);
    }
}
