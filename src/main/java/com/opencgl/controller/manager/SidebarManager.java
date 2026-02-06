package com.opencgl.controller.manager;

import java.util.concurrent.CompletableFuture;

import com.opencgl.i18n.I18N;
import com.opencgl.icon.CategoryIconManager;
import com.opencgl.util.TooltipUtil;

import io.github.palexdev.materialfx.controls.MFXIconWrapper;
import io.github.palexdev.materialfx.controls.MFXRectangleToggleNode;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;

/**
 * 侧边栏管理器
 * 负责创建和管理侧边栏的 Toggle 按钮
 *
 * @author Chance.W
 * @since v2.0
 */
public class SidebarManager {

    private final ToggleGroup toggleGroup;

    public SidebarManager(ToggleGroup toggleGroup) {
        this.toggleGroup = toggleGroup;
    }

    /**
     * 创建 Toggle 按钮
     */
    public ToggleButton createToggle(String icon, String text) {
        return createToggle(icon, text, 0);
    }

    public ToggleButton createToggle(String icon, StringBinding textBinding) {
        return createToggle(icon, textBinding, 0);
    }

    /**
     * 创建带旋转角度的 Toggle 按钮
     */
    public ToggleButton createToggle(String icon, String text, double rotate) {
        MFXIconWrapper wrapper = new MFXIconWrapper(icon, 24, 32);
        MFXRectangleToggleNode toggleNode = new MFXRectangleToggleNode(text, wrapper);
        toggleNode.setAlignment(Pos.CENTER_LEFT);
        toggleNode.setMaxWidth(Double.MAX_VALUE);
        toggleNode.setToggleGroup(toggleGroup);
        if (rotate != 0) {
            wrapper.getIcon().setRotate(rotate);
        }
        return toggleNode;
    }

    public ToggleButton createToggle(String icon, StringBinding textBinding, double rotate) {
        MFXIconWrapper wrapper = new MFXIconWrapper(icon, 24, 32);
        MFXRectangleToggleNode toggleNode = new MFXRectangleToggleNode("", wrapper);
        toggleNode.textProperty().bind(textBinding);
        toggleNode.setAlignment(Pos.CENTER_LEFT);
        toggleNode.setMaxWidth(Double.MAX_VALUE);
        toggleNode.setToggleGroup(toggleGroup);
        if (rotate != 0) {
            wrapper.getIcon().setRotate(rotate);
        }
        return toggleNode;
    }

    /**
     * 创建带右键菜单的 Toggle 按钮（用于插件分类）
     */
    public ToggleButton createToggleWithContextMenu(String icon, String categoryName) {
        // 尝试加载分类名称的国际化绑定，如果不存在则使用原始名称
        String categoryKey = "opencgl.category." + categoryName;
        StringBinding textBinding;
        if (I18N.getBundle(I18N.getLocale()).containsKey(categoryKey)) {
            textBinding = I18N.getBinding(categoryKey);
        }
        else {
            textBinding = Bindings.createStringBinding(() -> categoryName);
        }
        return createToggleWithContextMenu(icon, textBinding, categoryName);
    }

    public ToggleButton createToggleWithContextMenu(String icon, StringBinding textBinding, String categoryName) {
        MFXIconWrapper wrapper = new MFXIconWrapper(icon, 24, 32);
        MFXRectangleToggleNode toggleNode = new MFXRectangleToggleNode("", wrapper);
        toggleNode.textProperty().bind(textBinding);
        toggleNode.setAlignment(Pos.CENTER_LEFT);
        toggleNode.setMaxWidth(Double.MAX_VALUE);
        toggleNode.setToggleGroup(toggleGroup);

        // 创建右键菜单
        ContextMenu contextMenu = new ContextMenu();

        // 刷新图标菜单项
        MenuItem refreshItem = new MenuItem();
        refreshItem.textProperty()
            .bind(Bindings.concat("🔄 ", I18N.getBinding("opencgl.main.context.refreshIcon")));
        refreshItem.setOnAction(e -> CompletableFuture.supplyAsync(() -> CategoryIconManager.refreshIcon(categoryName))
            .thenAcceptAsync(success -> {
                if (success) {
                    String newIcon = CategoryIconManager.getIconForCategory(categoryName);
                    if (wrapper.getIcon() instanceof MFXFontIcon) {
                        ((MFXFontIcon) wrapper.getIcon()).setDescription(newIcon);
                    }
                    TooltipUtil.showToast(I18N.getOrDefault("opencgl.main.context.iconRefreshed"));
                }
                else {
                    TooltipUtil.showToast(I18N.getOrDefault("opencgl.main.context.iconPinnedCannotRefresh"));
                }
            }, Platform::runLater));

        // 固定/取消固定菜单项
        MenuItem pinItem = new MenuItem();
        updatePinMenuItem(pinItem, categoryName);
        pinItem.setOnAction(e -> CompletableFuture.runAsync(() -> {
            if (CategoryIconManager.isIconPinned(categoryName)) {
                CategoryIconManager.unpinIcon(categoryName);
                Platform.runLater(() -> TooltipUtil.showToast(I18N.getOrDefault("opencgl.main.context.unpinned")));
            }
            else {
                CategoryIconManager.pinIcon(categoryName);
                Platform.runLater(() -> TooltipUtil.showToast(I18N.getOrDefault("opencgl.main.context.pinned")));
            }
        }).thenRun(() -> Platform.runLater(() -> updatePinMenuItem(pinItem, categoryName))));

        contextMenu.getItems().addAll(refreshItem, pinItem);

        toggleNode.setOnContextMenuRequested(e -> {
            updatePinMenuItem(pinItem, categoryName);
            contextMenu.show(toggleNode, e.getScreenX(), e.getScreenY());
        });

        return toggleNode;
    }

    /**
     * 更新固定菜单项文本
     */
    private void updatePinMenuItem(MenuItem item, String categoryName) {
        boolean isPinned = CategoryIconManager.isIconPinned(categoryName);
        if (isPinned) {
            item.textProperty()
                .bind(Bindings.concat("📍 ", I18N.getBinding("opencgl.main.context.unpin")));
        }
        else {
            item.textProperty()
                .bind(Bindings.concat("📌 ", I18N.getBinding("opencgl.main.context.pin")));
        }
    }

    /**
     * 移除选中的 Toggle 按钮样式
     */
    public void removeSelectedToggleButton(ObservableList<Node> nodes) {
        for (Node node : nodes) {
            if (node instanceof ToggleButton toggleButton) {
                toggleButton.setSelected(false);
            }
        }
    }
}
