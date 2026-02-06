package com.opencgl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.time.Duration;

import com.opencgl.base.listener.Config;
import com.opencgl.base.model.Base;
import com.opencgl.base.model.OpenCGLSelfProperties;
import com.opencgl.util.PluginParserHelper;
import javafx.scene.image.Image;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.controller.NewMainController;
import com.opencgl.i18n.I18N;
import fr.brouillard.oss.cssfx.CSSFX;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.enums.ButtonType;
import com.opencgl.base.theme.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class RunApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(RunApplication.class);

    private static FileLock lock;

    @Override
    public void start(Stage primaryStage) {
        Image iconImage = new Image(String.valueOf(this.getClass().getClassLoader().getResource("com/opencgl/icon/logo_alt.png")), 64, 64, true, true);
        primaryStage.getIcons().add(iconImage);
        CSSFX.start();
        ThemeManager.getInstance().init();

        Boolean locked = lockingProcess(primaryStage);
        if (locked) {
            return;
        }

        // 应用保存的日志级别
        String logLevel = Config.readExternalConfigure(OpenCGLSelfProperties.LOG_LEVEL_KEY);
        if (logLevel != null && !logLevel.isEmpty()) {
            LogManager.getRootLogger().setLevel(Level.toLevel(logLevel));
        }

        // 创建并显示加载进度条
        MFXProgressBar progressBar = new MFXProgressBar();
        progressBar.setPrefSize(300, 25);
        Text loadingText = new Text(I18N.getOrDefault("opencgl.main.application.startupInfo"));
        loadingText.setStyle(" -fx-fill: -theme-text-primary;");

        // loadingText.getStyleClass().add("theme-text"); // 应用主题文本样式
        StackPane loadingScreen = new StackPane(progressBar, loadingText);
        //  loadingScreen.getStyleClass().add("root"); // 确保主题变量生效
        Scene loadingScene = new Scene(loadingScreen, Color.TRANSPARENT);
        ThemeManager.getInstance().registerScene(loadingScene);

        // Apply theme stylesheets to loading scene
//        ThemeManager.getInstance().getCurrentThemeStylesheets().forEach(stylesheet -> {
//            if (!loadingScene.getStylesheets().contains(stylesheet)) {
//                loadingScene.getStylesheets().add(stylesheet);
//            }
//        });

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(loadingScene);
        primaryStage.show();

        CompletableFuture.runAsync(() ->
        {
            try {
                logger.info("开始加载主界面FXML...");
                FXMLLoader loader = new FXMLLoader(RunApplication.class.getClassLoader().getResource("com/opencgl/view/Main.fxml"));
                loader.setControllerFactory(c -> new NewMainController(primaryStage));
                Parent root = loader.load();
                logger.info("FXML加载完成，准备显示主界面...");

                Platform.runLater(() -> {
                    try {
                        Scene scene = new Scene(root);
                        scene.setFill(Color.TRANSPARENT);

                        // 设置主 Scene 并加载保存的主题
                        ThemeManager.getInstance().registerScene(scene);

                        primaryStage.setTitle(I18N.get("opencgl.main.window.title"));
                        primaryStage.setResizable(true);
                        primaryStage.setScene(scene);

                        // 按当前屏幕可用区域 clamp 主窗口尺寸，解决低分辨率屏幕下窗口超出的问题
                        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
                        double maxW = visualBounds.getWidth() * 0.95;
                        double maxH = visualBounds.getHeight() * 0.95;
                        double stageW = Math.min(primaryStage.getWidth(), maxW);
                        double stageH = Math.min(primaryStage.getHeight(), maxH);
                        // 如果 pref 尺寸比屏幕大，强制缩小
                        if (primaryStage.getWidth() > maxW || primaryStage.getHeight() > maxH) {
                            primaryStage.setWidth(stageW);
                            primaryStage.setHeight(stageH);
                        }
                        // 居中到主屏
                        primaryStage.setX(visualBounds.getMinX() + (visualBounds.getWidth() - stageW) / 2);
                        primaryStage.setY(visualBounds.getMinY() + (visualBounds.getHeight() - stageH) / 2);

                        logger.info("主界面显示完成");
                    }
                    catch (Exception e) {
                        logger.error("显示主界面时发生错误", e);
                        showFatalErrorThenExit(primaryStage, "opencgl.startup.fatal.display", e.getMessage());
                    }
                });
            }
            catch (IOException e) {
                logger.error("Failed to load main FXML", e);
                Platform.runLater(() -> showFatalErrorThenExit(primaryStage, "opencgl.startup.fatal.fxml", e.getMessage()));
            }
        }).exceptionally(throwable -> {
            logger.error("Failed to initialize application", throwable);
            String msg = throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName();
            Platform.runLater(() -> showFatalErrorThenExit(primaryStage, "opencgl.startup.fatal.init", msg));
            return null;
        });
    }

    /**
     * 显示致命错误弹窗，用户点击确认后关闭窗口并退出进程。
     * 用于 FXML 加载失败、主界面显示异常、初始化异常等场景。
     */
    private static void showFatalErrorThenExit(Stage primaryStage, String messageKey, String detail) {
        primaryStage.close();
        Stage errorStage = new Stage();
        errorStage.setTitle(I18N.get("opencgl.startup.fatal.title"));
        Label messageLabel = new Label(I18N.get(messageKey, detail != null ? detail : ""));
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);
        MFXButton okButton = new MFXButton(I18N.get("opencgl.main.button.confirm"));
        okButton.setButtonType(ButtonType.RAISED);
        okButton.setOnAction(e -> {
            errorStage.close();
            System.exit(1);
        });
        VBox box = new VBox(15, messageLabel, okButton);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        Scene scene = new Scene(box, Color.TRANSPARENT);
        ThemeManager.getInstance().registerScene(scene);
        errorStage.setScene(scene);
        errorStage.initStyle(StageStyle.TRANSPARENT);
        errorStage.show();
        errorStage.setOnCloseRequest(e -> System.exit(1));
    }

    private static Boolean lockingProcess(Stage stage) {
        File file = new File(Base.OPEN_CGL_LOCK_FILE);
        if (!file.exists()) {
            try {
                logger.info("begin create lock file {}", Base.OPEN_CGL_LOCK_FILE);
                if (!file.getParentFile().exists()) {
                    boolean directoryCreateResult = file.getParentFile().mkdirs();
                    logger.info("end create lock directory {} and result is [{}]", Base.OPEN_CGL_LOCK_FILE, directoryCreateResult);
                }
                boolean fileCreateResult = file.createNewFile();
                logger.info("end create lock file {} and result is [{}]", Base.OPEN_CGL_LOCK_FILE, fileCreateResult);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            @SuppressWarnings("resource")
            RandomAccessFile lockFile = new RandomAccessFile(Base.OPEN_CGL_LOCK_FILE, "rw");
            FileChannel channel = lockFile.getChannel();
            lock = channel.tryLock();
            if (lock == null) {
                StackPane stackPane = new StackPane();
                stackPane.setStyle("-fx-border-color: gray;-fx-border-width: 1px;");
                MFXButton quitButton = new MFXButton(I18N.getOrDefault("opencgl.main.close.text"));
                quitButton.setButtonType(ButtonType.RAISED);
                quitButton.setAlignment(Pos.CENTER);
                quitButton.setMinHeight(40);
                quitButton.setMinWidth(100);
                quitButton.setOnAction(actionEvent -> System.exit(1));

                VBox vBox = new VBox();
                vBox.setFillWidth(true);
                VBox.setMargin(quitButton, new Insets(0, 0, 10, 0));
                vBox.setSpacing(10);
                vBox.setAlignment(Pos.CENTER);
                Label quitLabel = new Label(I18N.getOrDefault("opencgl.main.startRepeat"));
                quitLabel.setMinWidth(400);
                quitLabel.setMinHeight(300);
                quitLabel.setAlignment(Pos.CENTER);
                Font font = new Font(20);
                quitLabel.setFont(font);
                vBox.getChildren().addAll(quitLabel, quitButton);
                stackPane.getChildren().addAll(vBox);
                Scene scene = new Scene(stackPane, Color.TRANSPARENT);
                stage.initStyle(StageStyle.TRANSPARENT);
                stage.setScene(scene);
                stage.show();
                return true;
            }
            // 当程序关闭时释放文件锁和清理资源
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // 设置超时定时器，防止插件请求导致关闭卡死
                ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
                timeoutScheduler.schedule(() -> {
                    logger.warn("Shutdown timeout (5s), forcing exit to prevent hang");
                    Runtime.getRuntime().halt(0); // 强制退出，不执行剩余的shutdown hook
                }, 5, TimeUnit.SECONDS);

                try {
                    ThemeManager.getInstance().shutdown();

                    if (!Config.awaitPendingWrites(Duration.ofSeconds(2))) {
                        logger.warn("Configuration writes did not finish before shutdown cleanup");
                    }

                    // 清理所有插件 ClassLoader
                    PluginParserHelper.closeAllClassLoaders();
                    logger.info("Plugin ClassLoaders cleaned up");

                    // 释放文件锁
                    lock.release();
                    logger.info("File lock released");

                    // 取消超时定时器
                    timeoutScheduler.shutdownNow();
                }
                catch (IOException e) {
                    logger.error("Error during shutdown cleanup", e);
                }
            }));
        }
        catch (Exception e) {
            logger.error("", e);
            System.exit(1);
        }
        return false;
    }
}
