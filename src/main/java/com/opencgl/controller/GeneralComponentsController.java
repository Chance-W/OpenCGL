package com.opencgl.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.opencgl.service.ChangePaneContentService;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/04 14:51
 * @since v9.0
 */
public class GeneralComponentsController implements Initializable {
    @FXML
    public MFXScrollPane generalComponentsGridPane;
    @FXML
    public FlowPane flowPanel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
       // generalComponentsGridPane.setM(new Insets(0,0,0,100));
/*
        flowPanel.getChildren().add(new MFXButton("AAAAA"));
*/
    }
}
