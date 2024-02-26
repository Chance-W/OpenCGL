package com.opencgl.selfpane;

import java.util.Objects;

import com.opencgl.i18n.I18N;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.enums.ButtonType;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
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
 * @since v9.0
 */
@SuppressWarnings("unused")
public class CustomTextDialog extends Dialog<Void> {

    private final TextArea labelText = new TextArea(null);

    private final TextArea additionalLabelText = new TextArea(null);

    private final VBox alertVBox = new VBox();

    private final HBox buttonHBox = new HBox();

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
        alertVBox.getChildren().addAll(labelText, buttonHBox);
        // END VBox
        refreshContentAndPosition();
    }

    private void initCustomDialog(String text, String additionalText) {
        buildDialog();
        labelText.setText(text);
        labelText.setEditable(false);
        labelText.setWrapText(true);
        additionalLabelText.setText(additionalText);
        additionalLabelText.setWrapText(true);
        additionalLabelText.setEditable(false);
        alertVBox.getChildren().addAll(labelText, additionalLabelText, buttonHBox);
        refreshContentAndPosition();
    }

    private void refreshContentAndPosition() {
        // END VBox
        getDialogPane().setContent(alertVBox);
        Window window = Stage.getWindows().get(0);
        this.initOwner(window);
        this.setOnShown(event -> Platform.runLater(() -> {
            this.setX(window.getX() + (window.getWidth() - this.getDialogPane().getWidth()) / 2);
            // 位置稍微高一点，使用者视觉效果可能会好一点
            this.setY(window.getY() + (window.getHeight() - this.getDialogPane().getHeight() - 100) / 2);
        }));
    }

    private void buildDialog() {
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
        getDialogPane().getStylesheets().setAll(Objects.requireNonNull(this.getClass().getResource("/com/opencgl/css/opencgl-dialog.css")).toExternalForm());
        getDialogPane().getStyleClass().add(0, "opencgl-dialog");
        alertVBox.setSpacing(5);
        VBox.setVgrow(alertVBox, Priority.ALWAYS);

        MFXButton confirmButton = new MFXButton(I18N.getOrDefault("opencgl.main.button.confirm"));
        confirmButton.setButtonType(ButtonType.RAISED);
        confirmButton.setMinHeight(40);
        confirmButton.setMinWidth(100);
        confirmButton.setOnAction(event -> {
            Scene scene = this.getDialogPane().getScene();
            Stage stage = (Stage) scene.getWindow();
            stage.close();
        });

        buttonHBox.setAlignment(Pos.CENTER_RIGHT);
        buttonHBox.getChildren().addAll(confirmButton);
        buttonHBox.setPadding(new Insets(10.0, 0, 0, 0));
    }

}
