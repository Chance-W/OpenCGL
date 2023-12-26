package com.opencgl;

import com.opencgl.controller.MainController;
import fr.brouillard.oss.cssfx.CSSFX;
import io.github.palexdev.materialfx.css.themes.MFXThemeManager;
import io.github.palexdev.materialfx.css.themes.Themes;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class RunApplication extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		CSSFX.start();
		FXMLLoader loader = new FXMLLoader(RunApplication.class.getClassLoader().getResource("com/opencgl/view/Main.fxml"));
		loader.setControllerFactory(c -> new MainController(primaryStage));
		Parent root = loader.load();
		Scene scene = new Scene(root);
		MFXThemeManager.addOn(scene, Themes.DEFAULT, Themes.LEGACY);
		scene.setFill(Color.TRANSPARENT);
		primaryStage.initStyle(StageStyle.TRANSPARENT);
		primaryStage.setScene(scene);
		primaryStage.setTitle("OpenCGL");
		primaryStage.setResizable(true);
		primaryStage.show();
	}
}
