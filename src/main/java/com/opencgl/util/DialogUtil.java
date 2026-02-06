package com.opencgl.util;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import com.opencgl.base.theme.ThemeManager;
import com.opencgl.base.view.CustomConfirmDialog;
import com.opencgl.i18n.I18N;
import com.opencgl.selfpane.CustomDialog;
import com.opencgl.selfpane.CustomInfoDialog;
import com.opencgl.selfpane.CustomTextDialog;
import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/16 13:39
 * @since v2.0
 */
@SuppressWarnings("unused")
public class DialogUtil {

    private static final CustomDialog customDialog = new CustomDialog();

    private static final CustomInfoDialog customInfoDialog = new CustomInfoDialog();

    /**
     * 将弹窗居中定位到当前聚焦的 Stage 所在屏幕位置。
     * 解决多屏环境下弹窗出现在主屏而非当前操作屏幕的问题。
     * <p>
     * 需在 Platform.runLater 中调用（弹窗 show 之后宽高才稳定）。
     *
     * @param dialog 要定位的弹窗
     */
    /**
     * 获取当前聚焦的窗口，若无则返回第一个 Stage（用于弹窗相对当前操作窗口居中）。
     */
    public static Window getFocusedWindow() {
        return Stage.getWindows().stream()
                .filter(w -> w instanceof Stage && w.isFocused())
                .findFirst()
                .orElseGet(() -> Stage.getWindows().isEmpty() ? null : Stage.getWindows().get(0));
    }

    public static void centerOnActiveWindow(Dialog<?> dialog) {
        Platform.runLater(() -> {
            Window owner = getFocusedWindow();
            if (owner == null)
                return;

            double dialogW = dialog.getDialogPane().getWidth();
            double dialogH = dialog.getDialogPane().getHeight();

            // 先按 owner 居中
            double x = owner.getX() + (owner.getWidth() - dialogW) / 2;
            double y = owner.getY() + (owner.getHeight() - dialogH - 100) / 2;

            // 找到 owner 所在的屏幕，对 x/y 做边界 clamp，避免低分辨率下弹窗超出屏幕
            javafx.geometry.Rectangle2D sb = Screen.getScreensForRectangle(
                    owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight()).stream().findFirst()
                    .map(Screen::getVisualBounds)
                    .orElseGet(() -> Screen.getPrimary().getVisualBounds());

            x = Math.max(sb.getMinX(), Math.min(x, sb.getMaxX() - dialogW));
            y = Math.max(sb.getMinY(), Math.min(y, sb.getMaxY() - dialogH));

        dialog.setX(x);
        dialog.setY(y);
        });
    }

    /**
     * 将弹窗居中到指定 owner 窗口所在屏幕，保证多屏时弹窗与主程序同屏。
     */
    public static void centerOnOwner(Dialog<?> dialog, Window owner) {
        if (owner == null) {
            centerOnActiveWindow(dialog);
            return;
        }
        Platform.runLater(() -> {
            double dialogW = dialog.getDialogPane().getWidth();
            double dialogH = dialog.getDialogPane().getHeight();
            double x = owner.getX() + (owner.getWidth() - dialogW) / 2;
            double y = owner.getY() + (owner.getHeight() - dialogH - 100) / 2;
            javafx.geometry.Rectangle2D sb = Screen.getScreensForRectangle(
                    owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight()).stream().findFirst()
                    .map(Screen::getVisualBounds)
                    .orElseGet(() -> Screen.getPrimary().getVisualBounds());
            x = Math.max(sb.getMinX(), Math.min(x, sb.getMaxX() - dialogW));
            y = Math.max(sb.getMinY(), Math.min(y, sb.getMaxY() - dialogH));
            dialog.setX(x);
            dialog.setY(y);
        });
    }

    public static String show() {
        if (Platform.isFxApplicationThread()) {
            applyCurrentTheme(customDialog);
            return customDialog.showAndWait().orElse("");
        }
        FutureTask<String> task = new FutureTask<>(() -> {
            applyCurrentTheme(customDialog);
            return customDialog.showAndWait().orElse("");
        });
        Platform.runLater(task);
        try {
            return task.get();
        } catch (Exception e) {
            return "";
        }
    }

