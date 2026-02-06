package com.opencgl.model;

import com.opencgl.api.PluginUI;

public class PluginUpdateInfo {

    private final PluginUI localPlugin;         // 本地加载的插件实例引用
    private final RemotePluginInfo remoteInfo;  // 探测到的对应高资源版远端对象

    public PluginUpdateInfo(PluginUI localPlugin, RemotePluginInfo remoteInfo) {
        this.localPlugin = localPlugin;
        this.remoteInfo = remoteInfo;
    }

    public PluginUI getLocalPlugin() { return localPlugin; }
    public RemotePluginInfo getRemoteInfo() { return remoteInfo; }
}
