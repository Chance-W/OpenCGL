package com.opencgl.listener;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.opencgl.BaseEnum.BASE_CONF_FILE;
import static com.opencgl.BaseEnum.PLUGIN_PATH;
import com.alibaba.fastjson.JSON;
import com.opencgl.BaseEnum;
import com.opencgl.model.Properties;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/19 17:40
 * @since v9.0
 */
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    private static final Map<String, String> configMap = new HashMap<>();
    private static final File file = new File(BaseEnum.BASE_CONF_FILE);

    static {
        writeDefaultConfig();
    }

    public static String readConfigure(String configKey) {
        return configMap.get(configKey);
    }

    public static void updateConfig(Map<String, String> map) throws IOException {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            if (!map.get(entry.getKey()).equals(entry.getValue())) {
                configMap.putAll(map);
                FileUtils.write(file, JSON.toJSONString(configMap), StandardCharsets.UTF_8);
            }
        }
    }

    private static void writeDefaultConfig() {
        File file = new File(BASE_CONF_FILE);
        try {
            if (!file.getParentFile().exists()) {
                boolean res = file.getParentFile().mkdirs();
                logger.debug("create file is {}", res);
            }
            if (!file.exists()) {
                boolean mkdirs = file.createNewFile();
                if (mkdirs) {
                    configMap.put(Properties.PLUGIN_PATH_KEY, PLUGIN_PATH);
                    FileUtils.write(file, JSON.toJSONString(configMap), StandardCharsets.UTF_8);
                }
            }

            String jsonString = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            configMap.putAll(JSON.parseObject(jsonString, Map.class));
        }
        catch (IOException e) {
            logger.error("", e);
        }
    }
}
