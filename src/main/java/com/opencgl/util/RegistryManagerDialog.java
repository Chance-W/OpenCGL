package com.opencgl.util;

import com.opencgl.base.listener.Config;
import com.opencgl.base.model.OpenCGLSelfProperties;
import com.opencgl.base.theme.ThemeManager;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.enums.FloatMode;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class RegistryManagerDialog {

    public static void showDialog(Runnable onCloseAction) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        stage.titleProperty().bind(com.opencgl.i18n.I18N.getBinding("opencgl.market.registry.title"));

        VBox rootBox = new VBox(20);
        rootBox.setPadding(new Insets(20));
        rootBox.setStyle(
                "-fx-background-color: -theme-bg-primary; -fx-border-color: -theme-border; -fx-border-width: 1px;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);
        Label closeBtn = new Label("✕");
        closeBtn.setStyle(
                "-fx-font-size: 18px; -fx-text-fill: -theme-text-secondary; -fx-cursor: hand; -fx-padding: 0 5 0 5;");
        closeBtn.setOnMouseEntered(e -> closeBtn
                .setStyle("-fx-font-size: 18px; -fx-text-fill: #ff4c4c; -fx-cursor: hand; -fx-padding: 0 5 0 5;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-font-size: 18px; -fx-text-fill: -theme-text-secondary; -fx-cursor: hand; -fx-padding: 0 5 0 5;"));
        closeBtn.setOnMouseClicked(ev -> stage.close());
        header.getChildren().add(closeBtn);

        final double[] xOffset = new double[] { 0 };
        final double[] yOffset = new double[] { 0 };
        header.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        header.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset[0]);
            stage.setY(event.getScreenY() - yOffset[0]);
        });

        Label titleLabel = new Label();
        titleLabel.textProperty().bind(com.opencgl.i18n.I18N.getBinding("opencgl.market.registry.header"));
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -theme-text-primary;");

        Label descLabel = new Label();
        descLabel.textProperty().bind(com.opencgl.i18n.I18N.getBinding("opencgl.market.registry.description"));
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -theme-text-secondary; -fx-opacity: 0.8;");

        ListView<String> registryListView = new ListView<>();
        registryListView.setPrefSize(700, 450);

        // 允许非编辑区获取焦点，从而使输入框丢失焦点触发保存
        rootBox.setOnMousePressed(e -> rootBox.requestFocus());
        registryListView.setOnMousePressed(e -> registryListView.requestFocus());

        registryListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox cellRoot = new HBox(15);
                    cellRoot.setAlignment(Pos.CENTER_LEFT);
                    cellRoot.setPadding(new Insets(8, 10, 8, 10));

                    boolean isLocal = item.startsWith("file:/") || item.startsWith("/")
                            || item.matches("^[a-zA-Z]:\\\\.*");
                    MFXFontIcon icon = new MFXFontIcon(isLocal ? "fas-folder-open" : "fas-cloud", 16);
                    icon.setStyle("-mfx-color: " + (isLocal ? "#FF9800" : "-theme-accent") + ";");

                    Label urlLbl = new Label(item);
                    urlLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: -theme-text-primary;");

                    TextField editField = new TextField(item);
                    editField.setPrefWidth(550);
                    editField.setStyle(
                            "-fx-background-color: transparent; -fx-border-color: -theme-accent; -fx-border-width: 0 0 2 0; -fx-text-fill: -theme-text-primary;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    MFXFontIcon delIcon = new MFXFontIcon("fas-trash-can", 15);
                    delIcon.setStyle("-fx-text-fill: #ff4c4c; -fx-cursor: hand;");

                    HBox delBox = new HBox(delIcon);
                    delBox.setAlignment(Pos.CENTER);
                    delBox.setPadding(new Insets(4));
                    delBox.setOnMouseClicked(e -> {
                        getListView().getItems().remove(item);
                        saveRegistries(getListView());
                    });

                    // 编辑相关逻辑
                    Runnable commitEdit = () -> {
                        String newVal = editField.getText().trim();
                        if (!newVal.isEmpty() && !newVal.equals(item)) {
                            int index = getListView().getItems().indexOf(item);
                            if (index >= 0) {
                                getListView().getItems().set(index, newVal);
                                saveRegistries(getListView());
                            }
                        } else {
                            // 放弃修改，触发当前 ListCell 的重绘恢复状态
                            updateItem(item, false);
                        }
                    };

                    editField.focusedProperty().addListener((obs, oldV, newV) -> {
                        if (oldV && !newV) {
                            commitEdit.run();
                        }
                    });

                    editField.setOnKeyPressed(ke -> {
                        if (ke.getCode() == KeyCode.ENTER) {
                            commitEdit.run();
                        } else if (ke.getCode() == KeyCode.ESCAPE) {
                            updateItem(item, false);
                        }
                    });

                    Runnable startEdit = () -> {
                        cellRoot.getChildren().clear();
                        cellRoot.getChildren().addAll(delBox, icon, editField, spacer);
                        Platform.runLater(() -> {
                            editField.requestFocus();
                            editField.selectAll();
                        });
                    };

                    // 原先从 cellRoot 添加节点 (改变布局使其靠左：删除按钮，图标，URL文本，占位符在最右)
                    cellRoot.getChildren().addAll(delBox, icon, urlLbl, spacer);

                    // 双击激活修改
                    cellRoot.setOnMouseClicked(me -> {
                        if (me.getClickCount() == 2 && me.getButton() == MouseButton.PRIMARY) {
                            startEdit.run();
                        }
                    });

                    // 右键菜单
                    MFXContextMenu contextMenu = new MFXContextMenu(cellRoot);
                    MFXContextMenuItem editItem = new MFXContextMenuItem("");
                    editItem.textProperty().bind(com.opencgl.i18n.I18N.getBinding("opencgl.market.registry.modify"));
                    editItem.setOnAction(e -> startEdit.run());
                    MFXContextMenuItem deleteItem = new MFXContextMenuItem("");
                    deleteItem.textProperty().bind(com.opencgl.i18n.I18N.getBinding("opencgl.market.registry.delete"));
                    deleteItem.setOnAction(e -> {
                        getListView().getItems().remove(item);
                        saveRegistries(getListView());
                    });
                    contextMenu.getItems().addAll(editItem, deleteItem);

                    setGraphic(cellRoot);
                }
            }
        });

        String savedUrls = Config.readExternalConfigure(OpenCGLSelfProperties.REGISTRY_URLS_KEY);
        if (savedUrls != null && !savedUrls.trim().isEmpty()) {
            String[] urlsArr = savedUrls.split(",");
            for (String u : urlsArr) {
                if (!u.trim().isEmpty()) {
                    registryListView.getItems().add(u.trim());
                }
            }
        } else {
            registryListView.getItems().add("https://gitee.com/chance_w/opencgl-plugins-registry/raw/main/index.json");
            saveRegistries(registryListView);
        }

        MFXTextField newRegistryField = new MFXTextField();
        newRegistryField.promptTextProperty()
                .bind(com.opencgl.i18n.I18N.getBinding("opencgl.market.registry.new_prompt"));
        newRegistryField.setFloatMode(FloatMode.BORDER);
        newRegistryField.setPrefWidth(630);

        MFXFontIcon addIcon = new MFXFontIcon("fas-plus", 14);
        addIcon.setStyle("-mfx-color: white;");
        MFXButton addRegistryBtn = new MFXButton("", addIcon);
        addRegistryBtn.setButtonType(ButtonType.RAISED);
        addRegistryBtn.setStyle("-fx-background-color: -theme-accent; -fx-text-fill: white;");
        addRegistryBtn.setPrefSize(40, 40);

        addRegistryBtn.setOnAction(e -> {
            String url = newRegistryField.getText().trim();
            if (!url.isEmpty() && !registryListView.getItems().contains(url)) {
                registryListView.getItems().add(url);
                newRegistryField.clear();
                saveRegistries(registryListView);
            }
        });

        HBox addBox = new HBox(15, newRegistryField, addRegistryBtn);
        addBox.setAlignment(Pos.CENTER_LEFT);

        rootBox.getChildren().addAll(header, titleLabel, descLabel, registryListView, addBox);

        Scene scene = new Scene(rootBox);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                stage.close();
            }
        });
        stage.setScene(scene);

        stage.setOnShown(e -> {
            ThemeManager.getInstance().registerScene(scene);
        });

        stage.setOnHidden(e -> {
            ThemeManager.getInstance().unregisterScene(scene);
            if (onCloseAction != null) {
                onCloseAction.run();
            }
        });

        stage.showAndWait();
    }

    private static void saveRegistries(ListView<String> listView) {
        String joinedUrls = String.join(",", listView.getItems());
        Map<String, String> configMap = new HashMap<>();
        configMap.put(OpenCGLSelfProperties.REGISTRY_URLS_KEY, joinedUrls);
        Config.updateExternalConfigure(configMap);
    }

    private static boolean checkFocusWithin(javafx.scene.Node parent, javafx.scene.Node child) {
        javafx.scene.Node node = child;
        while (node != null) {
            if (node == parent)
                return true;
            node = node.getParent();
        }
        return false;
    }
}
