package com.opencgl.model;

/**
 * 自定义插件条目：名称、URL/路径、图标等，持久化到 Sqlite。
 */
public class CustomPluginEntry {
    private String id;
    private String name;
    private String urlOrPath;
    private String iconPath;
    private Integer sortOrder;
    private String createdAt;

    // 显式 getter/setter，保证编译通过
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrlOrPath() { return urlOrPath; }
    public void setUrlOrPath(String urlOrPath) { this.urlOrPath = urlOrPath; }
    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
