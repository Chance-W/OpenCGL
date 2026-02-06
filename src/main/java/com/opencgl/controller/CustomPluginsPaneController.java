package com.opencgl.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.opencgl.i18n.I18N;
import com.opencgl.model.CustomPluginEntry;
import com.opencgl.selfpane.OpenCGLVbox;
import com.opencgl.service.CustomPluginStorageService;
import com.opencgl.util.DialogUtil;
import com.opencgl.util.TooltipUtil;

import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * 自定义插件面板：搜索、新增、多选删除、帮助；卡片右键打开/修改/删除。
 */
public class CustomPluginsPaneController implements Initializable {

    private static final String PROP_CONTROLLER = "customPluginsPane.controller";

    @FXML
    private VBox rootContainer;
    @FXML
    private MFXTextField searchField;
    @FXML
    private Button addBtn;
    @FXML
    private Button multiSelectBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button helpBtn;
    @FXML
    private MFXScrollPane scrollPane;
    @FXML
    private FlowPane flowPane;

    private final CustomPluginStorageService storage = new CustomPluginStorageService();
    private final Set<String> selectedIds = new HashSet<>();
    private boolean multiSelectMode = false;

    private Consumer<CustomPluginEntry> openInTabHandler;
    private Predicate<String> isTabOpenPredicate;
    private Consumer<String> closeTabHandler;

    public void setOpenInTabHandler(Consumer<CustomPluginEntry> openInTabHandler) {
        this.openInTabHandler = openInTabHandler;
    }

    public void setIsTabOpenPredicate(Predicate<String> isTabOpenPredicate) {
        this.isTabOpenPredicate = isTabOpenPredicate;
    }

    public void setCloseTabHandler(Consumer<String> closeTabHandler) {
        this.closeTabHandler = closeTabHandler;
    }

    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        searchField.floatingTextProperty().bind(I18N.getBinding("opencgl.market.search_prompt"));
        addBtn.textProperty().bind(I18N.getBinding("opencgl.custom_plugin.add"));
        multiSelectBtn.textProperty().bind(I18N.getBinding("opencgl.custom_plugin.multiselect"));
        deleteBtn.textProperty().bind(I18N.getBinding("opencgl.custom_plugin.delete"));
        helpBtn.textProperty().bind(I18N.getBinding("opencgl.custom_plugin.help"));

        addBtn.setOnAction(e -> onAdd());
        multiSelectBtn.setOnAction(e -> toggleMultiSelect());
        deleteBtn.setOnAction(e -> onMultiDelete());
        helpBtn.setOnAction(e -> onHelp());

        searchField.textProperty().addListener((o, a, b) -> filterCards());

