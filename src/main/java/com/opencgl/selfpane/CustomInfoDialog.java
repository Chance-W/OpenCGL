package com.opencgl.selfpane;

import java.util.Objects;

import com.opencgl.base.theme.ThemeManager;
import com.opencgl.i18n.I18N;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.enums.ButtonType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/13 22:56
 * @since v2.0
 */
@SuppressWarnings("unused")
public class CustomInfoDialog extends Dialog<Void> {

    private final Label labelHeader = new Label(I18N.getOrDefault("opencgl.main.info.dialog.labelHeader"));

    private final Label labelText = new Label(I18N.getOrDefault("opencgl.main.info.dialog.labelText"));

    public CustomInfoDialog() {
        super();
        initCustomDialog();
    }

    private void initCustomDialog() {
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
        getDialogPane().getStyleClass().addFirst("root");
        getDialogPane().getStyleClass().add("opencgl-dialog");

        // 注册到 ThemeManager 以支持主题切换
        this.setOnShown(event -> {
            Scene scene = getDialogPane().getScene();
            if (scene != null) {
                ThemeManager.getInstance().registerScene(scene);
            }
        });

        // 当对话框关闭时注销
        this.setOnHidden(event -> {
            Scene scene = getDialogPane().getScene();
            if (scene != null) {
                ThemeManager.getInstance().unregisterScene(scene);
            }
        });
        VBox alertVBox = new VBox();
        alertVBox.setMinWidth(400.0);
        alertVBox.setMinHeight(150.0);
        VBox.setVgrow(alertVBox, Priority.ALWAYS);
        alertVBox.setSpacing(30);

        MFXButton confirmButton = new MFXButton(I18N.getOrDefault("opencgl.main.button.confirm"));
        confirmButton.setButtonType(ButtonType.RAISED);
        confirmButton.setOnAction(event -> {
            Scene scene = this.getDialogPane().getScene();
            Stage stage = (Stage) scene.getWindow();
            stage.close();
        });

        HBox buttonHBox = new HBox();
        buttonHBox.setAlignment(Pos.CENTER_RIGHT);
        buttonHBox.getChildren().addAll(confirmButton);
        buttonHBox.setPadding(new Insets(50.0, 0, 0, 0));
        alertVBox.getChildren().addAll(labelHeader, labelText, buttonHBox);

        // END VBox
        getDialogPane().setContent(alertVBox);

        this.setOnShown(event -> {
            Scene scene = getDialogPane().getScene();
            if (scene != null) {
                ThemeManager.getInstance().registerScene(scene);
            }
            // 居中定位到当前活跃屏幕
            com.opencgl.util.DialogUtil.centerOnActiveWindow(this);
        });

    }

    public void setLabelText(String value) {
        labelText.setText(value);
    }

    public void setCustomHeaderText(String value) {
        labelHeader.setText(value);
    }

}
