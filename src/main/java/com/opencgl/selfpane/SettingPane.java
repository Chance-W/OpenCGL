package com.opencgl.selfpane;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.i18n.I18N;
import com.opencgl.listener.Config;
import com.opencgl.model.OpenCGLSelfProperties;
import com.opencgl.util.DialogUtil;
import com.opencgl.util.TooltipUtil;
import io.github.palexdev.materialfx.controls.MFXButton;
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
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/16 13:55
 * @since v9.0
 */
@SuppressWarnings("unused")
public class SettingPane {
    private static final Logger logger = LoggerFactory.getLogger(SettingPane.class);

    private static final StackPane stackPane = new StackPane();
    private static final BorderPane borderPane = new BorderPane();
    private static final VBox vBox = new VBox();
    private static final Map<String, String> configMap = new HashMap<>();

    private void init(Pane root) {
        if (!vBox.getChildren().isEmpty()) {
            return;
        }
        Label headerLabel = new Label("设置全局配置");
        HBox headerBox = new HBox(headerLabel);
        headerBox.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 10, 0.12, -1.0, 2.0)");
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10));
        borderPane.setMaxSize(700, 300);
        borderPane.setTop(headerBox);
        borderPane.setCenter(vBox);
        borderPane.setStyle("-fx-background-color: white;-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 2), 10, 0.12, -1.0, 2.0)");
        stackPane.getChildren().add(borderPane);
        stackPane.setPrefSize(root.getPrefWidth(), root.getPrefHeight());
        stackPane.setAlignment(Pos.CENTER);

        Label label = new Label("插件路径：");
        MFXTextField pluginTextField = new MFXTextField();
        pluginTextField.setEditable(false);
        pluginTextField.setPrefSize(400, 20);
        pluginTextField.setText(Config.readExternalConfigure(OpenCGLSelfProperties.PLUGIN_PATH_KEY));

        MFXButton choosePluginButton = new MFXButton("...");
        choosePluginButton.setButtonType(ButtonType.RAISED);
        Tooltip chooseTooltip = new Tooltip("请选择插件包加载路径");
        chooseTooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(choosePluginButton, chooseTooltip);

        choosePluginButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            Stage direChooseStage = new Stage();
            direChooseStage.setTitle("请选择插件包默认加载路径");
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File selectedDirectory = directoryChooser.showDialog(direChooseStage);
            if (selectedDirectory != null)
                pluginTextField.setText(selectedDirectory.getAbsolutePath());
        });


        MFXButton openPluginButton = new MFXButton("打开插件路径");
        openPluginButton.setButtonType(ButtonType.RAISED);
        Tooltip openTooltip = new Tooltip("点击直接打开插件路径");
        openTooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(openPluginButton, openTooltip);

        openPluginButton.setOnAction(event -> {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(new File(pluginTextField.getText()));
                }
                catch (IOException e) {
                    logger.error("", e);

                }
            }
        });

        HBox pluginPathHBox = new HBox();
        pluginPathHBox.setAlignment(Pos.CENTER_LEFT);
        pluginPathHBox.getChildren().addAll(label, pluginTextField, choosePluginButton, openPluginButton);
        pluginPathHBox.setSpacing(10);
        HBox.setHgrow(pluginTextField, Priority.ALWAYS);

        vBox.getChildren().addAll(pluginPathHBox);

        vBox.setPadding(new Insets(10));
        vBox.setAlignment(Pos.TOP_CENTER);


        MFXButton confirmButton = new MFXButton(I18N.getOrDefault("opencgl.main.button.confirm"));
        confirmButton.getStylesheets().setAll(Objects.requireNonNull(SettingPane.class.getResource("/com/opencgl/css/opencgl-dialog.css")).toExternalForm());
        confirmButton.setOnAction(event -> {
            Map<String, String> pluginPathMap = new HashMap<>();
            pluginPathMap.put(OpenCGLSelfProperties.PLUGIN_PATH_KEY, pluginTextField.getText());
            try {
                Config.updateExternalConfigure(pluginPathMap);
                TooltipUtil.showToast("修改成功！");
            }
            catch (IOException e) {
                logger.error("", e);
                DialogUtil.showErrorInfo(e.getMessage());
            }
            Platform.runLater(() -> remove(root));
        });

        MFXButton cancelButton = new MFXButton(I18N.getOrDefault("opencgl.main.button.cancel"));
        cancelButton.getStylesheets().setAll(Objects.requireNonNull(SettingPane.class.getResource("/com/opencgl/css/opencgl-dialog.css")).toExternalForm());
        cancelButton.setOnAction(event -> Platform.runLater(() -> remove(root)));

        stackPane.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                confirmButton.fire();
            }
            else if (event.getCode() == KeyCode.ESCAPE) {
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
        Platform.runLater(() -> root.getChildren().remove(stackPane));
    }
}
