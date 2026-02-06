package com.opencgl.controller.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.base.theme.ThemeManager;
import com.opencgl.i18n.I18N;
import com.opencgl.util.TooltipUtil;

import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import lombok.Getter;

/**
 * 右键菜单处理器
 * 负责构建和管理应用右键菜单
 *
 * @author Chance.W
 * @since v2.0
 */
@Getter
public class ContextMenuHandler {
    private static final Logger logger = LoggerFactory.getLogger(ContextMenuHandler.class);

    /**
     * -- GETTER --
     * 检查菜单是否打开
     */
    private boolean contextMenuOpen = false;

    /**
     * 构建并安装下拉菜单
     *
     * @param targetNode      目标节点
     * @param onSwitchToTab   切换到 Tab 页回调
     * @param onReloadPlugins 重新加载插件回调
     * @param onOpenHistory   打开历史记录回调
     */
    public void buildAndInstall(
        Node targetNode,
        Runnable onSwitchToTab,
        Runnable onReloadPlugins,
        Runnable onOpenHistory
    ) {
        MFXFontIcon switchIcon = new MFXFontIcon("fas-check-double", 16);
        switchIcon.getStyleClass().add("toolbar-icon");
        MFXContextMenuItem switchToTabItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.getOrDefault("opencgl.main.switchToTabListPage"))
            .setIcon(switchIcon)
            .setOnAction(event -> onSwitchToTab.run())
            .get();
        switchToTabItem.setStyle("-fx-max-height: 20px;");

        MFXFontIcon reloadIcon = new MFXFontIcon("fas-arrow-rotate-left", 16);
        reloadIcon.getStyleClass().add("toolbar-icon");
        MFXContextMenuItem reloadPluginItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.getOrDefault("opencgl.main.reloadPluginMenuItem"))
            .setIcon(reloadIcon)
            .setOnAction(event -> {
                logger.info("begin reload plugin....");
                try {
                    onReloadPlugins.run();
                    TooltipUtil.showToast(I18N.getOrDefault("opencgl.mainWindows.loadPlugin.success"));
                }
                catch (Exception e) {
                    TooltipUtil.showToast(I18N.getOrDefault("opencgl.mainWindows.loadPlugin.error") + e.getMessage());
                }
            })
            .get();
        reloadPluginItem.setStyle("-fx-max-height: 20px;");

        MFXFontIcon histIcon = new MFXFontIcon("fas-clock-rotate-left", 16);
        histIcon.getStyleClass().add("toolbar-icon");
        MFXContextMenuItem historyItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.getOrDefault("opencgl.main.context.history"))
            .setIcon(histIcon)
            .setOnAction(e -> onOpenHistory.run())
            .get();

        MFXContextMenu menu;
        if (targetNode.getScene() != null && targetNode.getScene().getRoot() != null) {
            menu = new MFXContextMenu(targetNode.getScene().getRoot());
        }
        else {
            menu = new MFXContextMenu(targetNode);
        }

        menu.getItems().addAll(switchToTabItem, reloadPluginItem, historyItem);

        targetNode.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                javafx.geometry.Bounds bounds = targetNode.localToScreen(targetNode.getBoundsInLocal());
                if (bounds != null) {
                    menu.show(targetNode, bounds.getMinX(), bounds.getMaxY() + 5);
                }
            }
        });

        menu.setOnShown(event -> {
            contextMenuOpen = true;
            if (menu.getScene() != null) {
                ThemeManager.getInstance().getCurrentThemeStylesheets().forEach(sheet -> {
                    if (!menu.getScene().getStylesheets().contains(sheet)) {
                        menu.getScene().getStylesheets().add(sheet);
                    }
                });
            }
        });
        menu.setOnHidden(event -> contextMenuOpen = false);
    }

}
