package com.opencgl.controller;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import com.opencgl.service.PluginUpdateService;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.base.utils.LoadingMask;
import com.opencgl.i18n.I18N;
import com.opencgl.i18n.Language;
import com.opencgl.base.listener.Config;
import com.opencgl.listener.FlexibleListener;
import com.opencgl.base.model.OpenCGLSelfProperties;
import com.opencgl.api.PluginUI;
import com.opencgl.selfpane.CglTabPane;
import com.opencgl.selfpane.OpenCGLVbox;
import com.opencgl.selfpane.SettingPane;
import com.opencgl.util.DialogUtil;
import com.opencgl.util.AsyncUiPipeline;
import com.opencgl.util.ShutdownCoordinator;

import com.opencgl.util.PluginParserHelper;
import com.opencgl.util.PluginIdentity;
import com.opencgl.util.TooltipUtil;
import com.opencgl.util.ThemeMenuText;
import com.opencgl.util.WindowGeometry;
import com.opencgl.icon.CategoryIconManager;
import com.opencgl.service.PluginClickStore;
import com.opencgl.service.PluginService;
import com.opencgl.service.PluginStateService;
import com.opencgl.service.PluginLifecycleManager;
import com.opencgl.service.UpdateService;
import com.opencgl.base.theme.ThemeManager;
import com.opencgl.base.theme.ThemeMode;
import com.opencgl.base.theme.AccentColor;
import com.opencgl.api.ThemeAware;
import com.opencgl.controller.adapter.PluginViewAdapter;
import com.opencgl.controller.handler.ContextMenuHandler;
import com.opencgl.model.CustomPluginEntry;
import com.opencgl.controller.manager.HistoryManager;
import com.opencgl.controller.manager.SidebarManager;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.controls.MFXTextField;

import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import io.github.palexdev.materialfx.utils.ToggleButtonsUtil;
import io.github.palexdev.materialfx.utils.others.loader.MFXLoader;
import io.github.palexdev.materialfx.utils.others.loader.MFXLoaderBean;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/02 23:43
 * @since v2.0
 *
 */

