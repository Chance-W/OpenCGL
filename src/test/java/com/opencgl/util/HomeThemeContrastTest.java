package com.opencgl.util;

import com.opencgl.base.theme.ThemeManager;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXRectangleToggleNode;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HomeThemeContrastTest {
    @Test
    void darkHomeUsesReadableSelectionSurfacesAndPreservesPrimaryButton() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try { Platform.startup(started::countDown); }
        catch (IllegalStateException alreadyStarted) { started.countDown(); }
        assertTrue(started.await(10, TimeUnit.SECONDS));
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                Label version = new Label("当前版本 v2.2.3");
                version.getStyleClass().add("home-version");
                Label chip = new Label("JavaFX");
                chip.getStyleClass().add("home-tech-chip");
                MFXButton button = new MFXButton("访问项目主页");
                button.getStyleClass().add("home-primary-button");
                MFXRectangleToggleNode home = new MFXRectangleToggleNode("主页");
                home.setSelected(true);
                VBox navbar = new VBox(home);
                navbar.getStyleClass().add("navbar");
                VBox sidebar = new VBox(navbar);
                sidebar.getStyleClass().add("sidebar");
                VBox root = new VBox(sidebar, version, chip, button);
                root.getStyleClass().add("rootPane");
                root.getStylesheets().add(HomeThemeContrastTest.class.getResource("/com/opencgl/css/MainWindow.css").toExternalForm());
                Scene scene = new Scene(root, 600, 300);
                for (String css : new String[]{"MFXColors.css", "themes/ThemeTokens-dark.css", "themes/ThemeColors-dark.css", "themes/Accent-teal.css", "GlobalComponents.css"}) {
                    scene.getStylesheets().add(ThemeManager.class.getResource("/com/opencgl/base/css/" + css).toExternalForm());
                }
                root.applyCss();
                root.layout();
                for (javafx.scene.layout.Region node : new javafx.scene.layout.Region[]{home, version, chip}) {
                    Color bg = (Color) node.getBackground().getFills().get(0).getFill();
                    assertEquals(Color.web("#173F3B"), bg, node.getStyleClass().toString());
                }
                assertEquals(Color.web("#0F9D8A"), button.getBackground().getFills().get(0).getFill());
                assertEquals(1.0, ((Color) button.getTextFill()).getBrightness());
                assertTrue(((Color) button.getTextFill()).getOpacity() >= .85);
                assertTrue(((Color) version.getTextFill()).getBrightness() > .5);
                assertTrue(((Color) chip.getTextFill()).getBrightness() > .5);
                if (Boolean.getBoolean("opencgl.test.themeSnapshot")) {
                    javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(root.snapshot(null, null), null),
                        "png", new java.io.File("target/dark-home-theme.png"));
                }
            } catch (Throwable ex) { error.set(ex); }
            finally { finished.countDown(); }
        });
        assertTrue(finished.await(15, TimeUnit.SECONDS));
        if (error.get() != null) throw new AssertionError(error.get());
    }
}
