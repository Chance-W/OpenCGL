package com.opencgl.controller.adapter;

import java.net.URL;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.api.PluginUI;
import com.opencgl.api.WebPluginWithBridge;

import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * 插件视图适配器
 * 负责将不同类型的插件 UI 适配为 JavaFX Node
 *
 * @author Chance.W
 * @since v2.0
 */
public class PluginViewAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PluginViewAdapter.class);

    /**
     * 将插件 UI 适配为 JavaFX Node
     *
     * @param pluginUI 插件 UI 实例
     * @return 适配后的 JavaFX Node
     */
    public static Node adaptToFx(PluginUI pluginUI) {
        if (pluginUI == null) {
            return new Label("Empty view");
        }

        return switch (pluginUI.type()) {
            case JAVAFX -> adaptJavaFX(pluginUI);
            case SWING -> adaptSwing(pluginUI);
            case WEB -> adaptWeb(pluginUI);
        };
    }

    /**
     * 适配 JavaFX 类型插件。
     * 使用插件 ClassLoader 作为当前线程上下文，避免插件内第三方库（如 RocketMQ、fastjson 反序列化）
     * 在子线程中加载类时因上下文仍是主程序 ClassLoader 而出现 ClassNotFoundException。
     */
    private static Node adaptJavaFX(PluginUI pluginUI) {
        ClassLoader pluginCl = pluginUI.getClass().getClassLoader();
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(pluginCl);
            return (Node) pluginUI.createView();
        }
        finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }

    /**
     * 适配 Swing 类型插件
     */
    private static Node adaptSwing(PluginUI pluginUI) {
        ClassLoader pluginCl = pluginUI.getClass().getClassLoader();
        SwingNode swingNode = new SwingNode();
        Platform.runLater(() -> {
            ClassLoader prev = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(pluginCl);
                swingNode.setContent((JComponent) pluginUI.createView());
            }
            finally {
                Thread.currentThread().setContextClassLoader(prev);
            }
        });
        return swingNode;
    }

    /**
     * 适配 Web 类型插件。
     * createView() 可返回 String（HTML 内容或 URL 字符串）或 URL。
     * 若插件实现 WebPluginWithBridge，则在页面加载完成后将 getJsBridge() 暴露为 window.javaBridge。
     */
    private static Node adaptWeb(PluginUI pluginUI) {
        ClassLoader pluginCl = pluginUI.getClass().getClassLoader();
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        Object view;
        try {
            Thread.currentThread().setContextClassLoader(pluginCl);
            view = pluginUI.createView();
        }
        finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        engine.setJavaScriptEnabled(true);

        if ("true".equalsIgnoreCase(System.getProperty("opencgl.dev.mode"))) {
            System.setProperty("remote.debugging.port", "9222");
            logger.debug("WebView remote debugging enabled on port 9222");
        }

        final boolean withBridge = pluginUI instanceof WebPluginWithBridge;
        if (withBridge) {
            engine.getLoadWorker().stateProperty().addListener((o, oldState, state) -> {
                if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                    injectBridge(engine, (WebPluginWithBridge) pluginUI);
                }
            });
        }

        if (view instanceof URL) {
            engine.load(((URL) view).toExternalForm());
        }
        else if (view instanceof String content) {
            if (isUrlString(content)) {
                engine.load(content);
            }
            else {
                engine.loadContent(content);
            }
        }
        else {
            engine.loadContent("");
        }

        return webView;
    }

    private static void injectBridge(WebEngine engine, WebPluginWithBridge plugin) {
        try {
            JSObject window = (JSObject) engine.executeScript("window");
            if (window != null) {
                window.setMember("javaBridge", plugin.getJsBridge());
                logger.debug("Web plugin Java bridge injected as window.javaBridge");
            }
        }
        catch (Exception e) {
            logger.warn("Failed to inject Web plugin bridge: {}", e.getMessage());
        }
    }

    private static boolean isUrlString(String content) {
        return content != null && (
            content.startsWith("http://") ||
                content.startsWith("https://") ||
                content.startsWith("file:/") ||
                content.startsWith("jar:file:")
        );
    }
}
