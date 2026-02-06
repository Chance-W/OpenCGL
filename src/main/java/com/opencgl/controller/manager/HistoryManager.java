package com.opencgl.controller.manager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.opencgl.base.controls.CustomTextArea;
import com.opencgl.i18n.I18N;
import com.opencgl.util.DialogUtil;
import com.opencgl.base.utils.OperationHisRecord;
import com.opencgl.util.TooltipUtil;

import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 历史记录管理器
 * 负责创建和管理操作历史标签页
 *
 * @author Chance.W
 * @since v2.0
 */
public class HistoryManager {

    // private static final String TAB_TITLE = "操作历史"; // Deprecated inside I18N

    /**
     * 创建历史记录标签页
     *
     * @param rootPane 根面板（用于显示 Toast）
     * @return 历史记录标签页
     */
    public static Tab createHistoryTab(Pane rootPane) {
        Tab tab = new Tab();
        tab.textProperty().bind(I18N.getBinding("opencgl.history.tab_title"));
        tab.getProperties().put("isHistoryTab", true);
        tab.setClosable(true);

        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));

        // 顶部工具栏
        HBox topBar = createTopBar();
        MFXComboBox<String> dateComboBox = (MFXComboBox<String>) topBar.getChildren().get(1);
        MFXFontIcon refreshIcon = (MFXFontIcon) topBar.getChildren().get(2);
        MFXFontIcon clearIcon = (MFXFontIcon) topBar.getChildren().get(3);

        // 内容区域
        CustomTextArea textArea = new CustomTextArea();
        textArea.setEditable(false);
        textArea.setWrapText(false);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        contentBox.getChildren().addAll(topBar, textArea);
        tab.setContent(contentBox);

        // 加载内容逻辑
        Consumer<String> loadContent = date -> {
            if (date != null) {
                CompletableFuture.runAsync(() -> {
                    String content = OperationHisRecord.read(date);
                    Platform.runLater(() -> {
                        textArea.setText(content);
                        textArea.layout();
                        textArea.moveTo(textArea.getLength());
                        textArea.requestFollowCaret();
                    });
                });
            }
            else {
                textArea.clear();
            }
        };

        // 加载日期列表
        Runnable loadDates = () -> {
            List<String> dates = OperationHisRecord.getHistoryDates();
            Platform.runLater(() -> {
                String selected = dateComboBox.getValue();
                dateComboBox.setItems(FXCollections.observableArrayList(dates));

                if (dates.isEmpty()) {
                    dateComboBox.clearSelection();
                    textArea.clear();
                }
                else if (selected != null && dates.contains(selected)) {
                    dateComboBox.getSelectionModel().selectItem(selected);
                    loadContent.accept(selected);
                }
                else {
                    dateComboBox.selectFirst();
                }
            });
        };

        // 初始加载
        CompletableFuture.runAsync(loadDates);

        // 刷新操作
        refreshIcon.setOnMouseClicked(e -> {
            CompletableFuture.runAsync(loadDates);
            TooltipUtil.showToast(rootPane, I18N.get("opencgl.history.refreshed"));
        });

        // 清空操作
        clearIcon.setOnMouseClicked(e -> {
            String selectedDate = dateComboBox.getValue();
            if (selectedDate != null) {
                if (DialogUtil.showConfirm(I18N.get("opencgl.history.confirm"), I18N.get("opencgl.history.confirm.clear", selectedDate))) {
                    CompletableFuture.runAsync(() -> {
                        boolean success = OperationHisRecord.clear(selectedDate);
                        Platform.runLater(() -> {
                            if (success) {
                                TooltipUtil.showToast(rootPane, I18N.get("opencgl.history.cleared"));
                                loadDates.run();
                            }
                            else {
                                DialogUtil.showErrorInfo(I18N.get("opencgl.history.clear_failed"));
                            }
                        });
                    });
                }
            }
        });

        // 日期选择监听
        dateComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            loadContent.accept(newVal);
        });

        return tab;
    }

    /**
     * 创建顶部工具栏
     */
    private static HBox createTopBar() {
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        MFXComboBox<String> dateComboBox = new MFXComboBox<>();
        dateComboBox.setPrefWidth(200);
        dateComboBox.setFloatMode(io.github.palexdev.materialfx.enums.FloatMode.BORDER);
        dateComboBox.floatingTextProperty().bind(I18N.getBinding("opencgl.history.select_date"));

        MFXFontIcon refreshIcon = new MFXFontIcon("fas-rotate", 20);
        refreshIcon.setStyle("-fx-cursor: hand;");
        Tooltip refreshTooltip = new Tooltip();
        refreshTooltip.textProperty().bind(I18N.getBinding("opencgl.history.refresh"));
        Tooltip.install(refreshIcon, refreshTooltip);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        MFXFontIcon clearIcon = new MFXFontIcon("fas-trash", 20);
        clearIcon.setStyle("-fx-cursor: hand; -fx-text-fill: red;");
        Tooltip clearTooltip = new Tooltip();
        clearTooltip.textProperty().bind(I18N.getBinding("opencgl.history.clear"));
        Tooltip.install(clearIcon, clearTooltip);

        topBar.getChildren().addAll(spacer, dateComboBox, refreshIcon, clearIcon);
        return topBar;
    }

    /**
     * 检查标签是否为历史记录标签
     */
    public static boolean isHistoryTab(Tab tab) {
        return tab.getProperties().containsKey("isHistoryTab");
    }
}
