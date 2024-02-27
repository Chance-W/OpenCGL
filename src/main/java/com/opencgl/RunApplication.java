package com.opencgl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencgl.controller.MainController;
import com.opencgl.i18n.I18N;
import fr.brouillard.oss.cssfx.CSSFX;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.enums.ButtonType;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
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
        UserAgentBuilder.builder()
            .themes(JavaFXThemes.MODENA)
            .themes(MaterialFXStylesheets.forAssemble(true))
            .setDeploy(true)
            .setResolveAssets(true)
            .build()
            .setGlobal();

        Boolean locked = lockingProcess(primaryStage);
        if (locked) {
            return;
        }
        // 创建并显示加载进度条
        MFXProgressBar progressBar = new MFXProgressBar();
        progressBar.getStylesheets()
            .setAll(Objects.requireNonNull(this.getClass()
                    .getResource("/com/opencgl/css/ProgressIndicator.css"))
                .toExternalForm());
        progressBar.setPrefSize(300, 25);
        Text loadingText = new Text(I18N.getOrDefault("opencgl.main.application.startupInfo"));
        StackPane loadingScreen = new StackPane(progressBar, loadingText);
        Scene loadingScene = new Scene(loadingScreen, Color.TRANSPARENT);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(loadingScene);
        primaryStage.show();

        CompletableFuture.runAsync(() ->
        {
            try {
                FXMLLoader loader = new FXMLLoader(RunApplication.class.getClassLoader().getResource("com/opencgl/view/Main.fxml"));
                loader.setControllerFactory(c -> new MainController(primaryStage));
                Parent root = loader.load();
                Platform.runLater(() -> {
                    Scene scene = new Scene(root);
                    scene.setFill(Color.TRANSPARENT);
                    // MFXThemeManager.addOn(scene, Themes.DEFAULT, Themes.LEGACY);
                    primaryStage.setTitle("OpenCGL");
                    primaryStage.setResizable(true);
                    primaryStage.setScene(scene);

                    // 重新设置舞台位置使其居中
                    Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
                    primaryStage.setX((visualBounds.getWidth() - primaryStage.getWidth()) / 2);
                    primaryStage.setY((visualBounds.getHeight() - primaryStage.getHeight()) / 2);
                });
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Boolean lockingProcess(Stage stage) {
        File file = new File(BaseEnum.OPEN_CGL_LOCK_FILE);
        if (!file.exists()) {
            try {
                logger.info("begin create lock file {}", BaseEnum.OPEN_CGL_LOCK_FILE);
                if (!file.getParentFile().exists()) {
                    boolean directoryCreateResult = file.getParentFile().mkdirs();
                    logger.info("end create lock directory {} and result is [{}]", BaseEnum.OPEN_CGL_LOCK_FILE, directoryCreateResult);
                }
                boolean fileCreateResult = file.createNewFile();
                logger.info("end create lock file {} and result is [{}]", BaseEnum.OPEN_CGL_LOCK_FILE, fileCreateResult);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            @SuppressWarnings("resource")
            RandomAccessFile lockFile = new RandomAccessFile(BaseEnum.OPEN_CGL_LOCK_FILE, "rw");
            FileChannel channel = lockFile.getChannel();
            lock = channel.tryLock();
            if (lock == null) {
                StackPane stackPane = new StackPane();
                MFXButton quitButton = new MFXButton(I18N.getOrDefault("oepncgl.main.close.text"));
                quitButton.setButtonType(ButtonType.RAISED);
                quitButton.setAlignment(Pos.CENTER);
                quitButton.setMinHeight(40);
                quitButton.setMinWidth(100);
                quitButton.setOnAction(actionEvent -> System.exit(1));

                VBox vBox = new VBox();
                vBox.setFillWidth(true);
                VBox.setMargin(quitButton,new Insets(0,0,10,0));
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
            // 当程序关闭时释放文件锁
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    lock.release();
                }
                catch (IOException e) {
                    logger.error("", e);
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
