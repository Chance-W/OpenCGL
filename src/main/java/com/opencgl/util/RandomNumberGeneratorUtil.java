package com.opencgl.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/10 20:43
 * @since v2.0
 */
public class RandomNumberGeneratorUtil {
    public static int generateRandomNumber(int max) {
        return ThreadLocalRandom.current().nextInt(0, max);
    }
}
