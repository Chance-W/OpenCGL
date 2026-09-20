package com.opencgl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunApplicationIconPolicyTest {

    @Test
    void bundledMacApplicationKeepsItsNativeDockIcon() throws Exception {
        Method policy = assertDoesNotThrow(
                () -> RunApplication.class.getDeclaredMethod("shouldSetStageIcon", String.class),
                "启动图标策略缺失，macOS 会继续覆盖原生 Dock 图标"
        );
        policy.setAccessible(true);

        assertFalse((boolean) policy.invoke(null, "Mac OS X"));
        assertTrue((boolean) policy.invoke(null, "Windows 11"));
        assertTrue((boolean) policy.invoke(null, "Linux"));
    }
}
