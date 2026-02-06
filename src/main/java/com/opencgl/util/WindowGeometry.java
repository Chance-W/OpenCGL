package com.opencgl.util;

/** Pure geometry helpers for keeping desktop windows inside a target screen. */
public final class WindowGeometry {
    private WindowGeometry() {
    }

    public static Rect fitInside(Rect desired, Rect target, double maxWidthRatio, double maxHeightRatio) {
        if (desired == null || target == null) {
            throw new IllegalArgumentException("desired and target rectangles are required");
        }
        if (target.width() <= 0 || target.height() <= 0) {
            throw new IllegalArgumentException("target rectangle must have a positive size");
        }
        if (maxWidthRatio <= 0 || maxWidthRatio > 1 || maxHeightRatio <= 0 || maxHeightRatio > 1) {
            throw new IllegalArgumentException("maximum size ratios must be in the range (0, 1]");
        }

        double width = Math.min(Math.max(0, desired.width()), target.width() * maxWidthRatio);
        double height = Math.min(Math.max(0, desired.height()), target.height() * maxHeightRatio);
        double x = clamp(desired.x(), target.x(), target.maxX() - width);
        double y = clamp(desired.y(), target.y(), target.maxY() - height);
        return new Rect(x, y, width, height);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    public record Rect(double x, double y, double width, double height) {
        public double maxX() {
            return x + width;
        }

        public double maxY() {
            return y + height;
        }
    }
}
