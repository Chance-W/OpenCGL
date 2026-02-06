package com.opencgl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.opencgl.base.theme.AccentColor;
import com.opencgl.base.theme.ThemeMode;
import org.junit.jupiter.api.Test;

class ThemeMenuTextTest {

    @Test
    void marksOnlyTheSelectedMode() {
        assertEquals("✓ 显示模式 · 浅色", ThemeMenuText.mode(ThemeMode.LIGHT, ThemeMode.LIGHT));
        assertEquals("　显示模式 · 深色", ThemeMenuText.mode(ThemeMode.DARK, ThemeMode.LIGHT));
    }

    @Test
    void marksOnlyTheSelectedAccent() {
        assertEquals("✓ 强调色 · 蓝色", ThemeMenuText.accent(AccentColor.BLUE, AccentColor.BLUE));
        assertEquals("　强调色 · 青色", ThemeMenuText.accent(AccentColor.TEAL, AccentColor.BLUE));
    }
}
