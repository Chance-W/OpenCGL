package com.opencgl.controller;

import cn.hutool.core.io.StreamProgress;
import com.opencgl.base.utils.ToastUtil;
import com.opencgl.i18n.I18N;
import com.opencgl.model.RemotePluginInfo;
import com.opencgl.api.PluginUI;
import com.opencgl.service.PluginMarketService;
import com.opencgl.service.PluginService;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.enums.ButtonType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import io.github.palexdev.materialfx.controls.MFXComboBox;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Insets;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PluginMarketController implements Initializable {

    @FXML
    private Label marketTitleLabel;
    @FXML
    private MFXButton manageRegistriesBtn;
    @FXML
    private MFXButton refreshFetchBtn;
    @FXML
    private MFXTextField searchField;
    @FXML
    private MFXComboBox<String> categoryComboBox;
    @FXML
    private MFXButton viewToggleBtn;
    @FXML
    private MFXFontIcon viewToggleIcon;
    @FXML
    private ListView<RemotePluginInfo> marketListView;
    @FXML
    private ScrollPane gridScrollPane;
    @FXML
    private FlowPane marketGridView;
    @FXML
    private StackPane toastPane;

    private final PluginMarketService marketService = new PluginMarketService();
    private final PluginService pluginService = new PluginService();

    // 不再使用遮罩挡住全屏
    // private final LoadingMask loadingMask = new LoadingMask();

    private List<RemotePluginInfo> allPlugins = new ArrayList<>();
    private Map<String, PluginUI> installedPluginsMap = new HashMap<>();
    private final javafx.beans.property.BooleanProperty isListView = new javafx.beans.property.SimpleBooleanProperty(
            true);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupMarketListView();

        // 绑定搜索与分类监听
        searchField.textProperty().addListener((obs, oldV, newV) -> filterPlugins());
        categoryComboBox.selectedItemProperty().addListener((obs, oldV, newV) -> filterPlugins());

        // 绑定静态标签与提示
        marketTitleLabel.textProperty().bind(I18N.getBinding("opencgl.market.title"));
        manageRegistriesBtn.textProperty().bind(I18N.getBinding("opencgl.market.manage_registries"));
        refreshFetchBtn.textProperty().bind(I18N.getBinding("opencgl.market.refresh_fetch"));
        searchField.promptTextProperty().bind(I18N.getBinding("opencgl.market.search_prompt"));
        categoryComboBox.promptTextProperty().bind(I18N.getBinding("opencgl.market.all_categories"));

        // 绑定视图切换按钮文本
        viewToggleBtn.textProperty()
                .bind(com.opencgl.i18n.I18N
                        .getBinding(() -> isListView.get() ? com.opencgl.i18n.I18N.get("opencgl.market.grid_view")
                                : com.opencgl.i18n.I18N.get("opencgl.market.list_view")));

        // 初始化默认视图
        switchToGridView();

        // 自动触发首次加载
        Platform.runLater(this::handleRefresh);
    }

    private void setupMarketListView() {
        marketListView.setCellFactory(param -> new ListCell<RemotePluginInfo>() {
            @Override
            protected void updateItem(RemotePluginInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox root = new HBox(15);
                    root.setAlignment(Pos.CENTER_LEFT);

                    VBox infoBox = new VBox(5);
                    Label nameLbl = new Label(item.getName() != null ? item.getName()
                            : com.opencgl.i18n.I18N.get("opencgl.market.unknown_plugin"));
                    nameLbl.getStyleClass().add("market-plugin-name");

                    String verText = com.opencgl.i18n.I18N.get("opencgl.market.version") + item.getVersion();
                    if (item.getMinAppVersion() != null) {
                        verText += com.opencgl.i18n.I18N.get("opencgl.market.minAppVersion") + item.getMinAppVersion();
                    }
                    Label verLbl = new Label(verText);
                    verLbl.getStyleClass().add("market-plugin-version");
                    infoBox.getChildren().addAll(nameLbl, verLbl);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    MFXButton actionBtn = buildActionButton(item);

                    root.getChildren().addAll(infoBox, spacer, actionBtn);

                    // 悬停提示
                    String desc = (item.getDescription() != null && !item.getDescription().isEmpty())
                            ? item.getDescription()
                            : com.opencgl.i18n.I18N.get("opencgl.market.no_description");
                    Tooltip tooltip = new Tooltip(desc);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(300);
                    Tooltip.install(root, tooltip);

                    setGraphic(root);
                }
            }
        });
    }

    private MFXButton buildActionButton(RemotePluginInfo item) {
        MFXButton actionBtn = new MFXButton();
        actionBtn.setButtonType(ButtonType.RAISED);
        actionBtn.getStyleClass().add("market-action-button");

        PluginUI installed = installedPluginsMap.get(item.getName());
        if (installed != null) {
            if (compareVersion(item.getVersion(), installed.version()) > 0) {
                actionBtn.setText(com.opencgl.i18n.I18N.get("opencgl.market.upgrade", item.getVersion()));
                setActionState(actionBtn, "market-action-upgrade");
                actionBtn.setOnAction(e -> handleDownload(item, actionBtn, installed));
            } else {
                actionBtn.setText(com.opencgl.i18n.I18N.get("opencgl.market.installed"));
                actionBtn.setDisable(true);
                setActionState(actionBtn, "market-action-installed");
            }
        } else {
            actionBtn.setText(com.opencgl.i18n.I18N.get("opencgl.market.install"));
            setActionState(actionBtn, "market-action-install");
            actionBtn.setOnAction(e -> handleDownload(item, actionBtn, null));
        }
        return actionBtn;
    }

    private void setActionState(MFXButton button, String stateClass) {
        button.getStyleClass().removeAll(
                "market-action-upgrade", "market-action-installed",
                "market-action-install", "market-action-error");
        button.getStyleClass().add(stateClass);
    }

    private int compareVersion(String v1, String v2) {
        if (v1 == null)
            v1 = "";
        if (v2 == null)
            v2 = "";
        String[] arr1 = v1.split("\\.");
        String[] arr2 = v2.split("\\.");
        int length = Math.max(arr1.length, arr2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < arr1.length
                    ? Integer.parseInt(
                            arr1[i].replaceAll("[^\\d]", "").isEmpty() ? "0" : arr1[i].replaceAll("[^\\d]", ""))
                    : 0;
            int num2 = i < arr2.length
                    ? Integer.parseInt(
                            arr2[i].replaceAll("[^\\d]", "").isEmpty() ? "0" : arr2[i].replaceAll("[^\\d]", ""))
                    : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    @FXML
    private void handleRefresh() {
        viewToggleBtn.setDisable(true);
        CompletableFuture.supplyAsync(() -> {
            installedPluginsMap = pluginService.loadPlugins().stream()
                    .collect(Collectors.toMap(PluginUI::name, p -> p, (a, b) -> a));
            return marketService.fetchRemotePlugins();
        }).whenComplete((result, ex) -> {
            Platform.runLater(() -> {
                viewToggleBtn.setDisable(false);
                if (ex != null) {
                    ToastUtil.show(toastPane, com.opencgl.i18n.I18N.get("opencgl.market.refresh.fail", ex.getMessage()),
                            false);
                } else {
                    allPlugins = result != null ? result : new ArrayList<>();
                    updateCategoryDropdown();
                    filterPlugins();
                    if (allPlugins.isEmpty()) {
                        ToastUtil.showWarning(toastPane, com.opencgl.i18n.I18N.get("opencgl.market.no_plugins"));
                    } else {
                        ToastUtil.show(toastPane,
                                com.opencgl.i18n.I18N.get("opencgl.market.refresh.success", allPlugins.size()), true);
                    }
                }
            });
        });
    }

    private void updateCategoryDropdown() {
        String current = categoryComboBox.getSelectedItem();
        Set<String> categories = new HashSet<>();
        categories.add(com.opencgl.i18n.I18N.get("opencgl.market.all_categories"));

        for (RemotePluginInfo info : allPlugins) {
            if (info.getCategory() != null && !info.getCategory().trim().isEmpty()) {
                categories.add(info.getCategory().trim());
            }
        }

        try {
            categoryComboBox.getSelectionModel().clearSelection();
            categoryComboBox.setItems(javafx.collections.FXCollections.observableArrayList(categories));
            if (categories.contains(current)) {
                categoryComboBox.selectItem(current);
            } else {
                categoryComboBox.selectItem(com.opencgl.i18n.I18N.get("opencgl.market.all_categories"));
            }
        } catch (Exception e) {
            // 忽略由于快速刷新造成的底层组件 MFXComboBox 内部的 Range 索引越界异常
        }
    }

    private void filterPlugins() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String category = categoryComboBox.getSelectedItem();

        List<RemotePluginInfo> filteredList = allPlugins.stream().filter(p -> {
            boolean matchKey = true;
            if (!keyword.isEmpty()) {
                matchKey = p.getName() != null && p.getName().toLowerCase().contains(keyword);
            }
            boolean matchCate = com.opencgl.i18n.I18N.get("opencgl.market.all_categories").equals(category) ||
                    (category == null) ||
                    (p.getCategory() != null && p.getCategory().trim().equals(category));
            return matchKey && matchCate;
        }).collect(Collectors.toList());

        // 更新 List 视图
        marketListView.getItems().setAll(filteredList);
        // 更新 Grid 视图
        renderGridView(filteredList);
    }

    @FXML
    private void toggleView() {
        if (isListView.get()) {
            switchToGridView();
        } else {
            switchToListView();
        }
    }

    private void switchToListView() {
        isListView.set(true);
        marketListView.setVisible(true);
        gridScrollPane.setVisible(false);
        viewToggleIcon.setDescription("fas-bars-staggered");
    }

    private void switchToGridView() {
        isListView.set(false);
        marketListView.setVisible(false);
        gridScrollPane.setVisible(true);
        viewToggleIcon.setDescription("fas-list");
    }

    private void renderGridView(List<RemotePluginInfo> plugins) {
        marketGridView.getChildren().clear();
        for (RemotePluginInfo item : plugins) {
            VBox card = new VBox(10);
            card.setPadding(new Insets(15));
            // 使用内建的 grid-pane 样式
            card.getStyleClass().add("grid-pane");
            card.setPrefSize(200, 160);
            card.setAlignment(Pos.CENTER);

            MFXFontIcon icon = new MFXFontIcon("fas-cube", 32);
            icon.getStyleClass().add("market-plugin-icon");

            Label nameLbl = new Label(item.getName() != null ? item.getName()
                    : com.opencgl.i18n.I18N.get("opencgl.market.unknown_plugin"));
            nameLbl.getStyleClass().add("market-plugin-name");

            Label verLbl = new Label("v" + item.getVersion());
            verLbl.getStyleClass().add("market-plugin-version");

            MFXButton actionBtn = buildActionButton(item);

            card.getChildren().addAll(icon, nameLbl, verLbl, actionBtn);

            // 悬停提示
            String desc = (item.getDescription() != null && !item.getDescription().isEmpty()) ? item.getDescription()
                    : com.opencgl.i18n.I18N.get("opencgl.market.no_description");
            Tooltip tooltip = new Tooltip(desc);
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(300);
            Tooltip.install(card, tooltip);

            marketGridView.getChildren().add(card);
        }
    }

    private void handleDownload(RemotePluginInfo info, MFXButton btn, PluginUI oldPluginData) {
        if (info.getDownloadUrl() == null || info.getDownloadUrl().isEmpty()) {
            btn.setText(com.opencgl.i18n.I18N.get("opencgl.market.invalid_url"));
            return;
        }

        btn.setDisable(true);

        CompletableFuture.runAsync(() -> {
            try {
                String tmpDir = System.getProperty("java.io.tmpdir");
                String fileName = info.getName() + "-" + info.getVersion() + ".jar";
                java.io.File targetFile = new java.io.File(tmpDir, fileName);

                String downloadUrl = convertToRawUrl(info.getDownloadUrl());
                cn.hutool.core.io.StreamProgress progress = new StreamProgress() {
                    @Override
                    public void start() {
                        Platform.runLater(() -> btn.setText(com.opencgl.i18n.I18N.get("opencgl.market.downloading")));
                    }

                    @Override
                    public void progress(long total, long progressSize) {
                        Platform.runLater(() -> {
                            if (total > 0) {
                                btn.setText(com.opencgl.i18n.I18N.get("opencgl.market.download_progress",
                                        String.format("%d%%", (progressSize * 100) / total)));
                            } else {
                                btn.setText(com.opencgl.i18n.I18N.get("opencgl.market.download_progress",
                                        String.format("%d KB", progressSize / 1024)));
                            }
                        });
                    }

                    @Override
                    public void finish() {
                        Platform.runLater(() -> btn.setText(com.opencgl.i18n.I18N.get("opencgl.market.installing")));
                    }
                };

                if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
                    cn.hutool.http.HttpUtil.downloadFile(downloadUrl, targetFile, progress);
                } else {
                    Platform.runLater(() -> btn.setText(com.opencgl.i18n.I18N.get("opencgl.market.copy_local")));
                    String cleanPath = downloadUrl.toLowerCase().startsWith("file:///") ? downloadUrl.substring(7)
                            : (downloadUrl.toLowerCase().startsWith("file:/") ? downloadUrl.substring(5) : downloadUrl);
                    java.io.File srcFile = new java.io.File(cleanPath);
                    if (!srcFile.exists()) {
                        throw new RuntimeException(com.opencgl.i18n.I18N.get("opencgl.market.file_not_exist"));
                    }
                    cn.hutool.core.io.FileUtil.copy(srcFile, targetFile, true);
                    Thread.sleep(500); // 假装安装进度缓冲
                }

                // 如果是更新，且新插件文件已成功下载到本地临时目录，则开始物理卸载旧插件
                if (oldPluginData != null) {
                    Platform.runLater(() -> btn.setText(com.opencgl.i18n.I18N.get("opencgl.market.clean_old")));
                    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    Pane rootPane = (Pane) marketListView.getScene().getRoot();
                    pluginService.uninstallPlugin(oldPluginData, rootPane, () -> latch.countDown());
                    latch.await();
                }

                Platform.runLater(() -> {
                    btn.setText(com.opencgl.i18n.I18N.get("opencgl.market.loading"));
                    Pane rootPane = (Pane) marketListView.getScene().getRoot();
                    pluginService.installPlugin(targetFile, rootPane, () -> {
                        btn.setText(com.opencgl.i18n.I18N.get("opencgl.market.installed"));
                        setActionState(btn, "market-action-installed");
                        // 重新刷新左侧全量缓存以及状态
                        handleRefresh();
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btn.setDisable(false);
                    btn.setText(com.opencgl.i18n.I18N.get("opencgl.market.download_fail"));
                    setActionState(btn, "market-action-error");
                    String errMsg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                    ToastUtil.show(toastPane, "异常: " + errMsg, false);
                });
            }
        });
    }

    @FXML
    private void handleManageRegistries() {
        com.opencgl.util.RegistryManagerDialog.showDialog(this::handleRefresh);
    }

    private String convertToRawUrl(String url) {
        if (url == null)
            return url;
        if (url.contains("gitee.com") && url.contains("/blob/")) {
            return url.replace("/blob/", "/raw/");
        }
        if (url.contains("github.com") && url.contains("/blob/")) {
            return url.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/");
        }
        return url;
    }
}
