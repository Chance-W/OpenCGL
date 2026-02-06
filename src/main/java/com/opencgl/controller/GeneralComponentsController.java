package com.opencgl.controller;

import java.net.URL;
import java.util.ResourceBundle;

import com.opencgl.i18n.I18N;
import io.github.palexdev.materialfx.controls.MFXScrollPane;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/04 14:51
 * @since v2.0
 */
public class GeneralComponentsController implements Initializable {
    @FXML
    public MFXScrollPane generalComponentsGridPane;
    @FXML
    public FlowPane flowPanel;
    @FXML
    public MFXTextField searchField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        searchField.floatingTextProperty().bind(I18N.getBinding("opencgl.market.search_prompt"));

    }
}
