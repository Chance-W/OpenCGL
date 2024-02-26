package com.opencgl.util;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


import com.opencgl.selfpane.CustomDialog;
import com.opencgl.selfpane.CustomInfoDialog;
import com.opencgl.selfpane.CustomTextDialog;
import javafx.application.Platform;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/16 13:39
 * @since v9.0
 */
@SuppressWarnings("unused")
public class DialogUtil {

    private static final CustomDialog customDialog = new CustomDialog();

    private static final CustomInfoDialog customInfoDialog = new CustomInfoDialog();

    public static String show() {
        AtomicReference<String> value = new AtomicReference<>("");
        Platform.runLater(() -> {
            Optional<String> result = customDialog.showAndWait();
            result.ifPresent(value::set);
        });
        return value.get();
    }

    public static String show(String text) {
        AtomicReference<String> value = new AtomicReference<>(text);
        Platform.runLater(() -> {
            customDialog.setTextField(text);
            Optional<String> result = customDialog.showAndWait();
            result.ifPresent(value::set);
        });
        return value.get();
    }

    public static void showSuccessInfo(String text) {
        Platform.runLater(() -> {
            customInfoDialog.setLabelText(text);
            customInfoDialog.showAndWait();
        });

    }

    public static void showErrorInfo(String text) {
        Platform.runLater(() -> {
            customInfoDialog.setHeaderText("ERROR");
            customInfoDialog.setLabelText(text);
            customInfoDialog.showAndWait();
        });
    }

    public static void showCustomTextInfo(String text) {
        Platform.runLater(() -> {
            CustomTextDialog customTextDialog = new CustomTextDialog(text);
            customTextDialog.showAndWait();
        });
    }
    public static void showCustomTextInfo(String headerText, String text) {
        Platform.runLater(() -> {
            CustomTextDialog customTextDialog = new CustomTextDialog(text);
            customTextDialog.setHeaderText(headerText);
            customTextDialog.showAndWait();
        });
    }

    public static void showCustomTextInfo(String headerText, String text, String additionalText) {
        Platform.runLater(() -> {
            CustomTextDialog customTextDialog = new CustomTextDialog(text, additionalText);
            customTextDialog.setHeaderText(headerText);
            customTextDialog.showAndWait();
        });
    }
}
