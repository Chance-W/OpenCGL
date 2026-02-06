package com.opencgl.util;

import com.opencgl.base.theme.AccentColor;
import com.opencgl.base.theme.ThemeMode;

/** Produces aligned, accessible labels for the two-dimensional theme menu. */
public final class ThemeMenuText {
    private static final String SELECTED = "✓ ";
    private static final String UNSELECTED = "　";

    private ThemeMenuText() {
    }

    public static String mode(ThemeMode value, ThemeMode selected) {
        return marker(value == selected) + switch (value) {
            case SYSTEM -> "显示模式 · 跟随系统";
            case LIGHT -> "显示模式 · 浅色";
            case DARK -> "显示模式 · 深色";
        };
    }

    public static String accent(AccentColor value, AccentColor selected) {
        return marker(value == selected) + switch (value) {
            case TEAL -> "强调色 · 青色";
            case BLUE -> "强调色 · 蓝色";
            case PURPLE -> "强调色 · 紫色";
            case OCEAN -> "强调色 · 海蓝";
        };
    }

    private static String marker(boolean selected) {
        return selected ? SELECTED : UNSELECTED;
    }
}
