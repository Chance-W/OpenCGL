package com.opencgl.controller;

import java.io.File;
import java.nio.file.Files;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.base.theme.ThemeManager;
import com.opencgl.i18n.I18N;
import com.opencgl.model.CustomPluginEntry;
import com.opencgl.util.DialogUtil;
import com.opencgl.util.TooltipUtil;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * 新增/编辑自定义插件弹框。可继承 com.opencgl.base.view.BaseCustomDialog 实现（若 base 提供）。
 * 当前实现为 Dialog&lt;CustomPluginEntry&gt;，表单项：名称、URL/路径、图标（预设 + 本地图片，本地图做压缩）。
 */
public class CustomPluginEditDialog extends Dialog<CustomPluginEntry> {
    private static final Logger log = LoggerFactory.getLogger(CustomPluginEditDialog.class);
    private static final int ICON_MAX_SIZE = 128;
    private static final String[] PRESET_ICONS = {"fas-link", "fas-globe", "fas-file-code", "fas-book", "fas-puzzle-piece", "fas-star"};

    private final MFXTextField nameField;
    private final MFXTextField urlField;
    private final MFXComboBox<String> presetIconCombo;
    private final Label localIconLabel;
    private final StackPane iconPreviewPane;
    private String localIconPath;
    private final CustomPluginEntry editingEntry;
    private Window ownerWindow;

    public CustomPluginEditDialog(CustomPluginEntry existing) {
        this.editingEntry = existing;
        setTitle(existing == null ? I18N.get("opencgl.custom_plugin.add") : I18N.get("opencgl.custom_plugin.edit"));
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UNDECORATED);
        getDialogPane().getStyleClass().add("opencgl-dialog");

        nameField = new MFXTextField();
        nameField.setPromptText(I18N.get("opencgl.custom_plugin.dialog.name"));
        nameField.setPrefWidth(320);
        urlField = new MFXTextField();
        urlField.setPromptText(I18N.get("opencgl.custom_plugin.dialog.url_or_path"));
        urlField.setPrefWidth(320);

