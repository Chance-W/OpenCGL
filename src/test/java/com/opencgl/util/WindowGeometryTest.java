package com.opencgl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WindowGeometryTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("windowPlacements")
    void fitsRestoredWindowEntirelyInsideTargetScreen(
        String scenario,
        WindowGeometry.Rect desired,
        WindowGeometry.Rect target,
        WindowGeometry.Rect expected
    ) {
        WindowGeometry.Rect actual = WindowGeometry.fitInside(desired, target, 0.95, 0.95);

        assertEquals(expected, actual);
        assertTrue(actual.x() >= target.x());
        assertTrue(actual.y() >= target.y());
        assertTrue(actual.maxX() <= target.maxX());
        assertTrue(actual.maxY() <= target.maxY());
    }

    private static Stream<Arguments> windowPlacements() {
        return Stream.of(
            Arguments.of(
                "keeps an already visible window unchanged",
                new WindowGeometry.Rect(100, 80, 900, 600),
                new WindowGeometry.Rect(0, 0, 1920, 1040),
                new WindowGeometry.Rect(100, 80, 900, 600)),
            Arguments.of(
                "supports a secondary screen with negative coordinates",
                new WindowGeometry.Rect(-2200, 100, 1400, 900),
                new WindowGeometry.Rect(-2560, 0, 2560, 1400),
                new WindowGeometry.Rect(-2200, 100, 1400, 900)),
            Arguments.of(
                "shrinks a window that is larger than the target screen",
                new WindowGeometry.Rect(100, 100, 1800, 1200),
                new WindowGeometry.Rect(0, 0, 1280, 720),
                new WindowGeometry.Rect(64, 36, 1216, 684)),
            Arguments.of(
                "moves an off-screen window back into the target screen",
                new WindowGeometry.Rect(5000, -900, 1000, 700),
                new WindowGeometry.Rect(1920, 0, 1920, 1040),
                new WindowGeometry.Rect(2840, 0, 1000, 700))
        );
    }
}
