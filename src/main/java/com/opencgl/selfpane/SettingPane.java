package com.opencgl.selfpane;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.base.utils.CssUtil;
import com.opencgl.i18n.I18N;
import com.opencgl.icon.CategoryIconManager;
import com.opencgl.base.listener.Config;
import com.opencgl.base.model.OpenCGLSelfProperties;
import com.opencgl.util.DialogUtil;
import com.opencgl.util.LocalPluginIndexGenerator;
import com.opencgl.util.PluginParserHelper;
import com.opencgl.util.TooltipUtil;
import com.opencgl.service.PluginStateService;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.ButtonType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/16 13:55
 * @since v2.0
 */
@SuppressWarnings("unused")
public class SettingPane {
    private static final Logger logger = LoggerFactory.getLogger(SettingPane.class);

    // 改为实例变量，避免静态 UI 组件导致的内存问题
    private final StackPane stackPane = new StackPane();
    private final BorderPane borderPane = new BorderPane();
    private final VBox vBox = new VBox();
    private boolean initialized = false;
    private Runnable onPluginStateChanged = () -> { };

    public void setOnPluginStateChanged(Runnable callback) {
        this.onPluginStateChanged = callback == null ? () -> { } : callback;
    }

    private void init(Pane root) {
        if (initialized) {
            return;
        }
        initialized = true;
        Label headerLabel = new Label();
        headerLabel.textProperty().bind(I18N.getBinding("opencgl.setting.title"));
        HBox headerBox = new HBox(headerLabel);
        headerBox.getStyleClass().add("setting-pane-header");
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10));
        // 动态适配屏幕：面板宽高不超过当前屏幕的 85%×80%，解决低分辨率下超出屏幕的问题
        javafx.geometry.Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double maxPanelW = Math.min(700, screenBounds.getWidth() * 0.85);
        double maxPanelH = Math.min(380, screenBounds.getHeight() * 0.80);
        borderPane.setMaxSize(maxPanelW, maxPanelH);
        borderPane.setTop(headerBox);
        borderPane.setCenter(vBox);
        borderPane.getStyleClass().add("setting-pane-container");
        stackPane.getChildren().add(borderPane);
        stackPane.setPrefSize(root.getPrefWidth(), root.getPrefHeight());
        stackPane.setAlignment(Pos.CENTER);

        Label label = new Label();
        label.textProperty().bind(I18N.getBinding("opencgl.setting.plugin_path"));
        MFXTextField pluginTextField = new MFXTextField();
        pluginTextField.setEditable(false);
        pluginTextField.setPrefSize(400, 20);
        pluginTextField.setText(Config.readExternalConfigure(OpenCGLSelfProperties.PLUGIN_PATH_KEY));

        MFXButton choosePluginButton = new MFXButton("...");
        choosePluginButton.setButtonType(ButtonType.RAISED);
        Tooltip chooseTooltip = new Tooltip();
        chooseTooltip.textProperty().bind(I18N.getBinding("opencgl.setting.choose_path_prompt"));
        chooseTooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(choosePluginButton, chooseTooltip);

        choosePluginButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            Stage direChooseStage = new Stage();
            direChooseStage.titleProperty().bind(I18N.getBinding("opencgl.setting.choose_path_title"));
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File selectedDirectory = directoryChooser.showDialog(direChooseStage);
            if (selectedDirectory != null)
                pluginTextField.setText(selectedDirectory.getAbsolutePath());
        });

        MFXButton openPluginButton = new MFXButton();
        openPluginButton.textProperty().bind(I18N.getBinding("opencgl.setting.open_path"));
        openPluginButton.setButtonType(ButtonType.RAISED);
        Tooltip openTooltip = new Tooltip();
        openTooltip.textProperty().bind(I18N.getBinding("opencgl.setting.open_path_tooltip"));
        openTooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(openPluginButton, openTooltip);

        openPluginButton.setOnAction(event -> {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(new File(pluginTextField.getText()));
                } catch (IOException e) {
                    logger.error("", e);

                }
            }
        });

        HBox pluginPathHBox = new HBox();
        pluginPathHBox.setAlignment(Pos.CENTER_LEFT);
        pluginPathHBox.getChildren().addAll(label, pluginTextField, choosePluginButton, openPluginButton);
        pluginPathHBox.setSpacing(10);
        HBox.setHgrow(pluginTextField, Priority.ALWAYS);

        // 插件加载状态
        Label pluginStatusLabel = new Label();
        pluginStatusLabel.textProperty().bind(I18N.getBinding("opencgl.setting.plugin_load_status"));
        pluginStatusLabel.setMinWidth(70);
        Map<String, String> loadFailures = PluginParserHelper.getLastLoadFailures();
        Map<String, PluginParserHelper.PluginLoadInfo> loadedPlugins = PluginParserHelper.getLastLoadedPlugins();
        long bundledCount = loadedPlugins.values().stream()
            .filter(info -> info.source() == PluginParserHelper.PluginSource.BUNDLED).count();
        long userCount = loadedPlugins.size() - bundledCount;
        String statusText = I18N.get("opencgl.setting.plugin_load_summary",
            loadedPlugins.size(), bundledCount, userCount);
        String overrides = loadedPlugins.values().stream()
            .filter(PluginParserHelper.PluginLoadInfo::overridesBundledPlugin)
            .map(info -> info.pluginId() + " (" + new File(info.jarPath()).getName() + ")")
            .collect(Collectors.joining("\n"));
        if (!overrides.isBlank()) {
            statusText += "\n" + I18N.get("opencgl.setting.plugin_override_summary", overrides);
        }
        if (loadFailures.isEmpty()) {
            statusText += "\n" + I18N.get("opencgl.setting.plugin_load_status_ok");
        } else {
            String failureText;
            if (loadFailures.containsKey(PluginParserHelper.KEY_PLUGIN_PATH_INVALID)) {
                failureText = I18N.get("opencgl.plugin.path_invalid");
            } else {
                failureText = loadFailures.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            }
            statusText += "\n" + failureText + "\n" + I18N.get("opencgl.setting.plugin_load_status_fail_hint");
        }
        Label pluginStatusValue = new Label(statusText);
        pluginStatusValue.setWrapText(true);
        pluginStatusValue.setMaxWidth(500);
        VBox pluginStatusBox = new VBox(5, pluginStatusLabel, pluginStatusValue);
        Map<String, String> disabledPlugins = PluginParserHelper.getLastDisabledPlugins();
        if (!disabledPlugins.isEmpty()) {
            Label disabledTitle = new Label(I18N.get("opencgl.setting.plugin_disabled_title"));
            VBox disabledList = new VBox(5);
            disabledPlugins.forEach((pluginId, pluginName) -> {
                Label itemLabel = new Label(pluginName + " (" + pluginId + ")");
                itemLabel.setWrapText(true);
                MFXButton enableButton = new MFXButton(I18N.get("opencgl.setting.plugin_enable"));
                HBox item = new HBox(8, itemLabel, enableButton);
                item.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(itemLabel, Priority.ALWAYS);
                enableButton.setOnAction(event -> {
                    new PluginStateService().enable(pluginId);
                    disabledList.getChildren().remove(item);
                    onPluginStateChanged.run();
                });
                disabledList.getChildren().add(item);
            });
            MFXButton enableAllButton = new MFXButton();
            enableAllButton.textProperty().bind(I18N.getBinding("opencgl.setting.plugin_enable_all"));
            enableAllButton.setOnAction(event -> {
                new PluginStateService().enableAll();
                disabledList.getChildren().clear();
                onPluginStateChanged.run();
                TooltipUtil.showToast(I18N.get("opencgl.setting.plugin_enable_all_saved"));
            });
            pluginStatusBox.getChildren().addAll(disabledTitle, disabledList, enableAllButton);
        }
        pluginStatusBox.setAlignment(Pos.CENTER_LEFT);

        // 图标管理区域（第一个设置项）
        Label iconLabel = new Label();
        iconLabel.textProperty().bind(I18N.getBinding("opencgl.setting.sidebar_icon"));
        iconLabel.setMinWidth(70);

        MFXButton refreshAllIconsButton = new MFXButton();
        refreshAllIconsButton.textProperty().bind(I18N.getBinding("opencgl.setting.refresh_icons"));
        refreshAllIconsButton.setButtonType(ButtonType.RAISED);
        Tooltip refreshIconTooltip = new Tooltip();
        refreshIconTooltip.textProperty().bind(I18N.getBinding("opencgl.setting.refresh_icons_tooltip"));
        refreshIconTooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(refreshAllIconsButton, refreshIconTooltip);
        refreshAllIconsButton.setOnAction(event -> {
            int count = 0;
            for (String categoryName : CategoryIconManager.getIconCache().keySet()) {
                if (CategoryIconManager.refreshIcon(categoryName)) {
                    count++;
                }
            }
            TooltipUtil.showToast(I18N.get("opencgl.setting.icons_refreshed", count));
        });

        MFXButton unpinAllIconsButton = new MFXButton();
        unpinAllIconsButton.textProperty().bind(I18N.getBinding("opencgl.setting.unpin_icons"));
        unpinAllIconsButton.setButtonType(ButtonType.RAISED);
        Tooltip unpinTooltip = new Tooltip();
        unpinTooltip.textProperty().bind(I18N.getBinding("opencgl.setting.unpin_icons_tooltip"));
        unpinTooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(unpinAllIconsButton, unpinTooltip);
        unpinAllIconsButton.setOnAction(event -> {
            int count = 0;
            for (String categoryName : CategoryIconManager.getIconCache().keySet()) {
                if (CategoryIconManager.isIconPinned(categoryName)) {
                    CategoryIconManager.unpinIcon(categoryName);
                    count++;
                }
            }
            TooltipUtil.showToast(I18N.get("opencgl.setting.icons_unpinned", count));
        });

        HBox iconHBox = new HBox(10, iconLabel, refreshAllIconsButton, unpinAllIconsButton);
        iconHBox.setAlignment(Pos.CENTER_LEFT);

        // 主题选择已移动至右上角菜单

        // 日志级别设置区域（第三个设置项）
        Label logLevelLabel = new Label();
        logLevelLabel.textProperty().bind(I18N.getBinding("opencgl.setting.log_level"));
        logLevelLabel.setMinWidth(70);
        MFXComboBox<String> logLevelComboBox = new MFXComboBox<>();
        logLevelComboBox.setPrefWidth(200);
        logLevelComboBox.getItems().addAll("DEBUG", "INFO", "WARN", "ERROR");

        String currentLevel = Config.readExternalConfigure(OpenCGLSelfProperties.LOG_LEVEL_KEY);
        if (currentLevel == null || currentLevel.isEmpty()) {
            currentLevel = "WARN"; // 默认 WARN
        }
        logLevelComboBox.selectItem(currentLevel);

        HBox logLevelHBox = new HBox(10, logLevelLabel, logLevelComboBox);
        logLevelHBox.setAlignment(Pos.CENTER_LEFT);

        // 顺序：图标管理 -> 日志级别 -> 插件路径 -> 插件加载状态 -> (高级工具可选)
        vBox.getChildren().addAll(iconHBox, logLevelHBox, pluginPathHBox, pluginStatusBox);

        // 高级工具区域 (第四个设置项) - 仅在开发者模式下显示
        if (CssUtil.isDevMode()) {
            Label toolsLabel = new Label();
            toolsLabel.textProperty().bind(I18N.getBinding("opencgl.setting.advanced_tools"));
            toolsLabel.setMinWidth(70);

            MFXButton exportIndexButton = new MFXButton();
            exportIndexButton.textProperty().bind(I18N.getBinding("opencgl.setting.export_index"));
            exportIndexButton.setButtonType(ButtonType.RAISED);
            Tooltip exportTooltip = new Tooltip();
            exportTooltip.textProperty().bind(I18N.getBinding("opencgl.setting.export_index_tooltip"));
            exportTooltip.setShowDelay(Duration.ZERO);
            Tooltip.install(exportIndexButton, exportTooltip);

            exportIndexButton.setOnAction(event -> {
                javafx.stage.DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle(I18N.get("opencgl.setting.export_index_choose_title"));
                // 默认打开用户主目录
                java.io.File initDir = new java.io.File(System.getProperty("user.home"));
                if (initDir.exists()) chooser.setInitialDirectory(initDir);

                javafx.stage.Window owner = exportIndexButton.getScene() != null
                        ? exportIndexButton.getScene().getWindow() : null;
                java.io.File selectedDir = chooser.showDialog(owner);
                if (selectedDir == null) return; // 用户取消，不做任何操作

                String targetOutput = selectedDir.getAbsolutePath();
                LocalPluginIndexGenerator.generateIndexJson(targetOutput,
                        () -> DialogUtil.showSuccessInfo(I18N.get("opencgl.setting.export_success", targetOutput)),
                        error -> DialogUtil.showErrorInfo(I18N.get("opencgl.setting.export_failed", error)));
            });

            HBox toolsHBox = new HBox(10, toolsLabel, exportIndexButton);
            toolsHBox.setAlignment(Pos.CENTER_LEFT);
            vBox.getChildren().add(toolsHBox);
        }

        vBox.setSpacing(15);

        vBox.setPadding(new Insets(10));
        vBox.setAlignment(Pos.TOP_CENTER);

        MFXButton confirmButton = new MFXButton(I18N.getOrDefault("opencgl.main.button.confirm"));
        confirmButton.setMinWidth(90);
        confirmButton.setMinHeight(36);
        confirmButton.setPadding(new Insets(10, 20, 10, 20));
        // confirmButton.getStylesheets().setAll(Objects.requireNonNull(SettingPane.class.getResource("/com/opencgl/css/opencgl-dialog.css")).toExternalForm());
        confirmButton.setOnAction(event -> {
            Map<String, String> pluginPathMap = new HashMap<>();
            pluginPathMap.put(OpenCGLSelfProperties.PLUGIN_PATH_KEY, pluginTextField.getText());

            // 保存日志级别并动态应用
            String newLogLevel = logLevelComboBox.getSelectedItem();
            if (newLogLevel != null) {
                pluginPathMap.put(OpenCGLSelfProperties.LOG_LEVEL_KEY, newLogLevel);
                LogManager.getRootLogger().setLevel(Level.toLevel(newLogLevel));
            }

            Config.updateExternalConfigure(pluginPathMap);
            TooltipUtil.showToast(I18N.get("opencgl.setting.save_success"));
            Platform.runLater(() -> remove(root));
        });

        MFXButton cancelButton = new MFXButton(I18N.getOrDefault("opencgl.main.button.cancel"));
        cancelButton.setMinWidth(90);
        cancelButton.setMinHeight(36);
        cancelButton.setPadding(new Insets(10, 20, 10, 20));
        // cancelButton.getStylesheets().setAll(Objects.requireNonNull(SettingPane.class.getResource("/com/opencgl/css/opencgl-dialog.css")).toExternalForm());
        cancelButton.setOnAction(event -> Platform.runLater(() -> remove(root)));

        stackPane.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                confirmButton.fire();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                cancelButton.fire();
            }
        });

        HBox buttonHBox = new HBox();
        buttonHBox.setAlignment(Pos.CENTER_RIGHT);
        buttonHBox.setSpacing(10);
        buttonHBox.setPadding(new Insets(10));
        buttonHBox.getChildren().addAll(confirmButton, cancelButton);
        borderPane.setBottom(buttonHBox);

    }

    public void show(Pane root) {
        init(root);
        Platform.runLater(() -> {
            root.getChildren().add(stackPane);
            stackPane.requestFocus();
        });
    }

    public void remove(Pane root) {
        Platform.runLater(() -> {
            root.getChildren().remove(stackPane);
            // Rebuild on the next open so plugin status and disabled entries are current.
            vBox.getChildren().clear();
            stackPane.getChildren().clear();
            borderPane.setTop(null);
            borderPane.setCenter(null);
            borderPane.setBottom(null);
            initialized = false;
        });
    }
}