        presetIconCombo = new MFXComboBox<>();
        presetIconCombo.getItems().add(""); // 默认
        for (String icon : PRESET_ICONS) {
            presetIconCombo.getItems().add(icon);
        }
        presetIconCombo.setPrefWidth(200);
        iconPreviewPane = new StackPane();
        iconPreviewPane.setMinSize(32, 32);
        iconPreviewPane.setPrefSize(32, 32);
        iconPreviewPane.setAlignment(Pos.CENTER);
        localIconLabel = new Label(I18N.get("opencgl.custom_plugin.dialog.local_icon_none"));
        presetIconCombo.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                localIconPath = null;
                localIconLabel.setText(I18N.get("opencgl.custom_plugin.dialog.local_icon_none"));
            }
            updateIconPreview();
        });

        MFXButton chooseLocalBtn = new MFXButton(I18N.get("opencgl.custom_plugin.dialog.choose_local_icon"));
        chooseLocalBtn.setButtonType(ButtonType.RAISED);
        chooseLocalBtn.setOnAction(e -> chooseLocalIcon());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.add(new Label(I18N.get("opencgl.custom_plugin.dialog.name")), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label(I18N.get("opencgl.custom_plugin.dialog.url_or_path")), 0, 1);
        grid.add(urlField, 1, 1);
        grid.add(new Label(I18N.get("opencgl.custom_plugin.dialog.icon")), 0, 2);
        HBox iconRow = new HBox(10);
        iconRow.setAlignment(Pos.CENTER_LEFT);
        iconRow.getChildren().addAll(presetIconCombo, iconPreviewPane, chooseLocalBtn, localIconLabel);
        grid.add(iconRow, 1, 2);

        MFXButton okBtn = new MFXButton(I18N.get("opencgl.main.button.confirm"));
        okBtn.setButtonType(ButtonType.RAISED);
        okBtn.setOnAction(e -> onConfirm());
        MFXButton cancelBtn = new MFXButton(I18N.get("opencgl.main.button.cancel"));
        cancelBtn.setButtonType(ButtonType.RAISED);
        cancelBtn.setOnAction(e -> {
            setResult(null);
            close();
        });

        HBox buttons = new HBox(10, okBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(15, grid, buttons);
        root.setPadding(new Insets(15));
        getDialogPane().setContent(root);

        setResultConverter(btn -> null);

        if (existing != null) {
            nameField.setText(existing.getName());
            urlField.setText(existing.getUrlOrPath());
            if (StringUtils.isNotBlank(existing.getIconPath())) {
                if (new File(existing.getIconPath()).exists()) {
                    localIconPath = existing.getIconPath();
                    localIconLabel.setText(new File(localIconPath).getName());
                } else {
                    for (String icon : PRESET_ICONS) {
                        if (icon.equals(existing.getIconPath())) {
                            presetIconCombo.getSelectionModel().selectItem(icon);
                            break;
                        }
                    }
                }
            }
        }
        updateIconPreview();

        setOnShown(e -> {
            ThemeManager.getInstance().registerScene(getDialogPane().getScene());
            if (ownerWindow != null) {
                DialogUtil.centerOnOwner(this, ownerWindow);
            } else {
                DialogUtil.centerOnActiveWindow(this);
            }
            getDialogPane().getScene().addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == KeyCode.ESCAPE) {
                    setResult(null);
                    close();
                }
            });
        });
        setOnHidden(e -> {
            if (getDialogPane().getScene() != null)
                ThemeManager.getInstance().unregisterScene(getDialogPane().getScene());
        });
    }

    private void chooseLocalIcon() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        Window w = getDialogPane().getScene().getWindow();
        File f = fc.showOpenDialog(w);
        if (f == null) return;
        try {
            String compressed = compressAndSaveIcon(f);
            if (compressed != null) {
                localIconPath = compressed;
                presetIconCombo.getSelectionModel().clearSelection();
                localIconLabel.setText(f.getName());
                updateIconPreview();
            }
        } catch (Exception ex) {
            log.warn("Local icon load/compress failed", ex);
            localIconPath = f.getAbsolutePath();
            localIconLabel.setText(f.getName());
            updateIconPreview();
        }
    }

    private void updateIconPreview() {
        iconPreviewPane.getChildren().clear();
        if (StringUtils.isNotBlank(localIconPath)) {
            try {
                File f = new File(localIconPath);
                if (f.exists()) {
                    ImageView iv = new ImageView(new Image(f.toURI().toString(), 32, 32, true, true));
                    iv.setFitWidth(32);
                    iv.setFitHeight(32);
                    iv.setPreserveRatio(true);
                    iconPreviewPane.getChildren().add(iv);
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        String preset = presetIconCombo.getSelectionModel().getSelectedItem();
        if (StringUtils.isNotBlank(preset)) {
            MFXFontIcon icon = new MFXFontIcon(preset, 24);
            iconPreviewPane.getChildren().add(icon);
        } else {
            ImageView iv = new ImageView(new Image(Objects.requireNonNull(getClass().getResource("/com/opencgl/icon/logo.png")).toExternalForm(), 32, 32, true, true));
            iv.setFitWidth(32);
            iv.setFitHeight(32);
            iv.setPreserveRatio(true);
            iconPreviewPane.getChildren().add(iv);
        }
    }

    /** 压缩并保存到临时或应用目录，返回可存储的路径；失败则返回原路径。 */
    private String compressAndSaveIcon(File source) {
        try {
            java.awt.image.BufferedImage img = ImageIO.read(source);
            if (img == null) return source.getAbsolutePath();
            int w = img.getWidth(), h = img.getHeight();
            if (w <= ICON_MAX_SIZE && h <= ICON_MAX_SIZE)
                return source.getAbsolutePath();
            double scale = Math.min((double) ICON_MAX_SIZE / w, (double) ICON_MAX_SIZE / h);
            int nw = (int) (w * scale), nh = (int) (h * scale);
            java.awt.Image scaled = img.getScaledInstance(nw, nh, java.awt.Image.SCALE_SMOOTH);
            java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(nw, nh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            out.getGraphics().drawImage(scaled, 0, 0, null);
            File tmp = Files.createTempFile("opencgl_custom_icon_", ".png").toFile();
            tmp.deleteOnExit();
            ImageIO.write(out, "png", tmp);
            return tmp.getAbsolutePath();
        } catch (Exception e) {
            log.debug("Compress icon failed, use path as-is", e);
            return source.getAbsolutePath();
        }
    }

    private void onConfirm() {
        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        String url = urlField.getText() != null ? urlField.getText().trim() : "";
        if (name.isEmpty() || url.isEmpty()) {
            TooltipUtil.showToast(I18N.get("opencgl.custom_plugin.dialog.required_hint"));
            return;
        }
        CustomPluginEntry entry = editingEntry != null ? editingEntry : new CustomPluginEntry();
        if (editingEntry == null) {
            entry.setId(UUID.randomUUID().toString());
            entry.setCreatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
        entry.setName(name);
        entry.setUrlOrPath(url);
        if (StringUtils.isNotBlank(localIconPath)) {
            entry.setIconPath(localIconPath);
        } else {
            String preset = presetIconCombo.getSelectionModel().getSelectedItem();
            entry.setIconPath(preset != null ? preset : "");
        }
        entry.setSortOrder(entry.getSortOrder() != null ? entry.getSortOrder() : 0);
        setResult(entry);
        close();
    }

    public static Optional<CustomPluginEntry> showAdd(Window owner) {
        CustomPluginEditDialog d = new CustomPluginEditDialog(null);
        d.ownerWindow = owner;
        if (owner != null) d.initOwner(owner);
        return d.showAndWait();
    }

    public static Optional<CustomPluginEntry> showEdit(Window owner, CustomPluginEntry entry) {
        CustomPluginEditDialog d = new CustomPluginEditDialog(entry);
        d.ownerWindow = owner;
        if (owner != null) d.initOwner(owner);
        return d.showAndWait();
    }
}
