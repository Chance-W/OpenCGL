package com.opencgl.util;

/**
 * @author Chance.W
 * @version 1.0
 * @CreateDate 2023/06/11 16:05
 * @since v2.0
 */
public class NumberUtil {
    /**
     * 数字不足自动补零
     *
     * @param num    需要补零的数字
     * @param length 补零之后数字的总长度
     * @return 补零之后的字符串
     */
    public static String addZeroForNum(int num, int length) {
        String str = String.valueOf(num);
        int strLen = str.length();
        if (strLen < length) {
            while (strLen < length) {
                StringBuffer sb = new StringBuffer();
                sb.append("0").append(str);//左补0
                // sb.append(str).append("0");//右补0
                str = sb.toString();
                strLen = str.length();
            }
        }
        return str;
    }
}
