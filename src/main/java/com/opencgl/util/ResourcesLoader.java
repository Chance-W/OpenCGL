package com.opencgl.util;

import java.io.InputStream;
import java.net.URL;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/04 11:11
 * @since v9.0
 */
public class ResourcesLoader {
    private ResourcesLoader() {
    }

    public static URL loadURL(String path) {
        return ResourcesLoader.class.getResource(path);
    }

    public static String load(String path) {
        return loadURL(path).toString();
    }

    public static InputStream loadStream(String name) {
        return ResourcesLoader.class.getResourceAsStream(name);
    }

}
