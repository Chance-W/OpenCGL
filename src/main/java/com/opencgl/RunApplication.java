package com.opencgl;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.opencgl.controller.MainController;
import com.opencgl.i18n.I18N;
import fr.brouillard.oss.cssfx.CSSFX;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.css.themes.MFXThemeManager;
import io.github.palexdev.materialfx.css.themes.Themes;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class RunApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
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

        CompletableFuture.runAsync(() -> {
            try {
                CSSFX.start();
                FXMLLoader loader = new FXMLLoader(RunApplication.class.getClassLoader().getResource("com/opencgl/view/Main.fxml"));
                loader.setControllerFactory(c -> new MainController(primaryStage));
                Parent root = loader.load();
                Platform.runLater(() -> {
                    Scene scene = new Scene(root);
                    scene.setFill(Color.TRANSPARENT);
                    MFXThemeManager.addOn(scene, Themes.DEFAULT, Themes.LEGACY);
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
}
