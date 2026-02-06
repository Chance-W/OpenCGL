package com.opencgl.selfpane;

import java.util.Objects;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/06 09:53
 * @since v2.0
 */
public class OpenCGLVbox extends VBox {

    public OpenCGLVbox() {
        initialize();
    }


    private void initialize() {
        String STYLE_CLASS = "opencgl-vbox";
        getStyleClass().add(STYLE_CLASS);
        setAlignment(Pos.CENTER);
        setPrefHeight(70);
        setPrefWidth(250);
       // getStylesheets().add(Objects.requireNonNull(getClass().getResource("/com/opencgl/css/OpencglVbox.css")).toExternalForm());
    }
}
