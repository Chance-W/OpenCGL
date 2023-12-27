package com.opencgl;

import java.io.File;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/05 20:20
 * @since v9.0
 */
public class BaseEnum {

    public static final String ROOT_PATH = System.getProperty("user.home") + File.separator + ".opencgl_new" + File.separator;

    public static final String PLUGIN_PATH = ROOT_PATH + "ext-plugin" + File.separator;

    public static final String BASE_CONF_PATH = ROOT_PATH + "conf/base" + File.separator;

    public static final String BASE_CONF_FILE = BASE_CONF_PATH + "config.json";

    public static final String DEFAULT_ICON_PATH = "com/opencgl/icon/logo.png";

}