    public static String show(String text) {
        if (Platform.isFxApplicationThread()) {
            applyCurrentTheme(customDialog);
            customDialog.setTextField(text);
            return customDialog.showAndWait().orElse(text);
        }
        FutureTask<String> task = new FutureTask<>(() -> {
            applyCurrentTheme(customDialog);
            customDialog.setTextField(text);
            return customDialog.showAndWait().orElse(text);
        });
        Platform.runLater(task);
        try {
            return task.get();
        } catch (Exception e) {
            return text;
        }
    }

    private static void applyCurrentTheme(Dialog<?> dialog) {
        if (dialog == null) return;
        try {
            List<String> sheets = ThemeManager.getInstance().getCurrentThemeStylesheets();
            javafx.scene.layout.Region root = (javafx.scene.layout.Region) dialog.getDialogPane().getContent();
            if (root != null) {
                root.getStylesheets().setAll(sheets);
            }
            dialog.getDialogPane().getStylesheets().setAll(sheets);
        } catch (Exception e) {
            // 忽略样式加载失败
        }
    }

    public static void showSuccessInfo(String text) {
        Platform.runLater(() -> {
            customInfoDialog.setLabelText(text);
            customInfoDialog.showAndWait();
        });

    }

    public static void showErrorInfo(String text) {
        Platform.runLater(() -> {
            applyCurrentTheme(customInfoDialog);
            customInfoDialog.setCustomHeaderText(I18N.get("opencgl.main.error.header"));
            customInfoDialog.setLabelText(text);
            customInfoDialog.showAndWait();
        });
    }

    public static void showCustomTextInfo(String text) {
        Platform.runLater(() -> {
            Window owner = getFocusedWindow();
            CustomTextDialog customTextDialog = new CustomTextDialog(text);
            applyCurrentTheme(customTextDialog);
            if (owner != null) customTextDialog.initOwner(owner);
            customTextDialog.showAndWait();
        });
    }

    public static void showCustomTextInfo(String headerText, String text) {
        Platform.runLater(() -> {
            Window owner = getFocusedWindow();
            CustomTextDialog customTextDialog = new CustomTextDialog(text);
            applyCurrentTheme(customTextDialog);
            customTextDialog.setCustomHeaderText(headerText);
            if (owner != null) customTextDialog.initOwner(owner);
            customTextDialog.showAndWait();
        });
    }

    public static void showCustomTextInfo(String headerText, String text, String additionalText) {
        Platform.runLater(() -> {
            Window owner = getFocusedWindow();
            CustomTextDialog customTextDialog = new CustomTextDialog(text, additionalText);
            applyCurrentTheme(customTextDialog);
            customTextDialog.setCustomHeaderText(headerText);
            if (owner != null) customTextDialog.initOwner(owner);
            customTextDialog.showAndWait();
        });
    }

    public static boolean showConfirm(String header, String text) {
        final AtomicReference<Boolean> result = new AtomicReference<>(false);

        if (Platform.isFxApplicationThread()) {
            CustomConfirmDialog dialog = new CustomConfirmDialog();
            applyCurrentTheme(dialog);
            dialog.setCustomHeaderText(header);
            dialog.setLabelText(text);
            Optional<Boolean> res = dialog.showAndWait();
            return res.orElse(false);
        } else {
            // Block until done
            FutureTask<Boolean> task = new FutureTask<>(() -> {
                CustomConfirmDialog dialog = new CustomConfirmDialog();
                applyCurrentTheme(dialog);
                dialog.setCustomHeaderText(header);
                dialog.setLabelText(text);
                Optional<Boolean> res = dialog.showAndWait();
                return res.orElse(false);
            });
            Platform.runLater(task);
            try {
                return task.get();
            } catch (Exception e) {
                return false;
            }
        }
    }
}
