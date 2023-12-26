package com.opencgl.selfpane;

import java.io.InputStream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/06 09:53
 * @since v9.0
 */
public class OpenCGLVbox extends VBox {

    private final String STYLE_CLASS = "opencgl-vbox";

    public OpenCGLVbox() {
        initialize();
    }


    private void initialize() {
        getStyleClass().add(STYLE_CLASS);
        setAlignment(Pos.CENTER);
        setPrefHeight(70);
        setPrefWidth(250);
        getStylesheets().add(getClass().getResource("/com/opencgl/css/OpencglVbox.css").toExternalForm());
    }
}