        if (rootContainer != null) {
            rootContainer.getProperties().put(PROP_CONTROLLER, this);
        }
    }

    public static CustomPluginsPaneController getControllerFromRoot(javafx.scene.Node root) {
        Object c = root != null ? root.getProperties().get(PROP_CONTROLLER) : null;
        return c instanceof CustomPluginsPaneController ? (CustomPluginsPaneController) c : null;
    }

    public void refreshList() {
        flowPane.getChildren().clear();
        selectedIds.clear();
        List<CustomPluginEntry> list = storage.loadAll();
        for (CustomPluginEntry entry : list) {
            flowPane.getChildren().add(buildCard(entry));
        }
    }

    private javafx.scene.Node buildCard(CustomPluginEntry entry) {
        javafx.scene.Node iconNode = buildIconNode(entry);
        StackPane iconPane = new StackPane(iconNode);
        iconPane.setMinSize(70, 50);
        iconPane.setPrefSize(70, 50);

        Label nameLabel = new Label(entry.getName());
        CheckBox selectCb = new CheckBox();
        selectCb.setUserData(entry.getId());
        selectCb.selectedProperty().addListener((o, a, selected) -> {
            if (Boolean.TRUE.equals(selected)) selectedIds.add(entry.getId());
            else selectedIds.remove(entry.getId());
        });

        selectCb.setVisible(multiSelectMode);
        selectCb.setManaged(multiSelectMode);

        OpenCGLVbox card = new OpenCGLVbox();
        card.getStyleClass().add("plugin-card-box");
        StackPane top = new StackPane(iconPane, selectCb);
        StackPane.setAlignment(selectCb, javafx.geometry.Pos.TOP_RIGHT);
        card.getChildren().addAll(top, nameLabel);
        card.setUserData(entry);
        card.getProperties().put("selectCb", selectCb);

        MFXContextMenuItem openItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.get("opencgl.custom_plugin.open"))
            .setOnAction(ev -> openEntry(entry))
            .get();
        MFXContextMenuItem editItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.get("opencgl.custom_plugin.edit"))
            .setOnAction(ev -> onEdit(entry))
            .get();
        MFXContextMenuItem deleteItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.get("opencgl.custom_plugin.delete"))
            .setOnAction(ev -> onDeleteOne(entry))
            .get();

        MFXContextMenu.Builder.build(card)
            .addItem(openItem)
            .addItem(editItem)
            .addItem(deleteItem)
            .setShowCondition(me -> me.getButton() == MouseButton.SECONDARY)
            .installAndGet();

        card.setOnMouseClicked(me -> {
            if (me.getButton() == MouseButton.PRIMARY && me.getClickCount() == 1 && !selectCb.isFocused())
                openEntry(entry);
        });

        return card;
    }

    private void toggleMultiSelect() {
        multiSelectMode = !multiSelectMode;
        if (!multiSelectMode) {
            selectedIds.clear();
        }
        updateCheckboxesVisibility();
    }

    private void updateCheckboxesVisibility() {
        for (javafx.scene.Node node : flowPane.getChildren()) {
            Object cb = node.getProperties().get("selectCb");
            if (cb instanceof CheckBox) {
                ((CheckBox) cb).setSelected(false);
                ((CheckBox) cb).setVisible(multiSelectMode);
                ((CheckBox) cb).setManaged(multiSelectMode);
            }
        }
        if (!multiSelectMode) {
            selectedIds.clear();
        }
    }

    /**
     * 预设图标用 MFXFontIcon，本地文件用 ImageView，否则默认 logo
     */
    private javafx.scene.Node buildIconNode(CustomPluginEntry entry) {
        String path = entry.getIconPath();
        if (path != null && !path.isEmpty()) {
            if (path.startsWith("fas-") || path.startsWith("far-")) {
                MFXFontIcon icon = new MFXFontIcon(path, 32);
                StackPane wrap = new StackPane(icon);
                wrap.setAlignment(javafx.geometry.Pos.CENTER);
                return wrap;
            }
            try {
                File f = new File(path);
                if (f.exists()) {
                    Image img = new Image(f.toURI().toString(), 70, 50, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(70);
                    iv.setFitHeight(50);
                    iv.setPreserveRatio(true);
                    return iv;
                }
            }
            catch (Exception ignored) {
            }
        }
        Image img = new Image(Objects.requireNonNull(getClass().getResource("/com/opencgl/icon/logo.png")).toExternalForm(), 70, 50, true, true);
        ImageView iv = new ImageView(img);
        iv.setFitWidth(70);
        iv.setFitHeight(50);
        iv.setPreserveRatio(true);
        return iv;
    }

    private void openEntry(CustomPluginEntry entry) {
        if (openInTabHandler != null) {
            openInTabHandler.accept(entry);
            searchField.clear();
        }
    }

    private void onAdd() {
        Window w = addBtn.getScene() != null ? addBtn.getScene().getWindow() : null;
        Optional<CustomPluginEntry> result = CustomPluginEditDialog.showAdd(w);
        result.ifPresent(entry -> {
            storage.save(entry);
            refreshList();
        });
    }

    private void onEdit(CustomPluginEntry entry) {
        if (isTabOpenPredicate != null && isTabOpenPredicate.test(entry.getId())) {
            TooltipUtil.showToast(I18N.get("opencgl.custom_plugin.tab_open_please_close"));
            return;
        }
        Window w = flowPane.getScene() != null ? flowPane.getScene().getWindow() : null;
        Optional<CustomPluginEntry> result = CustomPluginEditDialog.showEdit(w, entry);
        result.ifPresent(updated -> {
            storage.update(updated);
            refreshList();
        });
    }

    private void onDeleteOne(CustomPluginEntry entry) {
        if (!DialogUtil.showConfirm(I18N.get("opencgl.custom_plugin.delete"), I18N.get("opencgl.custom_plugin.delete_confirm")))
            return;
        if (closeTabHandler != null) closeTabHandler.accept(entry.getId());
        storage.deleteById(entry.getId());
        selectedIds.remove(entry.getId());
        refreshList();
    }

    private void onMultiDelete() {
        if (!multiSelectMode) {
            TooltipUtil.showToast(I18N.get("opencgl.custom_plugin.multiselect_first"));
            return;
        }
        if (selectedIds.isEmpty()) {
            TooltipUtil.showToast(I18N.get("opencgl.custom_plugin.select_then_delete"));
            return;
        }
        if (!DialogUtil.showConfirm(I18N.get("opencgl.custom_plugin.delete"), I18N.get("opencgl.custom_plugin.delete_confirm")))
            return;
        List<String> ids = new ArrayList<>(selectedIds);
        for (String id : ids) {
            if (closeTabHandler != null) closeTabHandler.accept(id);
            storage.deleteById(id);
        }
        refreshList();
    }

    private void onHelp() {
        String text = I18N.get("opencgl.custom_plugin.help.content");
        DialogUtil.showCustomTextInfo(I18N.get("opencgl.custom_plugin.help"), text, null);
    }

    private void filterCards() {
        String filter = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        flowPane.getChildren().forEach(node -> {
            Object ud = node.getUserData();
            String name = ud instanceof CustomPluginEntry ? ((CustomPluginEntry) ud).getName() : "";
            boolean visible = filter.isEmpty() || (name != null && name.toLowerCase().contains(filter));
            node.setVisible(visible);
            node.setManaged(visible);
        });
    }
}
