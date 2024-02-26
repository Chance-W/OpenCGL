package com.opencgl.util;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.opencgl.listener.Config;
import com.opencgl.model.MenuInfo;
import com.opencgl.model.PluginInfo;
import com.opencgl.model.OpenCGLSelfProperties;

/**
 * @author Chance.W
 * @version 9.0
 * @className PluginParserHelper
 * @description 解析插件文件
 * @date 2022/8/6 17:17
 */
@SuppressWarnings("unused")
public class PluginParserHelper {
    private static final Logger logger = LoggerFactory.getLogger(PluginParserHelper.class);

    public final static List<MenuInfo> MENU_INFOS = new ArrayList<>(32);

    private static final String COMPONENT_NAME = "com/opencgl/config/menuComponent.json";

    public static List<MenuInfo> analysisComponentJson() throws Exception {
        try {
            if (CollectionUtils.isNotEmpty(MENU_INFOS)) {
                //此处优化会导致新增插件包必须重启应用才能生效,减少不必要的重复解析文件
                //  return MENU_INFOS;
                MENU_INFOS.clear();
            }
       /*   InputStream stream = MainController.class.getClassLoader().getResourceAsStream(COMPONENT_NAME);
            assert stream != null;
            String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
             List<MenuInfo>
            MENU_INFOS.addAll(JSONArray.parseArray(content, MenuInfo.class));*/

            File pluginFile = new File(Config.readExternalConfigure(OpenCGLSelfProperties.PLUGIN_PATH_KEY));
            if (!pluginFile.exists()) {
                boolean mkdirs = pluginFile.mkdirs();
                logger.debug("create plugin file result is [{}]", mkdirs);
            }

            for (File file : FileUtils.listFiles(pluginFile, null, false)) {
                if (!file.getName().endsWith(".jar")) {
                    continue;
                }
                List<PluginInfo> pluginInfos = initParse(file);
                pluginInfos.forEach(e -> {
                    MenuInfo menuInfo = MenuInfo.builder()
                        .menuName(e.getPluginName())
                        .enable(e.getEnable())
                        .fxmlPath(e.getFxmlPath())
                        .jarName(file.getName())
                        .iconPath(e.getIconPath())
                        .pluginInfo(e.getPluginInfo())
                        .build();
                    menuInfo.setFatherMenuName(StringUtils.defaultIfEmpty(e.getFatherName(), null));
                    MENU_INFOS.add(menuInfo);
                });


            }

            logger.info("get menu items {}", MENU_INFOS);
            return MENU_INFOS;
        }
        catch (Exception e) {
            logger.error("", e);
            throw e;
        }
    }

    private static List<PluginInfo> initParse(File pluginFile) throws Exception {
        try (JarFile jarFile = new JarFile(pluginFile)) {
            JarEntry entry = jarFile.getJarEntry("plugin-info.json");
            List<PluginInfo> pluginInfos = new ArrayList<>();
            if (entry == null) {
                throw new Exception("插件包不存在或配置不符合规格");
            }
            InputStream input = jarFile.getInputStream(entry);
            Object object = JSONObject.parse(IOUtils.toString(input, StandardCharsets.UTF_8));

            if (object instanceof JSONObject) {
                pluginInfos.add(((JSONObject) object).toJavaObject(PluginInfo.class));
            }
            else if (object instanceof JSONArray) {
                pluginInfos.addAll(((JSONArray) object).toJavaList(PluginInfo.class));
            }
            return pluginInfos;
        }
    }

    public static InputStream getFileInputStream(File pluginFile, String filePath) throws Exception {
        JarFile jarFile = new JarFile(pluginFile);
        JarEntry jarEntry = jarFile.getJarEntry(filePath);
        if (null == jarEntry) {
            return null;
        }
        return jarFile.getInputStream(jarEntry);
    }
}
