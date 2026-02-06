package com.opencgl.model;

public class RemotePluginInfo {

    private String name;           // 插件英文/主类标识符
    private String version;        // 当前远程提供的版本号 (e.g. "1.1.0")
    private String downloadUrl;    // Jar 包下载直连
    private String sha256;         // 安装包防篡改校验和
    private String minAppVersion;  // 运行该插件需要的客户端最低版本
    private String category;       // 插件分类
    private String description;    // 插件描述

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public String getMinAppVersion() { return minAppVersion; }
    public void setMinAppVersion(String minAppVersion) {
        this.minAppVersion = minAppVersion;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
