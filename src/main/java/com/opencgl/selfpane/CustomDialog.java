package com.opencgl.selfpane;

import java.util.Objects;

import org.apache.commons.lang.StringUtils;

import com.opencgl.i18n.I18N;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.enums.FloatMode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/13 22:56
 * @since v9.0
 */
@SuppressWarnings("unused")
public class CustomDialog extends Dialog<String> {

    private MFXButton confirmButton;

    private MFXButton cancelButton;

    private MFXTextField textField;

    public CustomDialog() {
        super();
        initCustomDialog();
    }

    private void initCustomDialog() {
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
        getDialogPane().getStylesheets().setAll(Objects.requireNonNull(this.getClass().getResource("/com/opencgl/css/opencgl-dialog.css")).toExternalForm());
        getDialogPane().getStyleClass().setAll("opencgl-dialog");


        Label label = new Label(I18N.getOrDefault("opencgl.main.dialog.labelHeader"));
        HBox headerHBox = new HBox(label);


        textField = new MFXTextField();
        textField.setFloatMode(FloatMode.ABOVE);
        textField.setPromptText(I18N.getOrDefault("opencgl.main.dialog.labelHeader"));
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (StringUtils.isEmpty(newValue)) {
                textField.setStyle("-fx-border-color: red;");
            }
            else {
                textField.setStyle("");
            }
        });
        HBox textHBox = new HBox();
        textHBox.getChildren().add(textField);

        confirmButton = new MFXButton(I18N.getOrDefault("opencgl.main.button.confirm"));
        confirmButton.setButtonType(ButtonType.RAISED);
        confirmButton.setOnAction(event -> {
            if (textField.getText() == null || textField.getText().trim().isEmpty()) {
                textField.setStyle("-fx-border-color: red;");
                return;
            }
            setResult(textField.getText());
            close();
        });

        cancelButton = new MFXButton(I18N.getOrDefault("opencgl.main.button.cancel"));
        cancelButton.setButtonType(ButtonType.RAISED);
        cancelButton.setOnAction(event -> {
            setResult("");
            close();
        });

        HBox buttonHBox = new HBox();
        buttonHBox.setAlignment(Pos.CENTER_RIGHT);
        buttonHBox.setSpacing(10.0);
        buttonHBox.getChildren().addAll(confirmButton, cancelButton);

        VBox vBox = new VBox();
        vBox.getChildren().addAll(headerHBox, textHBox, buttonHBox);
        vBox.setSpacing(20);
        vBox.setPadding(new Insets(10));
        // END VBox
        getDialogPane().setContent(vBox);

        Window window = Stage.getWindows().get(0);
        this.initOwner(window);
        this.setOnShown(event -> Platform.runLater(() -> {
            this.setX(window.getX() + (window.getWidth() - this.getDialogPane().getWidth()) / 2);
            // 位置稍微高一点，使用者视觉效果可能会好一点
            this.setY(window.getY() + (window.getHeight() - this.getDialogPane().getHeight() - 100) / 2);
        }));
    }

    public void setConfirmOnAction(EventHandler<ActionEvent> value) {
        confirmButton.setOnAction(value);
    }

    public void setCancelOnAction(EventHandler<ActionEvent> value) {
        cancelButton.setOnAction(value);
    }

    public void setTextField(String value) {
        textField.setText(value);
    }

}