@SuppressWarnings("unused")
public class NewMainController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(NewMainController.class);

    /**
     * 侧栏固定入口：首页、全部、市场、自定义插件等，与插件分类分开维护便于扩展
     */
    private static final List<SidebarFixedEntry> FIXED_SIDEBAR_ENTRIES = List.of(new SidebarFixedEntry("HomePane", "com/opencgl/view/HomePane.fxml", "fas-circle-dot", "opencgl.sidebar.home", true),
        new SidebarFixedEntry("allPane", "com/opencgl/view/GeneralComponentsPane.fxml", "fas-bars-progress", "opencgl.sidebar.all", false),
        new SidebarFixedEntry("marketPane", "com/opencgl/view/PluginMarketPane.fxml", "fas-store", "opencgl.sidebar.market", false),
        new SidebarFixedEntry("customPluginsPane", "com/opencgl/view/CustomPluginsPane.fxml", "fas-puzzle-piece", "opencgl.sidebar.custom_plugins", false));

    private record SidebarFixedEntry(String viewName, String resourcePath, String icon, String labelKey,
                                     boolean defaultRoot) {
    }

    private static int tag = 1;

    private final Stage stage;

    private double xOffset;
    private double yOffset;
    /**
     * 最大化前记录的真实窗口宽高，还原时使用，避免直接用 prefWidth/prefHeight 导致小屏超界
     */
    private double widthOffset = -1;
    private double heightOffset = -1;
    /**
     * 全屏前记录的窗口位置与尺寸，退出全屏时还原
     */
    private double fsRestoreX, fsRestoreY, fsRestoreW, fsRestoreH;
    private final ToggleGroup toggleGroup;

    // 记录进入标签页前左侧最后一个选中的分类Toggle按钮
    private ToggleButton lastSelectedCategoryToggle;

    /**
     * 根据实时点击频次排序，并重建流式面板节点，实现免重启刷新
     */
    private static final int DEFERRED_REORDER_DELAY_MS = 350;

    /**
     * 每个 runLater 内最多添加的卡片数，避免单次占用 FX 线程过久
     */
    private static final int REORDER_CHUNK_SIZE = 4;

    @FXML
    private HBox autoGroupHBox;

    @FXML
    private HBox windowHeader;

    @FXML
    private MFXFontIcon handUp;

    @FXML
    private MFXFontIcon switchToTabPageIcon;

    @FXML
    private MFXFontIcon closeIcon;

    @FXML
    private MFXFontIcon minimizeIcon;

    @FXML
    private MFXFontIcon maximizeIcon;

    @FXML
    private MFXFontIcon fullscreenIcon;

    @FXML
    private MFXFontIcon settingIcon;

    @FXML
    private MFXFontIcon themeToggleIcon;

    @FXML
    private MFXFontIcon languageToggleIcon;

    @FXML
    private MFXFontIcon dropDownIcon;

    @FXML
    private MFXFontIcon questionIcon;

    @FXML
    private MFXFontIcon upgradeCheckIcon;

    @FXML
    private AnchorPane rootPane;

    @FXML
    private MFXScrollPane scrollPane;

    @FXML
    private VBox sidebar;

    @FXML
    private VBox navBar;

    @FXML
    private StackPane contentPane;

    @FXML
    private StackPane logoContainer;

    @FXML
    private CglTabPane componentJfxTabPane;

    @FXML
    public Label componentsLabel;

    @FXML
    private Label sidebarHeaderLabel;

    private BorderPane homeRootPane = null;

    private final boolean contextMenuOpen = false;

    private SidebarManager sidebarManager;

    private final SettingPane settingPane = new SettingPane();

    private final Tooltip maxTip = new Tooltip();
    private final LoadingMask loadingMask = new LoadingMask();
    private final CompletableFuture<Void> initialContentReady = new CompletableFuture<>();

    private final java.util.List<FlowPane> pluginFlowPanes = new java.util.ArrayList<>();

    public NewMainController(Stage stage) {
        this.stage = stage;
        this.toggleGroup = new ToggleGroup();
        ToggleButtonsUtil.addAlwaysOneSelectedSupport(toggleGroup);
    }

    private final PluginService pluginService = new PluginService();
    private final PluginStateService pluginStateService = new PluginStateService();
    private final UpdateService updateService = new UpdateService();
    private final PluginClickStore pluginClickStore = new PluginClickStore();
    private final com.opencgl.service.PluginUpdateService pluginUpdateService = new PluginUpdateService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settingPane.setOnPluginStateChanged(this::reloadPlugins);
        // 加载图标配置
        CategoryIconManager.loadIconConfig();

        setComponentsI18n();
        new FlexibleListener(stage).enableDrag(rootPane);

        // 拖拽JAR包安装插件逻辑 (Delegated to PluginService)
        rootPane.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                boolean hasJar = event.getDragboard().getFiles().stream()
                    .anyMatch(f -> f.getName().toLowerCase().endsWith(".jar"));
                if (hasJar) {
                    event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                }
            }
            event.consume();
        });

        rootPane.setOnDragDropped(event -> {
            if (event.getDragboard().hasFiles()) {
                List<File> files = event.getDragboard().getFiles();

                // 过滤出 JAR 文件
                List<File> jarFiles = files.stream()
                    .filter(f -> f.getName().toLowerCase().endsWith(".jar"))
                    .toList();

                if (!jarFiles.isEmpty()) {
                    // 目前仅支持逐个安装，或者你可以扩展 service 支持批量
                    // 这里简化逻辑，遍历调用 service
                    for (File jarFile : jarFiles) {
                        pluginService.installPlugin(jarFile, rootPane, () -> {
                            try {
                                initializeLoader();
                            }
                            catch (Exception e) {
                                logger.error("Refresh plugins failed", e);
                            }
                        });
                    }
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });

        componentJfxTabPane.getTabs().addListener((ListChangeListener.Change<? extends Tab> c) -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    for (Tab removedTab : c.getRemoved()) {
                        Node content = removedTab.getContent();
                        if (content instanceof javafx.scene.web.WebView wv) {
                            wv.getEngine().load(null);
                            removedTab.setContent(null);
                        }

                        Object pluginObj = removedTab.getProperties().get("pluginInstance");
                        if (pluginObj instanceof PluginUI pluginInstance) {
                            try {
                                if (pluginInstance instanceof ThemeAware themeAware) {
                                    ThemeManager.getInstance().unregisterThemeAware(themeAware);
                                    logger.debug("Unregistered ThemeAware for: {}", pluginInstance.name());
                                }
                                PluginLifecycleManager.getInstance().disposeInstance(pluginInstance);
                                logger.info("Disposed plugin: {}", pluginInstance.name());
                            }
                            catch (Exception e) {
                                logger.error("Error during plugin cleanup: {}", pluginInstance.name(), e);
                            }
                        }
                    }
                }
            }
            if (componentJfxTabPane.getTabs().isEmpty()) {
                javafx.application.Platform.runLater(() -> {
                    if (lastSelectedCategoryToggle != null) {
                        lastSelectedCategoryToggle.setSelected(true);
                        if (lastSelectedCategoryToggle.getOnAction() != null) {
                            lastSelectedCategoryToggle.getOnAction().handle(new javafx.event.ActionEvent());
                        }
                    }
                    else if (!navBar.getChildren().isEmpty()) {
                        javafx.scene.control.ToggleButton homeToggle = (javafx.scene.control.ToggleButton) navBar.getChildren().getFirst();
                        homeToggle.setSelected(true);
                        if (homeToggle.getOnAction() != null) {
                            homeToggle.getOnAction().handle(new javafx.event.ActionEvent());
                        }
                    }
                });
            }
        });

        buildAndInitContextMenu();
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(I18N.getBinding("opencgl.main.hang.click.info"));
        tooltip.setShowDelay(Duration.seconds(3));
        Tooltip.install(handUp, tooltip);
        handUp.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (tag == 1) {
                    handUp.setDescription("fas-indent");
                    HBox.setMargin(handUp, new Insets(0, 0, 0, 10));
                    rootPane.getChildren().remove(sidebar);
                    AnchorPane.setLeftAnchor(contentPane, 0.0);
                    tag = 2;
                }
                else {
                    handUp.setDescription("fas-outdent");
                    HBox.setMargin(handUp, new Insets(0, 0, 0, sidebar.getPrefWidth() + 10));
                    rootPane.getChildren().add(sidebar);
                    AnchorPane.setLeftAnchor(contentPane, sidebar.getPrefWidth());
                    tag = 1;
                }
            }
        });

        Tooltip switchToTabTooltip = new Tooltip();
        switchToTabTooltip.textProperty().bind(I18N.getBinding("opencgl.main.switchToTabListPage"));
        switchToTabTooltip.setShowDelay(Duration.seconds(1));
        Tooltip.install(switchToTabPageIcon, switchToTabTooltip);
        switchToTabPageIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> switchToTabPage());

        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            // 显示 loading
            loadingMask.show(rootPane);

            ShutdownCoordinator shutdown = new ShutdownCoordinator(
                Executors.newSingleThreadScheduledExecutor(),
                CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS),
                () -> {
                    PluginParserHelper.closeAllClassLoaders();
                    logger.info("All plugin ClassLoaders closed");
                },
                () -> {
                    Platform.exit();
                    System.exit(0);
                },
                () -> {
                    logger.warn("Shutdown timeout (5s), forcing halt");
                    Runtime.getRuntime().halt(0);
                },
                java.time.Duration.ofSeconds(5)
            );
            shutdown.shutdown();
        });

        Tooltip closeTip = new Tooltip();
        closeTip.textProperty().bind(I18N.getBinding("opencgl.main.close.text"));
        closeTip.setShowDelay(Duration.ZERO);
        Tooltip.install(closeIcon, closeTip);

        minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED,
            event -> ((Stage) rootPane.getScene().getWindow()).setIconified(true));
        Tooltip minTip = new Tooltip();
        minTip.textProperty().bind(I18N.getBinding("opencgl.main.min.text"));
        minTip.setShowDelay(Duration.ZERO);
        Tooltip.install(minimizeIcon, minTip);

        Tooltip dropDownTip = new Tooltip();
        dropDownTip.textProperty().bind(I18N.getBinding("opencgl.main.dropDown.text"));
        dropDownTip.setShowDelay(Duration.seconds(3.0));
        Tooltip.install(dropDownIcon, dropDownTip);

        Tooltip questionTip = new Tooltip();
        questionTip.textProperty().bind(I18N.getBinding("opencgl.main.help.text"));
        questionTip.setShowDelay(Duration.seconds(1.0));
        Tooltip.install(questionIcon, questionTip);
        questionIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> jumpToHelpPage());

        Tooltip upgradeCheckTip = new Tooltip();
        upgradeCheckTip.textProperty().bind(I18N.getBinding("opencgl.main.upgrade.check.text"));
        questionTip.setShowDelay(Duration.seconds(1.0));
        Tooltip.install(upgradeCheckIcon, upgradeCheckTip);
        upgradeCheckIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            Platform.runLater(() -> loadingMask.show(contentPane));
            updateService.checkUpdate(true)
                .whenComplete((v, e) -> Platform.runLater(loadingMask::hide));
        });

        // ---- Theme Toggle Menu ----
        MFXContextMenu themeMenu = new MFXContextMenu(rootPane);
        populateThemeMenu(themeMenu);
        themeToggleIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            populateThemeMenu(themeMenu);
            Bounds bounds = themeToggleIcon.localToScreen(themeToggleIcon.getBoundsInLocal());
            if (bounds != null) {
                themeMenu.show(themeToggleIcon, bounds.getMinX(), bounds.getMaxY() + 5);
            }
        });
        Tooltip themeTip = new Tooltip();
        themeTip.textProperty().bind(I18N.getBinding("opencgl.main.theme.text"));
        themeTip.setShowDelay(Duration.ZERO);
        Tooltip.install(themeToggleIcon, themeTip);

        // ---- Language Toggle Menu ----
        MFXContextMenu langMenu = new MFXContextMenu(rootPane);
        Language[] supportedLangs = {Language.SIMPLIFIED_CHINESE, Language.ENGLISH};
        for (Language lang : supportedLangs) {
            String label = lang == Language.SIMPLIFIED_CHINESE ? "简体中文" : "English";
            MFXContextMenuItem item = new MFXContextMenuItem(label);
            item.setOnAction(e -> {
                I18N.setLanguage(lang);
                Config.updateSingleConfig(OpenCGLSelfProperties.LANGUAGE_KEY, lang.name());
                TooltipUtil.showToast(I18N.get("opencgl.lang.changed.toast"));
            });
            langMenu.getItems().add(item);
        }
        languageToggleIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            Bounds bounds = languageToggleIcon.localToScreen(languageToggleIcon.getBoundsInLocal());
            if (bounds != null) {
                langMenu.show(languageToggleIcon, bounds.getMinX(), bounds.getMaxY() + 5);
            }
        });
        Tooltip langTip = new Tooltip();
        langTip.textProperty().bind(I18N.getBinding("opencgl.main.language.text"));
        langTip.setShowDelay(Duration.ZERO);
        Tooltip.install(languageToggleIcon, langTip);

        settingIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> settingPane.show(rootPane));
        Tooltip settingTip = new Tooltip();
        settingTip.textProperty().bind(I18N.getBinding("opencgl.main.setting.text"));
        settingTip.setShowDelay(Duration.ZERO);
        Tooltip.install(settingIcon, settingTip);

        Tooltip.install(maximizeIcon, maxTip);
        maxTip.setShowDelay(Duration.ZERO);
        maximizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> autoFillWindows());

        Tooltip fullscreenTip = new Tooltip();
        fullscreenTip.textProperty().bind(I18N.getBinding("opencgl.main.fullscreen.text"));
        fullscreenTip.setShowDelay(Duration.ZERO);
        Tooltip.install(fullscreenIcon, fullscreenTip);
        fullscreenIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> toggleFullScreen());
        stage.fullScreenProperty().addListener((o, was, now) -> {
            if (!now && fsRestoreW > 0) {
                stage.setX(fsRestoreX);
                stage.setY(fsRestoreY);
                stage.setWidth(fsRestoreW);
                stage.setHeight(fsRestoreH);
                fsRestoreW = 0;
            }
        });

        Tooltip movePosition = new Tooltip();
        movePosition.textProperty().bind(I18N.getBinding("opencgl.main.movePosition.text"));
        Tooltip.install(autoGroupHBox, movePosition);

        autoGroupHBox.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<>() {
            private long lastClickTime = 0;

            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    long currentTime = System.currentTimeMillis();
                    // 判断是否在双击时间间隔内进行了两次点击
                    // 定义双击时间间隔（毫秒）
                    int DOUBLE_CLICK_TIME_GAP = 300;
                    if (currentTime - lastClickTime < DOUBLE_CLICK_TIME_GAP) {
                        Platform.runLater(NewMainController.this::autoFillWindows);
                    }
                    lastClickTime = currentTime;
                }
            }
        });
        AtomicBoolean mouseHeldDown = new AtomicBoolean(false);
        AtomicReference<Double> xOffsetTest = new AtomicReference<>((double) 0);
        AtomicReference<Double> yOffsetTest = new AtomicReference<>((double) 0);
        autoGroupHBox.setOnMousePressed(event -> {
            xOffsetTest.set(stage.getX() - event.getScreenX());
            yOffsetTest.set(stage.getY() - event.getScreenY());
            autoGroupHBox.setOnMouseDragged(event1 -> {
                if (contextMenuOpen) {
                    return;
                }
                stage.setX(event1.getScreenX() + xOffsetTest.get());
                stage.setY(event1.getScreenY() + yOffsetTest.get());
            });
        });
        // 自绘最大化窗口拖到另一块屏幕后，按鼠标所在屏幕重新铺满可视区域。
        autoGroupHBox.setOnMouseReleased(event -> {
            if (isMaximizedForTip.get() && !stage.isFullScreen()) {
                maximizeOnScreenAt(event.getScreenX(), event.getScreenY());
            }
        });
        // 显示器拔插或系统重新排列屏幕时，避免最大化窗口遗留在不可见区域。
        Screen.getScreens().addListener((ListChangeListener<Screen>) change -> {
            if (isMaximizedForTip.get() && !stage.isFullScreen()) {
                Platform.runLater(() -> maximizeOnScreenAt(
                    stage.getX() + stage.getWidth() / 2,
                    stage.getY() + stage.getHeight() / 2));
            }
        });

        try {
            initializeLoader().whenComplete((ignored, failure) -> {
                if (failure == null) {
                    initialContentReady.complete(null);
                }
                else {
                    initialContentReady.completeExceptionally(failure);
                }
            });
        }
        catch (Exception e) {
            logger.error("", e);
            initialContentReady.completeExceptionally(e);
        }
        ScrollUtils.addSmoothScrolling(scrollPane);
        // The only way to get a fucking smooth image in this shitty framework
        Image image = new Image(
            String.valueOf(this.getClass().getClassLoader().getResource("com/opencgl/icon/logo_alt.png")), 64, 64,
            true, true, true);
        ImageView logo = new ImageView(image);
        Circle clip = new Circle(30);
        clip.centerXProperty().bind(logo.layoutBoundsProperty().map(Bounds::getCenterX));
        clip.centerYProperty().bind(logo.layoutBoundsProperty().map(Bounds::getCenterY));
        logo.setClip(clip);
        logoContainer.getChildren().add(logo);
        maxTip.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(() -> {
            // 这里我们无法直接感知窗口是否最大化，因为 autoFillWindows 是手动触发的
            // 但我们可以根据 stage 的状态或者记录的按钮文字意图来切换
            // 简单起见，我们可以在 autoFillWindows 里更新一个 property，然后在这里绑定它
            return I18N
                .getOrDefault(isMaximizedForTip.get() ? "opencgl.main.max.reduction" : "opencgl.main.max.maximize");
        }, I18N.localeProperty(), isMaximizedForTip));

        Platform.runLater(() -> updateService.checkUpdate(false));
    }

    private final javafx.beans.property.BooleanProperty isMaximizedForTip = new javafx.beans.property.SimpleBooleanProperty(
        false);

    private void jumpToHelpPage() {
        Desktop desktop = Desktop.getDesktop();
        try {
            desktop.browse(new URI(Config.readInternalConfigure(OpenCGLSelfProperties.PLUGIN_HELP_URL)));
        }
        catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void autoFillWindows() {
        // 用窗口中心点判定当前所在屏幕，避免拖到副屏后双击最大化误回主屏
        double centerX = stage.getX() + stage.getWidth() / 2;
        double centerY = stage.getY() + stage.getHeight() / 2;
        Screen currentScreen = Screen.getScreensForRectangle(centerX - 0.5, centerY - 0.5, 1, 1)
            .stream().findFirst().orElse(Screen.getPrimary());
        Rectangle2D screenBounds = currentScreen.getVisualBounds();

        double screenMinX = screenBounds.getMinX();
        double screenMinY = screenBounds.getMinY();
        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();

        // 判断是否已在当前屏幕最大化：位置贴近屏幕左上角且尺寸与屏幕相同
        boolean isMaximized = Math.abs(stage.getX() - screenMinX) < 1.0
            && Math.abs(stage.getY() - screenMinY) < 1.0
            && Math.abs(stage.getWidth() - screenWidth) < 1.0
            && Math.abs(stage.getHeight() - screenHeight) < 1.0;

        if (isMaximized) {
            // 还原到最大化前的尺寸和位置
            isMaximizedForTip.set(false);
            // 使用记录的真实宽高；如第一次则按屏幟 70% 说明默认
            double restoreW = widthOffset > 0 ? widthOffset : screenWidth * 0.7;
            double restoreH = heightOffset > 0 ? heightOffset : screenHeight * 0.7;
            WindowGeometry.Rect restoreBounds = WindowGeometry.fitInside(
                new WindowGeometry.Rect(xOffset, yOffset, restoreW, restoreH),
                new WindowGeometry.Rect(screenMinX, screenMinY, screenWidth, screenHeight),
                0.95,
                0.95);
            stage.setWidth(restoreBounds.width());
            stage.setHeight(restoreBounds.height());
            stage.setX(restoreBounds.x());
            stage.setY(restoreBounds.y());
        }
        else {
            // 记录当前位置与宽高，再最大化
            xOffset = stage.getX();
            yOffset = stage.getY();
            widthOffset = stage.getWidth();
            heightOffset = stage.getHeight();
            isMaximizedForTip.set(true);
            stage.setX(screenMinX);
            stage.setY(screenMinY);
            stage.setWidth(screenWidth);
            stage.setHeight(screenHeight);
        }
    }

    private void maximizeOnScreenAt(double screenX, double screenY) {
        Screen target = Screen.getScreensForRectangle(screenX - 0.5, screenY - 0.5, 1, 1)
            .stream().findFirst().orElse(Screen.getPrimary());
        Rectangle2D bounds = target.getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        isMaximizedForTip.set(true);
    }

    private void toggleFullScreen() {
        if (stage.isFullScreen()) {
            stage.setFullScreen(false);
            return;
        }
        // 用窗口中心点判定当前屏幕，副屏全屏时使用该屏完整边界铺满
        double centerX = stage.getX() + stage.getWidth() / 2;
        double centerY = stage.getY() + stage.getHeight() / 2;
        Screen currentScreen = Screen.getScreensForRectangle(centerX - 0.5, centerY - 0.5, 1, 1)
            .stream().findFirst().orElse(Screen.getPrimary());
        // 使用 getBounds() 获得该显示器完整区域，避免副屏比主屏大时两侧留空
        Rectangle2D screenBounds = currentScreen.getBounds();
        fsRestoreX = stage.getX();
        fsRestoreY = stage.getY();
        fsRestoreW = stage.getWidth();
        fsRestoreH = stage.getHeight();
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        // 先落到目标屏并设好尺寸，再进全屏，避免全屏用错屏或尺寸
        Platform.runLater(() -> stage.setFullScreen(true));
    }

    public CompletableFuture<Void> initialContentReady() {
        return initialContentReady;
    }

    private CompletableFuture<Void> initializeLoader() {
        long loadStartedNanos = System.nanoTime();
        boolean showReloadMask = rootPane.getScene() != null;
        if (showReloadMask) {
            loadingMask.show(rootPane);
        }
        CompletableFuture<Void> completion = new CompletableFuture<>();

        // 异步加载插件，避免阻塞 UI 线程
        CompletableFuture.supplyAsync(() -> {
            long scanStartedNanos = System.nanoTime();
            try {
                // 扫描插件 (IO密集型)
                List<PluginUI> pluginUIList = pluginService.loadPlugins();

                // 注入排序逻辑
                Map<String, Long> clickCounts = pluginClickStore.loadAll();
                pluginUIList.sort(java.util.Comparator.comparingLong(
                    p -> -clickCounts.getOrDefault(PluginIdentity.resolve(p), 0L)));

                // 分组
                Map<String, List<PluginUI>> menuMap = pluginService.groupPluginsByCategory(pluginUIList);
                // 预加载图标 (可能涉及文件IO)
                menuMap.keySet().forEach(CategoryIconManager::getIconForCategory);
                logger.info("插件扫描完成：{} 个插件，耗时 {} ms", pluginUIList.size(),
                    elapsedMillis(scanStartedNanos));
                return new Object[]{pluginUIList, menuMap};
            }
            catch (Exception e) {
                logger.error("插件扫描失败", e);
                throw new RuntimeException(e);
            }
        }).thenAcceptAsync(objects -> {
            // 回到 UI 线程构建界面
            @SuppressWarnings("unchecked")
            List<PluginUI> pluginUIList = (List<PluginUI>) objects[0];
            @SuppressWarnings("unchecked")
            Map<String, List<PluginUI>> menuMap = (Map<String, List<PluginUI>>) objects[1];

            try {
                MFXLoader loader = new MFXLoader();
                for (SidebarFixedEntry entry : FIXED_SIDEBAR_ENTRIES) {
                    var beanBuilder = MFXLoaderBean
                        .of(entry.viewName, this.getClass().getClassLoader().getResource(entry.resourcePath))
                        .setBeanToNodeMapper(() -> createToggle(entry.icon, I18N.getBinding(entry.labelKey)));
                    loader.addView((entry.defaultRoot ? beanBuilder.setDefaultRoot(true) : beanBuilder).get());
                }

                menuMap.forEach((s, pluginUIS) -> {
                    String iconName = CategoryIconManager.getIconForCategory(s);
                    // 分类名称：主程序若有 opencgl.category.<key> 则随语言切换，否则用插件的 directoryName()
                    String categoryKey = "opencgl.category." + s;
                    StringBinding categoryBinding =
                        I18N.getBundle(I18N.getLocale()).containsKey(categoryKey)
                            ? I18N.getBinding(categoryKey)
                            : Bindings.createStringBinding(() -> {
                            if (pluginUIS != null && !pluginUIS.isEmpty()) {
                                String dirName = pluginUIS.getFirst().directoryName();
                                if (dirName != null && !dirName.isBlank()) return dirName;
                            }
                            return s;
                        }, I18N.localeProperty());

                    loader.addView(MFXLoaderBean
                        .of(s, this.getClass().getClassLoader()
                            .getResource("com/opencgl/view/GeneralComponentsPane.fxml"))
                        .setBeanToNodeMapper(
                            () -> getSidebarManager().createToggleWithContextMenu(iconName, categoryBinding, s))
                        .get());
                });

                loader.setOnLoadedAction(beans -> {
                    long renderStartedNanos = System.nanoTime();
                    final Node[] customPluginsPaneRootRef = new Node[1];
                    List<ToggleButton> nodes = beans.stream()
                        .map(bean -> {
                            String viewName = bean.getViewName();
                            if (viewName.equals("allPane") || menuMap.containsKey(viewName)) {
                                VBox rootBox = (VBox) bean.getRoot();
                                HBox searchBar = (HBox) rootBox.getChildren().get(0);
                                MFXTextField searchField = (MFXTextField) searchBar.getChildren().getFirst();
                                MFXScrollPane scrollPane = (MFXScrollPane) rootBox.getChildren().get(1);
                                FlowPane flowPane = (FlowPane) scrollPane.getContent();
                                // 保存引用用于后续排序刷新
                                if (!pluginFlowPanes.contains(flowPane)) {
                                    pluginFlowPanes.add(flowPane);
                                }

                                // 记录当前面板应该包含的插件列表，便于后续实时排序刷新
                                List<PluginUI> targetPlugins = viewName.equals("allPane") ? pluginUIList
                                    : menuMap.get(viewName);
                                flowPane.setUserData(targetPlugins);

                                for (PluginUI pluginUI : targetPlugins) {
                                    try {
                                        Node card = buildMenuInfo(pluginUI);
                                        card.setUserData(pluginUI.name());
                                        flowPane.getChildren().add(card);
                                    }
                                    catch (Exception e) {
                                        logger.error("Error building card for {}", pluginUI.name(), e);
                                    }
                                }

                                initSearchField(searchField, flowPane);
                            }
                            else if (viewName.equals("customPluginsPane")) {
                                VBox root = (VBox) bean.getRoot();
                                if (root == null) {
                                    try {
                                        URL customPaneUrl = getClass().getClassLoader().getResource("com/opencgl/view/CustomPluginsPane.fxml");
                                        if (customPaneUrl != null) {
                                            FXMLLoader fxmlLoader = new FXMLLoader(customPaneUrl);
                                            root = fxmlLoader.load();
                                            customPluginsPaneRootRef[0] = root;
                                            CustomPluginsPaneController ctrl = fxmlLoader.getController();
                                            if (ctrl != null) {
                                                ctrl.setOpenInTabHandler(this::openCustomPluginInTab);
                                                ctrl.setIsTabOpenPredicate(this::isCustomPluginTabOpen);
                                                ctrl.setCloseTabHandler(this::closeCustomPluginTab);
                                                ctrl.refreshList();
                                            }
                                        }
                                        else {
                                            logger.warn("CustomPluginsPane.fxml resource not found");
                                        }
                                    }
                                    catch (Exception e) {
                                        logger.warn("Fallback load customPluginsPane failed", e);
                                    }
                                }
                                else {
                                    CustomPluginsPaneController ctrl = CustomPluginsPaneController.getControllerFromRoot(root);
                                    if (ctrl != null) {
                                        ctrl.setOpenInTabHandler(this::openCustomPluginInTab);
                                        ctrl.setIsTabOpenPredicate(this::isCustomPluginTabOpen);
                                        ctrl.setCloseTabHandler(this::closeCustomPluginTab);
                                        ctrl.refreshList();
                                    }
                                }
                            }

                            ToggleButton toggle = (ToggleButton) bean.getBeanToNodeMapper().get();
                            Node customPaneRoot = customPluginsPaneRootRef[0];
                            toggle.setOnAction(event -> {
                                try {
                                    if ("customPluginsPane".equals(bean.getViewName()) && customPaneRoot != null) {
                                        contentPane.getChildren().setAll(customPaneRoot);
                                    }
                                    else if (bean.getRoot() != null) {
                                        contentPane.getChildren().setAll(bean.getRoot());
                                    }
                                    else {
                                        logger.warn("View root is null for: {}", bean.getViewName());
                                    }
                                }
                                catch (Exception ex) {
                                    logger.error("Error switching to view: {}", bean.getViewName(), ex);
                                }
                            });

                            if (bean.isDefaultView() && bean.getRoot() != null) {
                                homeRootPane = (BorderPane) bean.getRoot();
                                contentPane.getChildren().setAll(homeRootPane);
                                toggle.setSelected(true);
                            }
                            return toggle;
                        }).collect(Collectors.toList());
                    navBar.getChildren().setAll(nodes);

                    // 版本检查不阻塞首屏显示。
                    CompletableFuture.runAsync(() -> {
                        List<com.opencgl.model.PluginUpdateInfo> updates = pluginUpdateService
                            .checkForUpdates(pluginUIList);
                        if (!updates.isEmpty()) {
                            Platform.runLater(() -> applyUpdateBadges(updates));
                        }
                    });

                    showPluginLoadFailures();
                    logger.info("主页与插件卡片构建完成：耗时 {} ms，总耗时 {} ms",
                        elapsedMillis(renderStartedNanos), elapsedMillis(loadStartedNanos));
                    completion.complete(null);
                });
                loader.start();
            }
            catch (Exception e) {
                logger.error("Error initializing loader UI", e);
                DialogUtil.showErrorInfo(I18N.getOrDefault("opencgl.startup.init.error", e.getMessage()));
                completion.completeExceptionally(e);
            }
        }, Platform::runLater).exceptionally(ex -> {
            logger.error("Fatal error in plugin loader", ex);
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            Platform.runLater(() -> DialogUtil.showErrorInfo(
                I18N.get("opencgl.plugin.load.fatal") + "\n" + I18N.get("opencgl.plugin.load.fatal.message", msg)));
            completion.completeExceptionally(ex);
            return null;
        });

        completion.whenComplete((ignored, failure) -> {
            if (showReloadMask) {
                Platform.runLater(loadingMask::hide);
            }
        });
        return completion;
    }

    private void showPluginLoadFailures() {
        Map<String, String> failures = PluginParserHelper.getLastLoadFailures();
        if (failures.isEmpty()) {
            return;
        }
        boolean pathInvalid = failures.containsKey(PluginParserHelper.KEY_PLUGIN_PATH_INVALID);
        Map<String, String> jarFailures = failures.entrySet().stream()
            .filter(e -> !PluginParserHelper.KEY_PLUGIN_PATH_INVALID.equals(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        StringBuilder body = new StringBuilder();
        if (pathInvalid) {
            body.append(I18N.get("opencgl.plugin.path_invalid"));
        }
        if (!jarFailures.isEmpty()) {
            if (!body.isEmpty()) body.append("\n\n");
            body.append(I18N.get("opencgl.plugin.load.some_failed.detail",
                jarFailures.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n"))));
        }
        DialogUtil.showCustomTextInfo(I18N.get("opencgl.plugin.load.some_failed"), body.toString());
    }

    private static long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private void applyUpdateBadges(List<com.opencgl.model.PluginUpdateInfo> updates) {
        List<String> updatableClasses = updates.stream()
            .map(u -> u.getLocalPlugin().getClass().getSimpleName())
            .toList();

        for (FlowPane fp : pluginFlowPanes) {
            for (Node node : fp.getChildren()) {
                if (node instanceof OpenCGLVbox card) {
                    Object pClass = card.getProperties().get("pluginClass");
                    if (pClass != null && updatableClasses.contains(pClass.toString())) {
                        Label badge = (Label) card.getProperties().get("updateBadge");
                        if (badge != null) {
                            badge.setVisible(true);
                        }
                        MFXContextMenuItem updateItem = (MFXContextMenuItem) card.getProperties().get("updateItem");
                        if (updateItem != null) {
                            com.opencgl.model.PluginUpdateInfo info = updates.stream()
                                .filter(u -> u.getLocalPlugin().getClass().getSimpleName()
                                    .equals(pClass.toString()))
                                .findFirst().orElse(null);
                            if (info != null && info.getRemoteInfo() != null) {
                                updateItem.setText(I18N.get("opencgl.plugin.upgrade_to", info.getRemoteInfo().getVersion()));
                            }
                        }
                    }
                }
            }
        }
    }

    private void initSearchField(MFXTextField searchField, FlowPane flowPane) {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.toLowerCase().trim();
            flowPane.getChildren().forEach(node -> {
                String pluginName = (String) node.getUserData();
                boolean visible = filter.isEmpty() ||
                    (pluginName != null && pluginName.toLowerCase().contains(filter));
                node.setVisible(visible);
                node.setManaged(visible);
            });
        });
    }

    /**
     * 从插件卡片向上找到 GeneralComponents 面板根 {@link VBox}，清空顶部搜索框（打开/切换 Tab 成功后调用）。
     */

    /**
     * 延迟后异步、分块重排全部分类卡片，不阻塞 UI。
     */
    private void scheduleDeferredReorder() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(DEFERRED_REORDER_DELAY_MS);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Map<String, Long> newCounts = pluginClickStore.loadAll();
            java.util.List<FlowPane> panesWithData = new java.util.ArrayList<>();
            for (FlowPane fp : pluginFlowPanes) {
                if (fp.getUserData() != null) {
                    panesWithData.add(fp);
                }
            }
            if (panesWithData.isEmpty()) {
                return;
            }
            Platform.runLater(() -> runDeferredReorderForPane(panesWithData, newCounts, 0));
        });
    }

    private void runDeferredReorderForPane(java.util.List<FlowPane> panes, Map<String, Long> newCounts, int index) {
        if (index >= panes.size()) {
            logger.debug("延迟重排插件卡片完成");
            return;
        }
        FlowPane fp = panes.get(index);
        @SuppressWarnings("unchecked")
        List<PluginUI> targetList = (List<PluginUI>) fp.getUserData();
        if (targetList == null) {
            Platform.runLater(() -> runDeferredReorderForPane(panes, newCounts, index + 1));
            return;
        }
        reorderPluginCardsChunked(fp, targetList, newCounts, REORDER_CHUNK_SIZE, () ->
            Platform.runLater(() -> runDeferredReorderForPane(panes, newCounts, index + 1)));
    }

    /**
     * 分块、链式 runLater 重排单个 FlowPane，每批最多添加 chunkSize 张卡片，完成后回调 whenDone（在 FX 线程）。
     * 调用方可在任意线程；clear 与添加均在 FX 的 runLater 中执行。
     */
    private void reorderPluginCardsChunked(FlowPane flowPane, List<PluginUI> targetList,
                                           Map<String, Long> clickCounts, int chunkSize, Runnable whenDone) {
        List<PluginUI> sortedList = new java.util.ArrayList<>(targetList);
        sortedList.sort(java.util.Comparator.comparingLong(
            p -> -clickCounts.getOrDefault(PluginIdentity.resolve(p), 0L)));

        Platform.runLater(() -> {
            flowPane.getChildren().clear();
            int end = Math.min(chunkSize, sortedList.size());
            for (int i = 0; i < end; i++) {
                try {
                    PluginUI pluginUI = sortedList.get(i);
                    Node card = buildMenuInfo(pluginUI);
                    card.setUserData(pluginUI.name());
                    flowPane.getChildren().add(card);
                }
                catch (Exception e) {
                    logger.error("Error building card during reorder", e);
                }
            }
            if (end < sortedList.size()) {
                addCardsChunk(flowPane, sortedList, end, chunkSize, whenDone);
            }
            else {
                whenDone.run();
            }
        });
    }

    private void addCardsChunk(FlowPane flowPane, List<PluginUI> sortedList, int startIndex, int chunkSize, Runnable whenDone) {
        final int end = Math.min(startIndex + chunkSize, sortedList.size());
        Platform.runLater(() -> {
            for (int i = startIndex; i < end; i++) {
                try {
                    PluginUI pluginUI = sortedList.get(i);
                    Node card = buildMenuInfo(pluginUI);
                    card.setUserData(pluginUI.name());
                    flowPane.getChildren().add(card);
                }
                catch (Exception e) {
                    logger.error("Error building card during reorder", e);
                }
            }
            if (end < sortedList.size()) {
                addCardsChunk(flowPane, sortedList, end, chunkSize, whenDone);
            }
            else {
                whenDone.run();
            }
        });
    }

    private void reorderPluginCards(FlowPane flowPane, List<PluginUI> targetList, Map<String, Long> clickCounts) {
        reorderPluginCardsChunked(flowPane, targetList, clickCounts, REORDER_CHUNK_SIZE,
            () -> logger.info("实时重排插件卡片完成"));
    }

    private ToggleButton createToggle(String icon, String text) {
        return getSidebarManager().createToggle(icon, text);
    }

    private ToggleButton createToggle(String icon, StringBinding textBinding) {
        return getSidebarManager().createToggle(icon, textBinding);
    }

    @SuppressWarnings("all")
    private ToggleButton createToggle(String icon, String text, double rotate) {
        return getSidebarManager().createToggle(icon, text, rotate);
    }

    private ToggleButton createToggle(String icon, StringBinding textBinding, double rotate) {
        return getSidebarManager().createToggle(icon, textBinding, rotate);
    }

    /**
     * 创建带右键菜单的Toggle按钮（用于插件分类）
     */
    private ToggleButton createToggleWithContextMenu(String icon, String categoryName) {
        return getSidebarManager().createToggleWithContextMenu(icon, categoryName);
    }

    private SidebarManager getSidebarManager() {
        if (sidebarManager == null) {
            sidebarManager = new SidebarManager(toggleGroup);
        }
        return sidebarManager;
    }

    private void setComponentsI18n() {
        if (sidebarHeaderLabel != null) {
            sidebarHeaderLabel.textProperty().bind(I18N.getBinding("opencgl.main.sidebar.title"));
        }
        componentsLabel.textProperty().bind(I18N.getBinding("opencgl.main.components"));
    }

    /**
     * 加载插件图标，如果插件未提供或加载失败则使用默认图标。
     * 不从 jar: URL 直接构造 Image，避免 JavaFX 异步解析 jar 时触发 ZipException，改为先读字节再加载。
     */
    private Image loadPluginIcon(PluginUI pluginUI) {
        final int WIDTH = 70;
        final int HEIGHT = 50;

        if (pluginUI.iconPath() != null) {
            try (InputStream in = pluginUI.iconPath().openStream()) {
                byte[] bytes = in.readAllBytes();
                if (bytes.length > 0) {
                    return new Image(new ByteArrayInputStream(bytes), WIDTH, HEIGHT, true, true);
                }
            }
            catch (Exception e) {
                logger.warn("Failed to load plugin icon for {}, using default: {}", pluginUI.name(), e.getMessage());
            }
        }
        URL defaultIcon = this.getClass().getResource("/com/opencgl/icon/logo.png");
        if (defaultIcon != null) {
            try (InputStream in = defaultIcon.openStream()) {
                byte[] bytes = in.readAllBytes();
                if (bytes.length > 0) {
                    return new Image(new ByteArrayInputStream(bytes), WIDTH, HEIGHT, true, true);
                }
            }
            catch (Exception ignored) {
            }
            return new Image(defaultIcon.toString(), WIDTH, HEIGHT, true, true, true);
        }
        return new WritableImage(WIDTH, HEIGHT);
    }

    private VBox buildMenuInfo(PluginUI pluginUI) {
        Image image = loadPluginIcon(pluginUI);

        ImageView headImageView = new ImageView(image);

        StackPane iconPane = new StackPane(headImageView);
        Label updateBadge = new Label(" UP ");
        updateBadge.setStyle(
            "-fx-background-color: #ff4d4f; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 1 4 1 4; -fx-background-radius: 6;");
        updateBadge.setVisible(false);
        updateBadge.setManaged(false); // 防止撑开布局，形成悬浮效果
        StackPane.setAlignment(updateBadge, javafx.geometry.Pos.TOP_RIGHT);
        updateBadge.setTranslateX(15);
        updateBadge.setTranslateY(-10);
        iconPane.getChildren().add(updateBadge);

        Label header = new Label();
        header.setGraphic(iconPane);

        Label body = new Label();
        body.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
            pluginUI::name, I18N.localeProperty()));
        OpenCGLVbox vBox = new OpenCGLVbox();
        vBox.getProperties().put("updateBadge", updateBadge);
        vBox.getProperties().put("pluginClass", pluginUI.getClass().getSimpleName());
        // 使用 CSS 类代替硬编码样式
        vBox.getStyleClass().add("plugin-card-box");
        vBox.getChildren().addAll(header, body);

        // 创建右键菜单 (Phase 1 & Phase 2)
        MFXContextMenuItem openItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.get("opencgl.plugin.context.open"))
            .setOnAction(e -> handlePluginClick(pluginUI, vBox))
            .get();

        MFXContextMenuItem uninstallItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.get("opencgl.plugin.context.uninstall"))
            .setOnAction(e -> {
                if (DialogUtil.showConfirm(I18N.get("opencgl.plugin.confirm.uninstall"), I18N.get("opencgl.plugin.confirm.uninstall.message"))) {
                    pluginService.uninstallPlugin(pluginUI, rootPane, this::reloadPlugins);
                }
            })
            .get();

        MFXContextMenuItem disableItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.get("opencgl.plugin.context.disable"))
            .setOnAction(e -> {
                String pluginId = PluginIdentity.resolve(pluginUI);
                if (DialogUtil.showConfirm(I18N.get("opencgl.plugin.confirm.disable"),
                    I18N.get("opencgl.plugin.confirm.disable.message", pluginUI.name()))) {
                    pluginStateService.disable(pluginId);
                    reloadPlugins();
                }
            })
            .get();

        MFXContextMenuItem updateItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.get("opencgl.plugin.context.check_update"))
            .setOnAction(e -> {
                loadingMask.show(rootPane);
                CompletableFuture.supplyAsync(() -> pluginUpdateService.checkForUpdates(List.of(pluginUI)))
                    .whenComplete((updates, ex) -> Platform.runLater(() -> {
                        loadingMask.hide();
                        if (ex != null) {
                            DialogUtil.showErrorInfo(I18N.get("opencgl.plugin.error.check_update"));
                        }
                        else if (updates.isEmpty()) {
                            DialogUtil.showCustomTextInfo(I18N.get("opencgl.plugin.check_update.title"),
                                I18N.get("opencgl.plugin.check_update.latest", pluginUI.version()), null);
                        }
                        else {
                            com.opencgl.model.PluginUpdateInfo info = updates.getFirst();
                            if (DialogUtil.showConfirm(I18N.get("opencgl.plugin.confirm.upgrade"),
                                I18N.get("opencgl.plugin.confirm.upgrade.message", pluginUI.name(), info.getRemoteInfo().getVersion()))) {
                                handlePluginUpdate(info.getRemoteInfo());
                            }
                        }
                    }));
            })
            .get();

        MFXContextMenu contextMenu = MFXContextMenu.Builder.build(vBox)
            .addItem(openItem)
            .addItem(disableItem)
            .addItem(updateItem)
            .setShowCondition(mouseEvent -> mouseEvent.getButton() == MouseButton.SECONDARY)
            .installAndGet();

        String pluginId = PluginIdentity.resolve(pluginUI);
        PluginParserHelper.PluginLoadInfo loadInfo = PluginParserHelper.getLastLoadedPlugins().get(pluginId);
        if (loadInfo != null && loadInfo.uninstallable()) {
            contextMenu.getItems().add(2, uninstallItem);
        }

        vBox.getProperties().put("updateItem", updateItem);

        // 注入主题支持以防止右键菜单在深色模式下透明/发白
        contextMenu.setOnShown(event -> {
            if (contextMenu.getScene() != null) {
                ThemeManager.getInstance().getCurrentThemeStylesheets().forEach(sheet -> {
                    if (!contextMenu.getScene().getStylesheets().contains(sheet)) {
                        contextMenu.getScene().getStylesheets().add(sheet);
                    }
                });
            }
        });

        Tooltip pluginTip = new Tooltip(pluginUI.name());
        pluginTip.setShowDelay(Duration.seconds(2.0));
        Tooltip.install(vBox, pluginTip);

        // 创建TranslateTransition对象
        TranslateTransition transition = new TranslateTransition(Duration.seconds(0.3), vBox);
        // 设置Y轴方向的起始位置
        transition.setFromY(0);
        // 设置Y轴方向的结束位置
        transition.setToY(-10);
        // 设置动画循环模式
        transition.setAutoReverse(true);
        transition.setCycleCount(TranslateTransition.INDEFINITE);

        // 添加鼠标进入事件处理
        vBox.setOnMouseEntered(event -> {
            transition.playFromStart(); // 开始动画
        });

        // 添加鼠标移出事件处理
        vBox.setOnMouseExited(event -> {
            transition.stop(); // 停止动画
            vBox.setTranslateY(0); // 恢复原始位置
        });

        vBox.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                handlePluginClick(pluginUI, vBox);
            }
            else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                // Secondary click is automatically handled by MFXContextMenu attached to the
                // node
            }
        });
        return vBox;
    }

    private void handlePluginClick(PluginUI pluginUI, OpenCGLVbox vBox) {
        String pluginId = PluginIdentity.resolve(pluginUI);
        // 插件打开成功后记录点击 (使用稳定标识类名)
        pluginClickStore.increment(pluginId);

        Long i = tabValidate(pluginId, componentJfxTabPane.getTabs());
        if (i != 0) {
            // 切换到已打开的 Tab：可立即异步重排卡片
            componentJfxTabPane.getSelectionModel().select(Math.toIntExact(i - 1));
            ObservableList<Node> nodes = navBar.getChildren();
            recordLastSelectedToggle(nodes);
            removeSelectedToggleButton(nodes);
            contentPane.getChildren().setAll(componentJfxTabPane);
            CompletableFuture.runAsync(() -> {
                Map<String, Long> newCounts = pluginClickStore.loadAll();
                for (FlowPane fp : pluginFlowPanes) {
                    @SuppressWarnings("unchecked")
                    List<PluginUI> targetList = (List<PluginUI>) fp.getUserData();
                    if (targetList != null) {
                        reorderPluginCards(fp, targetList, newCounts);
                    }
                }
            });
            this.clearPluginMarketSearchField(vBox);
            return;
        }

        // 打开新 Tab：不在点击时触发重排，避免与 createView/setContent 争抢 FX 线程导致卡顿；等插件显示后再延迟重排
        // 点击时立即显示 LoadingMask（当前已在 FX 线程，用 showDirect 避免延迟一帧）
        loadingMask.showDirect(rootPane);

        long loadStartedNanos = System.nanoTime();
        AsyncUiPipeline.prepareThenRender(() -> {
            PluginUI pluginInstance = PluginParserHelper.createPluginInstance(pluginId);
            if (pluginInstance == null) {
                throw new IllegalStateException(I18N.get("opencgl.plugin.error.create_instance", pluginUI.name()));
            }
            return pluginInstance;
        }, pluginInstance -> {
            long renderStartedNanos = System.nanoTime();
            Tab tab = new Tab();
            tab.getProperties().put("pluginId", pluginId);
            tab.getProperties().put("pluginInstance", pluginInstance);
            tab.setClosable(true);
            tab.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                pluginInstance::name, I18N.localeProperty()));

            try {
                ObservableList<Node> nodes = navBar.getChildren();
                recordLastSelectedToggle(nodes);
                removeSelectedToggleButton(nodes);
                Node pluginUIView = PluginViewAdapter.adaptToFx(pluginInstance);

                if (pluginInstance instanceof ThemeAware themeAware) {
                    ThemeManager.getInstance().registerThemeAware(themeAware);
                    themeAware.onThemeChanged(ThemeManager.getInstance().getCurrentTheme().toThemeInfo());
                }

                tab.setContent(pluginUIView);
                componentJfxTabPane.getTabs().add(tab);
                componentJfxTabPane.getSelectionModel().select(tab);
                contentPane.getChildren().setAll(componentJfxTabPane);
                setupTabCloseGestures(tab);
                scheduleDeferredReorder();
                clearPluginMarketSearchField(vBox);
                logger.info("插件界面加载完成：{}，UI 构建 {} ms，总耗时 {} ms", pluginInstance.name(),
                    elapsedMillis(renderStartedNanos), elapsedMillis(loadStartedNanos));
            }
            catch (RuntimeException e) {
                PluginLifecycleManager.getInstance().disposeInstance(pluginInstance);
                throw e;
            }
        }, CompletableFuture.delayedExecutor(0, TimeUnit.MILLISECONDS),
            command -> CompletableFuture.delayedExecutor(80, TimeUnit.MILLISECONDS)
                .execute(() -> Platform.runLater(command)))
            .whenCompleteAsync((ignored, failure) -> {
                loadingMask.hideDirect();
                if (failure != null) {
                    logger.error("插件加载失败：{}", pluginUI.name(), failure);
                    String detail = failure.getCause() != null ? failure.getCause().getMessage() : failure.getMessage();
                    DialogUtil.showErrorInfo(I18N.getOrDefault("opencgl.mainWindows.loadPlugin.error")
                        + (detail == null ? I18N.get("opencgl.plugin.load.async_failed") : detail));
                }
            }, Platform::runLater);
    }

    private void clearPluginMarketSearchField(OpenCGLVbox card) {
        if (card == null) {
            return;
        }
        for (Node n = card.getParent(); n != null; n = n.getParent()) {
            if (n instanceof VBox vbox) {
                var children = vbox.getChildren();
                if (!children.isEmpty() && children.get(0) instanceof HBox searchBar) {
                    var barChildren = searchBar.getChildren();
                    if (!barChildren.isEmpty() && barChildren.get(0) instanceof MFXTextField tf) {
                        tf.clear();
                        return;
                    }
                }
            }
        }
    }

    private Long tabValidate(String pluginId, ObservableList<Tab> list) {
        Long i = 0L;
        for (Tab tab : list) {
            i++;
            if (pluginId.equals(tab.getProperties().get("pluginId"))) {
                return i;
            }
        }
        return 0L;
    }

    private void removeSelectedToggleButton(ObservableList<Node> nodes) {
        getSidebarManager().removeSelectedToggleButton(nodes);
    }

    private void recordLastSelectedToggle(ObservableList<Node> nodes) {
        for (Node node : nodes) {
            if (node instanceof ToggleButton toggleButton && toggleButton.isSelected()) {
                lastSelectedCategoryToggle = toggleButton;
                break;
            }
        }
    }

    private void buildAndInitContextMenu() {
        ContextMenuHandler contextMenuHandler = new ContextMenuHandler();
        contextMenuHandler.buildAndInstall(
            dropDownIcon,
            this::switchToTabPage,
            this::reloadPlugins,
            this::openHistoryTab);
    }

    private void reloadPlugins() {
        // Existing plugin views must not outlive the ClassLoader that created them.
        componentJfxTabPane.getTabs().clear();
        PluginParserHelper.closeAllClassLoaders();
        initializeLoader();
    }

    private void switchToTabPage() {
        if (!componentJfxTabPane.getTabs().isEmpty()) {
            contentPane.getChildren().setAll(componentJfxTabPane);
            ObservableList<Node> nodes = navBar.getChildren();
            recordLastSelectedToggle(nodes);
            removeSelectedToggleButton(nodes);
        }
        else {
            TooltipUtil.showToast(rootPane, I18N.getOrDefault("opencgl.main.tabIsNotExist"), 50.0);
        }
    }

    private void openHistoryTab() {
        // Check if tab already exists
        for (Tab tab : componentJfxTabPane.getTabs()) {
            if (HistoryManager.isHistoryTab(tab)) {
                componentJfxTabPane.getSelectionModel().select(tab);
                contentPane.getChildren().setAll(componentJfxTabPane);
                recordLastSelectedToggle(navBar.getChildren());
                removeSelectedToggleButton(navBar.getChildren());
                return;
            }
        }

        // Create new history tab using HistoryManager
        Tab tab = HistoryManager.createHistoryTab(rootPane);
        componentJfxTabPane.getTabs().add(tab);
        componentJfxTabPane.getSelectionModel().select(tab);
        contentPane.getChildren().setAll(componentJfxTabPane);
        removeSelectedToggleButton(navBar.getChildren());
    }

    private void openCustomPluginInTab(CustomPluginEntry entry) {
        String id = entry.getId();
        for (Tab tab : componentJfxTabPane.getTabs()) {
            if (id.equals(tab.getProperties().get("customPluginId"))) {
                componentJfxTabPane.getSelectionModel().select(tab);
                contentPane.getChildren().setAll(componentJfxTabPane);
                removeSelectedToggleButton(navBar.getChildren());
                return;
            }
        }
        Tab tab = new Tab(entry.getName());
        tab.setClosable(true);
        tab.getProperties().put("customPluginId", id);
        String loadUrl = entry.getUrlOrPath();
        if (loadUrl != null && !loadUrl.isBlank() && !loadUrl.startsWith("http://") && !loadUrl.startsWith("https://") && !loadUrl.startsWith("file:/")) {
            File f = new File(loadUrl);
            loadUrl = f.exists() ? f.toURI().toString() : loadUrl;
        }
        WebView webView = new WebView();
        javafx.scene.web.WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        try {
            URL fontCss = getClass().getResource("/com/opencgl/css/opencgl_webview_fonts.css");
            if (fontCss != null && (fontCss.toExternalForm().startsWith("file:") || fontCss.toExternalForm().startsWith("jar:"))) {
                engine.setUserStyleSheetLocation(fontCss.toExternalForm());
            }
        }
        catch (Exception e) {
            logger.debug("WebView user stylesheet not set: {}", e.getMessage());
        }
        engine.getLoadWorker().stateProperty().addListener((o, oldState, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                injectWebViewFontFamily(engine);
            }
        });
        final String contentType = "text/html; charset=UTF-8";
        if (loadUrl == null || loadUrl.isBlank()) {
            engine.loadContent("<html><body><p>URL/路径为空</p></body></html>", contentType);
        }
        else if (loadUrl.startsWith("file:")) {
            engine.load(loadUrl);
        }
        else {
            engine.load(loadUrl);
        }
        tab.setContent(webView);
        tab.setOnClosed(event -> {
            Node content = tab.getContent();
            if (content instanceof WebView wv) {
                wv.getEngine().load(null);
                tab.setContent(null);
            }
            if (componentJfxTabPane.getTabs().isEmpty()) {
                Platform.runLater(() -> {
                    if (lastSelectedCategoryToggle != null) {
                        lastSelectedCategoryToggle.setSelected(true);
                        if (lastSelectedCategoryToggle.getOnAction() != null) {
                            lastSelectedCategoryToggle.getOnAction().handle(new ActionEvent());
                        }
                    }
                    else {
                        ToggleButton homeToggle = (ToggleButton) navBar.getChildren().getFirst();
                        homeToggle.setSelected(true);
                        if (homeToggle.getOnAction() != null) {
                            homeToggle.getOnAction().handle(new ActionEvent());
                        }
                    }
                });
            }
        });
        componentJfxTabPane.getTabs().add(tab);
        componentJfxTabPane.getSelectionModel().select(tab);
        contentPane.getChildren().setAll(componentJfxTabPane);
        setupTabCloseGestures(tab);
        recordLastSelectedToggle(navBar.getChildren());
        removeSelectedToggleButton(navBar.getChildren());
    }

    /**
     * 向 WebView 页面注入中文字体样式，避免字体缺失导致方框/乱码
     */
    private void injectWebViewFontFamily(javafx.scene.web.WebEngine engine) {
        try {
            String fontCss = "html, body { font-family: 'Microsoft YaHei', 'PingFang SC', 'Hiragino Sans GB', 'Noto Sans CJK SC', 'WenQuanYi Micro Hei', sans-serif; }";
            String script = "(function(){ var s=document.getElementById('opencgl-font-inject'); if(s) return; s=document.createElement('style'); s.id='opencgl-font-inject'; s.textContent=\"" + fontCss.replace("\"", "\\\"") + "\"; document.head.appendChild(s); })();";
            engine.executeScript(script);
        }
        catch (Exception ignored) {
        }
    }

    private boolean isCustomPluginTabOpen(String id) {
        for (Tab tab : componentJfxTabPane.getTabs()) {
            if (id.equals(tab.getProperties().get("customPluginId"))) return true;
        }
        return false;
    }

    private void closeCustomPluginTab(String id) {
        for (Tab tab : componentJfxTabPane.getTabs()) {
            if (id.equals(tab.getProperties().get("customPluginId"))) {
                Node content = tab.getContent();
                if (content instanceof WebView) {
                    ((WebView) content).getEngine().load(null);
                }
                tab.setContent(null);
                componentJfxTabPane.getTabs().remove(tab);
                break;
            }
        }
    }

    private void setupTabCloseGestures(Tab tab) {
        ContextMenu tabMenu = new ContextMenu();

        MenuItem closeCurrent = new MenuItem();
        closeCurrent.textProperty().bind(I18N.getBinding("opencgl.tab.menu.close_current"));
        closeCurrent.setOnAction(ev -> componentJfxTabPane.getTabs().remove(tab));

        MenuItem closeLeft = new MenuItem();
        closeLeft.textProperty().bind(I18N.getBinding("opencgl.tab.menu.close_left"));
        closeLeft.setOnAction(ev -> {
            int idx = componentJfxTabPane.getTabs().indexOf(tab);
            if (idx > 0) {
                componentJfxTabPane.getTabs().subList(0, idx).clear();
            }
        });

        MenuItem closeRight = new MenuItem();
        closeRight.textProperty().bind(I18N.getBinding("opencgl.tab.menu.close_right"));
        closeRight.setOnAction(ev -> {
            int idx = componentJfxTabPane.getTabs().indexOf(tab);
            if (idx >= 0 && idx < componentJfxTabPane.getTabs().size() - 1) {
                componentJfxTabPane.getTabs().subList(idx + 1, componentJfxTabPane.getTabs().size()).clear();
            }
        });

        MenuItem closeAll = new MenuItem();
        closeAll.textProperty().bind(I18N.getBinding("opencgl.tab.menu.close_all"));
        closeAll.setOnAction(ev -> componentJfxTabPane.getTabs().clear());

        tabMenu.getItems().addAll(closeCurrent, closeLeft, closeRight, closeAll);
        tab.setContextMenu(tabMenu);

        String tabUuidClass = "opencgl-tab-uuid-" + java.util.UUID.randomUUID();

        tab.getStyleClass().add(tabUuidClass);
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                Platform.runLater(() -> {
                    for (Node node : componentJfxTabPane.lookupAll(".tab")) {
                        if (node.getStyleClass().contains(tabUuidClass)) {
                            if (node.getOnMouseClicked() == null) {
                                node.setOnMouseClicked(me -> {
                                    if (me.getButton() == javafx.scene.input.MouseButton.PRIMARY && me.getClickCount() == 2) {
                                        componentJfxTabPane.getTabs().remove(tab);
                                    }
                                });
                            }
                            break;
                        }
                    }
                });
            }
        });
    }

    private void populateThemeMenu(MFXContextMenu themeMenu) {
        themeMenu.getItems().clear();
        ThemeMode selectedMode = ThemeManager.getInstance().getCurrentPreference().mode();
        AccentColor selectedAccent = ThemeManager.getInstance().getCurrentPreference().accent();
        for (ThemeMode mode : new ThemeMode[]{ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK}) {
            MFXContextMenuItem item = new MFXContextMenuItem(ThemeMenuText.mode(mode, selectedMode));
            item.setOnAction(e -> ThemeManager.getInstance().switchTheme(mode,
                ThemeManager.getInstance().getCurrentPreference().accent()));
            themeMenu.getItems().add(item);
        }
        for (AccentColor accent : new AccentColor[]{AccentColor.TEAL, AccentColor.BLUE,
            AccentColor.PURPLE, AccentColor.OCEAN}) {
            MFXContextMenuItem item = new MFXContextMenuItem(ThemeMenuText.accent(accent, selectedAccent));
            item.setOnAction(e -> ThemeManager.getInstance().switchTheme(
                ThemeManager.getInstance().getCurrentPreference().mode(), accent));
            themeMenu.getItems().add(item);
        }
    }

    private void handlePluginUpdate(com.opencgl.model.RemotePluginInfo info) {
        loadingMask.show(rootPane);
        CompletableFuture.runAsync(() -> {
            try {
                String tmpDir = System.getProperty("java.io.tmpdir");
                String fileName = info.getName() + "-" + info.getVersion() + ".jar";
                java.io.File targetFile = new java.io.File(tmpDir, fileName);

                String downloadUrl = info.getDownloadUrl();
                if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
                    cn.hutool.http.HttpUtil.downloadFile(downloadUrl, targetFile);
                }
                else {
                    String cleanPath = downloadUrl.toLowerCase().startsWith("file:///") ? downloadUrl.substring(7)
                        : (downloadUrl.toLowerCase().startsWith("file:/") ? downloadUrl.substring(5) : downloadUrl);
                    java.io.File srcFile = new java.io.File(cleanPath);
                    if (!srcFile.exists())
                        throw new RuntimeException("本地更新资源不存在: " + srcFile.getAbsolutePath());
                    cn.hutool.core.io.FileUtil.copy(srcFile, targetFile, true);
                }

                Platform.runLater(() -> {
                    loadingMask.hide();
                    pluginService.installPlugin(targetFile, rootPane, () -> {
                        DialogUtil.showCustomTextInfo(I18N.get("opencgl.plugin.update.success"), I18N.get("opencgl.plugin.update.success.detail", info.getName()),
                            I18N.get("opencgl.plugin.update.success.hint", info.getVersion()));
                        try {
                            initializeLoader(); // 热拉起新的 UI 卡片排列
                        }
                        catch (Exception ignored) {
                        }
                    });
                });
            }
            catch (Exception ex) {
                Platform.runLater(() -> {
                    loadingMask.hide();
                    DialogUtil.showErrorInfo(I18N.get("opencgl.plugin.error.auto_update"));
                });
            }
        });
    }
}
