package com.opencgl.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.scene.Cursor;
import org.junit.jupiter.api.Test;

class FlexibleListenerTest {

    @Test
    void resolvesResizeCursorOnlyInsideWindowEdges() {
        assertEquals(Cursor.NW_RESIZE, FlexibleListener.cursorFor(1, 1, 1280, 720));
        assertEquals(Cursor.E_RESIZE, FlexibleListener.cursorFor(1279, 300, 1280, 720));
        assertEquals(Cursor.DEFAULT, FlexibleListener.cursorFor(640, 360, 1280, 720));
    }
}
