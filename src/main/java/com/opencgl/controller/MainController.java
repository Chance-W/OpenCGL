package com.opencgl.controller;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.i18n.I18N;
import com.opencgl.listener.Config;
import com.opencgl.listener.FlexibleListener;
import com.opencgl.model.MenuInfo;
import com.opencgl.model.OpenCGLSelfProperties;
import com.opencgl.selfpane.CglTabPane;
import com.opencgl.selfpane.OpenCGLVbox;
import com.opencgl.selfpane.SettingPane;
import com.opencgl.util.DialogUtil;
import com.opencgl.util.LoadingUtil;
import com.opencgl.util.PluginClassLoader;
import com.opencgl.util.PluginParserHelper;
import com.opencgl.util.RandomNumberGeneratorUtil;
import com.opencgl.util.TooltipUtil;
import io.github.palexdev.materialfx.controls.MFXContextMenu;
import io.github.palexdev.materialfx.controls.MFXContextMenu.Builder;
import io.github.palexdev.materialfx.controls.MFXContextMenuItem;
import io.github.palexdev.materialfx.controls.MFXIconWrapper;
import io.github.palexdev.materialfx.controls.MFXRectangleToggleNode;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.utils.ScrollUtils;
import io.github.palexdev.materialfx.utils.ToggleButtonsUtil;
import io.github.palexdev.materialfx.utils.others.loader.MFXLoader;
import io.github.palexdev.materialfx.utils.others.loader.MFXLoaderBean;
import io.github.palexdev.mfxresources.fonts.IconDescriptor;
import io.github.palexdev.mfxresources.fonts.MFXFontIcon;
import io.github.palexdev.mfxresources.fonts.fontawesome.FontAwesomeSolid;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
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
 * @since v9.0
 */
