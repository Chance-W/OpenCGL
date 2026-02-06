package com.opencgl.model;

/**
 * 插件点击记录（SQLite plugin_click_record 行映射）。
 */
public class PluginClickRecord {

    private String pluginName;
    private Long clickCount;
    private String lastClickAt;

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public Long getClickCount() {
        return clickCount;
    }

    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }

    public String getLastClickAt() {
        return lastClickAt;
    }

    public void setLastClickAt(String lastClickAt) {
        this.lastClickAt = lastClickAt;
    }
}
