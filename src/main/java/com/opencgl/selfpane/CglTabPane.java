package com.opencgl.selfpane;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/04 12:54
 * @since v2.0
 */
public class CglTabPane extends TabPane {

    public CglTabPane() {
        super();
    }

    public CglTabPane(Tab... tabs) {
        super(tabs);
    }

    @Override
    public ObservableList<Node> getChildren() {
        return super.getChildren();
    }
}