@SuppressWarnings("unused")
public class MainController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private static int tag = 1;

    private final Stage stage;

    @FXML
    private HBox autoGroupHBox;

    private double xOffset;
    private double yOffset;
    private final ToggleGroup toggleGroup;

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
    private MFXFontIcon settingIcon;

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

    private BorderPane homeRootPane = null;

    private boolean contextMenuOpen = false;

    private final List<IconDescriptor> icons = new ArrayList<>();

    private final SettingPane settingPane = new SettingPane();

    private final Tooltip maxTip = new Tooltip(I18N.getOrDefault("oepncgl.main.max.text"));


    public MainController(Stage stage) {
        this.stage = stage;
        this.toggleGroup = new ToggleGroup();
        ToggleButtonsUtil.addAlwaysOneSelectedSupport(toggleGroup);
        Collections.addAll(icons, FontAwesomeSolid.values());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setComponentsI18n();
        new FlexibleListener(stage).enableDrag(rootPane);
        buildAndInitContextMenu();
        Tooltip tooltip = new Tooltip(I18N.getOrDefault("oepncgl.main.hang.click.info"));
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

        Tooltip switchToTabTooltip = new Tooltip(I18N.getOrDefault("opencgl.main.switchToTabListPage"));
        switchToTabTooltip.setShowDelay(Duration.seconds(1));
        Tooltip.install(switchToTabPageIcon, switchToTabTooltip);
        switchToTabPageIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> switchToTabPage());

        closeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            System.exit(0);
            Platform.exit();
        });

        Tooltip closeTip = new Tooltip(I18N.getOrDefault("oepncgl.main.close.text"));
        closeTip.setShowDelay(Duration.ZERO);
        Tooltip.install(closeIcon, closeTip);

        minimizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> ((Stage) rootPane.getScene().getWindow()).setIconified(true));
        Tooltip minTip = new Tooltip(I18N.getOrDefault("oepncgl.main.min.text"));
        minTip.setShowDelay(Duration.ZERO);
        Tooltip.install(minimizeIcon, minTip);

        Tooltip dropDownTip = new Tooltip(I18N.getOrDefault("oepncgl.main.dropDown.text"));
        dropDownTip.setShowDelay(Duration.seconds(3.0));
        Tooltip.install(dropDownIcon, dropDownTip);

        Tooltip questionTip = new Tooltip(I18N.getOrDefault("opencgl.main.help.text"));
        questionTip.setShowDelay(Duration.seconds(1.0));
        Tooltip.install(questionIcon, questionTip);
        questionIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> jumpToHelpPage());

        Tooltip upgradeCheckTip = new Tooltip(I18N.getOrDefault("opencgl.main.upgrade.check.text"));
        questionTip.setShowDelay(Duration.seconds(1.0));
        Tooltip.install(upgradeCheckIcon, upgradeCheckTip);
        upgradeCheckIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            try {
                Platform.runLater(() -> LoadingUtil.show(contentPane));
                upgradeCheck(true);
            }
            finally {
                Platform.runLater(() -> LoadingUtil.remove(contentPane));
            }
        });



        settingIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> settingPane.show(rootPane));
        Tooltip settingTip = new Tooltip(I18N.getOrDefault("oepncgl.main.setting.text"));
        settingTip.setShowDelay(Duration.ZERO);
        Tooltip.install(settingIcon, settingTip);

        Tooltip.install(maximizeIcon, maxTip);
        maxTip.setShowDelay(Duration.ZERO);
        maximizeIcon.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> autoFillWindows());

        Tooltip movePosition = new Tooltip(I18N.getOrDefault("oepncgl.main.movePosition.text"));
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
                        Platform.runLater(MainController.this::autoFillWindows);
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


        try {
            initializeLoader();
        }
        catch (IOException e) {
            logger.error("", e);
            DialogUtil.showErrorInfo(e.getMessage());
        }
        catch (Exception e) {
            logger.error("", e);
        }
        ScrollUtils.addSmoothScrolling(scrollPane);
        // The only way to get a fucking smooth image in this shitty framework
        Image image = new Image(String.valueOf(this.getClass().getClassLoader().getResource("com/opencgl/icon/logo_alt.png")), 64, 64, true, true);
        ImageView logo = new ImageView(image);
        Circle clip = new Circle(30);
        clip.centerXProperty().bind(logo.layoutBoundsProperty().map(Bounds::getCenterX));
        clip.centerYProperty().bind(logo.layoutBoundsProperty().map(Bounds::getCenterY));
        logo.setClip(clip);
        logoContainer.getChildren().add(logo);
        Platform.runLater(() -> upgradeCheck(false));
    }

    private void upgradeCheck(boolean tag) {
        try {
            @SuppressWarnings("resource")
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(Config.readInternalConfigure(OpenCGLSelfProperties.DEFAULT_UPGRADE_URL)))
                .GET()
                .build();

            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                byte[] fileContent = response.body();
                Properties properties = new Properties();
                properties.load(new InputStreamReader(new ByteArrayInputStream(fileContent), StandardCharsets.UTF_8));
                // 通过Properties对象获取值
                String version = properties.getProperty("version");
                String description = properties.getProperty("description");
                String downloadAddress = properties.getProperty("downloadAddress");
                String currentVersion = Config.readInternalConfigure(OpenCGLSelfProperties.CURRENT_VERSION_KEY);
                if (currentVersion.compareTo(version) < 0) {
                    DialogUtil.showCustomTextInfo(I18N.getOrDefault("opencgl.main.info.upgradeInfo"), description, downloadAddress);
                }
                else {
                    if (tag) {
                        DialogUtil.showCustomTextInfo(I18N.getOrDefault("opencgl.main.info.noNeedUpgradeInfo"));
                    }
                }
            }
            else {
                logger.error("Failed to retrieve file list. Server returned status code: {}", response.statusCode());
            }
        }
        catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

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
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        double screenWidth = primaryScreenBounds.getWidth();
        double screenHeight = primaryScreenBounds.getHeight();
        if ((stage.getX() == 0.0 || stage.getY() == 0.0)
            && stage.getHeight() == screenHeight
            && stage.getWidth() == screenWidth) {
            maxTip.setText(I18N.getOrDefault("oepncgl.main.max.maximize"));
            stage.setY(yOffset);
            stage.setX(xOffset);
            stage.setWidth(rootPane.getPrefWidth());
            stage.setHeight(rootPane.getPrefHeight());
        }
        else {
            xOffset = stage.getX();
            yOffset = stage.getY();

            stage.setX(0);
            stage.setY(0);
            stage.setWidth(screenWidth);
            stage.setHeight(screenHeight);
            maxTip.setText(I18N.getOrDefault("oepncgl.main.max.reduction"));
        }
    }

    private void initializeLoader() throws Exception {
        List<MenuInfo> menuInfos = PluginParserHelper.analysisComponentJson();
        MFXLoader loader = new MFXLoader();
        loader.addView(MFXLoaderBean.of("HomePane", this.getClass().getClassLoader().getResource("com/opencgl/view/HomePane.fxml")).setBeanToNodeMapper(() -> createToggle("fas-circle-dot", "主页")).setDefaultRoot(true).get());
        loader.addView(MFXLoaderBean.of("allPane", this.getClass().getClassLoader().getResource("com/opencgl/view/GeneralComponentsPane.fxml")).setBeanToNodeMapper(() -> createToggle("fas-bars-progress", "全部")).get());

        Map<String, List<MenuInfo>> menuMap = menuInfos.stream().collect(Collectors.groupingBy(MenuInfo::getFatherMenuName));
        menuMap.forEach(new BiConsumer<>() {
            @Override
            public void accept(String s, List<MenuInfo> menuInfos) {
                int i = RandomNumberGeneratorUtil.generateRandomNumber(icons.size());
                loader.addView(MFXLoaderBean.of(s, this.getClass().getClassLoader().getResource("com/opencgl/view/GeneralComponentsPane.fxml")).setBeanToNodeMapper(() -> createToggle(icons.get(i).getDescription(), s)).get());
            }
        });

        loader.setOnLoadedAction(beans -> {
            List<ToggleButton> nodes = beans.stream()
                .map(bean -> {
                    if (bean.getViewName().equals("allPane")) {
                        MFXScrollPane scrollPane = (MFXScrollPane) bean.getRoot();
                        FlowPane flowPane = (FlowPane) scrollPane.getContent();
                        for (MenuInfo menuInfo : menuInfos) {
                            try {
                                flowPane.getChildren().add(buildMenuInfo(menuInfo));
                            }
                            catch (ClassNotFoundException | NoSuchMethodException e) {
                                logger.error("", e);
                                DialogUtil.showErrorInfo(e.getMessage());
                            }
                            catch (Exception e) {
                                logger.error("", e);
                            }
                        }
                    }
                    menuMap.forEach((s, menuInfos1) -> {
                        if (bean.getViewName().equals(s)) {
                            for (MenuInfo menuInfo : menuInfos1) {
                                MFXScrollPane scrollPane = (MFXScrollPane) bean.getRoot();
                                FlowPane flowPane = (FlowPane) scrollPane.getContent();
                                try {
                                    flowPane.getChildren().add(buildMenuInfo(menuInfo));
                                }
                                catch (Exception e) {
                                    logger.error("", e);
                                    DialogUtil.showErrorInfo(e.getMessage());
                                }
                            }
                        }
                    });
                    ToggleButton toggle = (ToggleButton) bean.getBeanToNodeMapper().get();
                    toggle.setOnAction(event -> contentPane.getChildren().setAll(bean.getRoot()));
                    if (bean.isDefaultView()) {
                        homeRootPane = (BorderPane) bean.getRoot();
                        contentPane.getChildren().setAll(homeRootPane);
                        toggle.setSelected(true);
                    }
                    return toggle;
                }).collect(Collectors.toList());
            navBar.getChildren().setAll(nodes);
        });
        loader.start();
    }

    private ToggleButton createToggle(String icon, String text) {
        return createToggle(icon, text, 0);
    }

    @SuppressWarnings("all")
    private ToggleButton createToggle(String icon, String text, double rotate) {
        MFXIconWrapper wrapper = new MFXIconWrapper(icon, 24, 32);
        MFXRectangleToggleNode toggleNode = new MFXRectangleToggleNode(text, wrapper);
        toggleNode.setAlignment(Pos.CENTER_LEFT);
        toggleNode.setMaxWidth(Double.MAX_VALUE);
        toggleNode.setToggleGroup(toggleGroup);
        if (rotate != 0) wrapper.getIcon().setRotate(rotate);
        return toggleNode;
    }

    private void setComponentsI18n() {
        componentsLabel.setText(I18N.getOrDefault("opencgl.mainWindows.components"));
    }

    private VBox buildMenuInfo(MenuInfo menuInfo)
        throws Exception {
        Image image = new Image(Objects.requireNonNull(
            Optional.ofNullable(PluginParserHelper.getFileInputStream(new File(Config.readExternalConfigure(OpenCGLSelfProperties.PLUGIN_PATH_KEY) + File.separator + menuInfo.getJarName()),
                menuInfo.getIconPath())).orElse(this.getClass().getResourceAsStream("/com/opencgl/icon/logo.png"))),
            70,
            50,
            true,
            true);

        ImageView headImageView = new ImageView(image);
        Label header = new Label();
        header.setGraphic(headImageView);
        Label body = new Label(menuInfo.getMenuName());
        OpenCGLVbox vBox = new OpenCGLVbox();
        vBox.setStyle("-fx-background-color: lightblue;-fx-border-width: 0px");
        vBox.getChildren().addAll(header, body);

        Tooltip pluginTip = new Tooltip(menuInfo.getPluginInfo());
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

        FXMLLoader fxmlLoader = new FXMLLoader();
        try (PluginClassLoader pluginClassLoader = PluginClassLoader
            .create(
                new File(Config.readExternalConfigure(OpenCGLSelfProperties.PLUGIN_PATH_KEY) + menuInfo.getJarName()))) {
            FXMLLoader pluginFxmlLoader = (FXMLLoader) pluginClassLoader.loadClass("javafx.fxml.FXMLLoader").getDeclaredConstructor().newInstance();
            fxmlLoader.setLocation(pluginClassLoader.getResource(menuInfo.getFxmlPath()));
        }
        vBox.setOnMouseClicked(new EventHandler<>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                Long i = tabValidate(menuInfo.getMenuName(), componentJfxTabPane.getTabs());
                if (i != 0) {
                    componentJfxTabPane.getSelectionModel().select(Math.toIntExact(i - 1));
                    // 移除选中
                    ObservableList<Node> nodes = navBar.getChildren();
                    removeSelectedToggleButton(nodes);

                    contentPane.getChildren().setAll(componentJfxTabPane);
                    return;
                }
                CompletableFuture.runAsync(() -> {
                    Platform.runLater(() -> LoadingUtil.show(contentPane));
                    Tab tab = new Tab();
                    tab.setClosable(true);
                    tab.setText(menuInfo.getMenuName());
                    try {
                        if (StringUtils.isNotEmpty(menuInfo.getJarName())) {
                            PluginClassLoader pluginClassLoader = PluginClassLoader.create(new File(Config.readExternalConfigure(OpenCGLSelfProperties.PLUGIN_PATH_KEY) + File.separator + menuInfo.getJarName()));
                            FXMLLoader pluginFxmlLoader = (FXMLLoader) pluginClassLoader.loadClass("javafx.fxml.FXMLLoader").getDeclaredConstructor().newInstance();
                            pluginFxmlLoader.setClassLoader(pluginClassLoader);
                            URL resource = pluginClassLoader.getResource(menuInfo.getFxmlPath());
                            pluginFxmlLoader.setLocation(resource);
                            tab.setContent(pluginFxmlLoader.load());
                            ObservableList<Node> nodes = navBar.getChildren();
                            removeSelectedToggleButton(nodes);
                            Platform.runLater(() -> contentPane.getChildren().setAll(componentJfxTabPane));
                            tab.setOnClosed(event -> {
                                try {
                                    pluginClassLoader.close();
                                    if (componentJfxTabPane.getTabs().isEmpty()) {
                                        Platform.runLater(() -> {
                                            ToggleButton homePaneToggleButton = (ToggleButton) navBar.getChildren().get(0);
                                            contentPane.getChildren().setAll(homeRootPane);
                                            homePaneToggleButton.setSelected(true);
                                        });
                                    }
                                }
                                catch (IOException e) {
                                    logger.error("", e);
                                    Platform.runLater(() -> DialogUtil.showErrorInfo(e.getMessage()));
                                }
                            });
                        }
                        else {
                            tab.setContent(FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource(menuInfo.getFxmlPath()))));
                        }
                        Platform.runLater(() -> {
                            componentJfxTabPane.getTabs().add(tab);
                            componentJfxTabPane.getSelectionModel().select(tab);
                        });
                    }
                    catch (Exception e) {
                        logger.error("", e);
                        Platform.runLater(() -> DialogUtil.showErrorInfo(I18N.getOrDefault("opencgl.mainWindows.loadPlugin.error") + e.getMessage()));
                    }
                    finally {
                        Platform.runLater(() -> LoadingUtil.remove(contentPane));
                    }
                });
            }
        });
        return vBox;
    }

    private Long tabValidate(String value, ObservableList<Tab> list) {
        Long i = 0L;
        for (Tab tab : list) {
            i++;
            if (tab.getText().equals(value)) {
                return i;
            }
        }
        return 0L;
    }

    private void removeSelectedToggleButton(ObservableList<Node> nodes) {
        nodes.forEach(node -> {
            ToggleButton toggleButton = (ToggleButton) node;
            if (toggleButton.isSelected()) {
                toggleButton.setSelected(false);
            }
        });
    }

    private void buildAndInitContextMenu() {
        MFXContextMenuItem switchToTabPageMenuItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.getOrDefault("opencgl.main.switchToTabListPage"))
            .setIcon(new MFXFontIcon("fas-check-double", 16))
            .setOnAction(event -> switchToTabPage()).get();
        switchToTabPageMenuItem.setStyle("-fx-max-height: 20px;");

        MFXContextMenuItem reloadPluginMenuItem = MFXContextMenuItem.Builder.build()
            .setText(I18N.getOrDefault("opencgl.main.reloadPluginMenuItem"))
            .setIcon(new MFXFontIcon("fas-arrow-rotate-left", 16))
            .setOnAction(event -> {
                logger.info("begin reload plugin....");
                try {
                    initializeLoader();
                    TooltipUtil.showToast(I18N.getOrDefault("opencgl.mainWindows.loadPlugin.success"));
                }
                catch (Exception e) {
                    TooltipUtil.showToast(I18N.getOrDefault("opencgl.mainWindows.loadPlugin.error") + e.getMessage());
                }
            }).get();
        reloadPluginMenuItem.setStyle("-fx-max-height: 20px;");

        MFXContextMenu menu = Builder.build(dropDownIcon)
            .addItems(switchToTabPageMenuItem)
            .addSeparator(Builder.getLineSeparator())
            .addItem(reloadPluginMenuItem)
            .addSeparator(Builder.getLineSeparator())
            .setShowCondition(mouseEvent -> {
                contextMenuOpen = mouseEvent.getButton() == MouseButton.PRIMARY;
                return contextMenuOpen;
            })
            .installAndGet();
        menu.setOnHidden((event) -> contextMenuOpen = false);
    }

    private void switchToTabPage() {
        if (!componentJfxTabPane.getTabs().isEmpty()) {
            contentPane.getChildren().setAll(componentJfxTabPane);
            ObservableList<Node> nodes = navBar.getChildren();
            removeSelectedToggleButton(nodes);
        }
        else {
            TooltipUtil.showToast(rootPane, I18N.getOrDefault("opencgl.main.tabIsNotExist"), 30.0);
        }
    }
}