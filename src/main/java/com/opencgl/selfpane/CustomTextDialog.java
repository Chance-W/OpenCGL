package com.opencgl.selfpane;

import java.util.Objects;

import com.opencgl.i18n.I18N;
import com.opencgl.base.theme.ThemeManager;
import com.opencgl.util.DialogUtil;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.enums.ButtonType;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
public class CustomTextDialog extends Dialog<Void> {

    private final TextArea labelText = new TextArea(null);

    private final TextArea additionalLabelText = new TextArea(null);

    private final Label labelHeader = new Label("");

    private final VBox alertVBox = new VBox();

    private final HBox buttonHBox = new HBox();

    private final EventHandler<KeyEvent> escCloseFilter = e -> {
        if (e.getCode() == KeyCode.ESCAPE) {
            e.consume();
            close();
        }
    };

    public CustomTextDialog(String text, String additionalText) {
        super();
        initCustomDialog(text, additionalText);
    }

    public CustomTextDialog(String text) {
        super();
        initCustomDialog(text);
    }

    private void initCustomDialog(String text) {
        buildDialog();
        labelText.setText(text);
        labelText.setWrapText(true);
        labelText.setEditable(false);
        labelText.getStyleClass().add("dialog-text-area");
        alertVBox.getChildren().addAll(labelHeader, labelText, buttonHBox);
        // END VBox
        refreshContentAndPosition();
    }

    private void initCustomDialog(String text, String additionalText) {
        buildDialog();
        labelText.setText(text);
        labelText.setEditable(false);
        labelText.setWrapText(true);
        labelText.getStyleClass().add("dialog-text-area");
        additionalLabelText.setText(additionalText);
        additionalLabelText.setWrapText(true);
        additionalLabelText.setEditable(false);
        additionalLabelText.getStyleClass().add("dialog-text-area");
        alertVBox.getChildren().addAll(labelHeader, labelText, additionalLabelText, buttonHBox);
        refreshContentAndPosition();
    }

    private void refreshContentAndPosition() {
        // END VBox
        getDialogPane().setContent(alertVBox);

        this.setOnShown(event -> {
            Scene scene = getDialogPane().getScene();
            if (scene != null) {
                ThemeManager.getInstance().registerScene(scene);
                // 在 Scene 上注册 ESC 关闭（焦点在 TextArea 时也能收到）
                scene.addEventFilter(KeyEvent.KEY_PRESSED, escCloseFilter);
            }
            // 有 owner 时相对 owner 居中（多屏/插件窗口一致），否则相对当前聚焦窗口
            if (getOwner() != null) {
                DialogUtil.centerOnOwner(this, getOwner());
            } else {
                DialogUtil.centerOnActiveWindow(this);
            }
        });

        // 对话框关闭时注销主题并移除 ESC 监听
        this.setOnHidden(event -> {
            Scene scene = getDialogPane().getScene();
            if (scene != null) {
                scene.removeEventFilter(KeyEvent.KEY_PRESSED, escCloseFilter);
                ThemeManager.getInstance().unregisterScene(scene);
            }
        });
    }

    private void buildDialog() {
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
//        getDialogPane().getStyleClass().add("root");
//        getDialogPane().getStyleClass().add("opencgl-dialog");

        alertVBox.setSpacing(5);
        VBox.setVgrow(alertVBox, Priority.ALWAYS);

        MFXButton confirmButton = new MFXButton(I18N.getOrDefault("opencgl.main.button.confirm"));
        confirmButton.setButtonType(ButtonType.RAISED);
        confirmButton.setMinHeight(40);
        confirmButton.setMinWidth(100);
        Scene scene = this.getDialogPane().getScene();
        ThemeManager.getInstance().registerScene(scene);
        confirmButton.setOnAction(event -> {
            ThemeManager.getInstance().unregisterScene(scene);
            Stage stage = (Stage) scene.getWindow();
            stage.close();
        });

        buttonHBox.setAlignment(Pos.CENTER_RIGHT);
        buttonHBox.getChildren().addAll(confirmButton);
        buttonHBox.setPadding(new Insets(10.0, 0, 0, 0));
    }

    public void setCustomHeaderText(String value) {
        labelHeader.setText(value);
    }

}
