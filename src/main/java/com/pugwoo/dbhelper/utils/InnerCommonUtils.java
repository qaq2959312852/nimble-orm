package com.pugwoo.dbhelper.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 内部常用工具类
 */
public class InnerCommonUtils {

    /**
     * 判断给定的数组是否非空
     */
    public static boolean isNotEmpty(String[] strs) {
        return strs != null && strs.length > 0;
    }

    /**
     * 检查strs中是否包含有checkStr字符串
     */
    public static boolean isContains(String checkStr, String[] strs) {
        if (!isNotEmpty(strs)) {
            return false;
        }

        boolean isContain = false;
        for (String str : strs) {
            if (Objects.equals(str, checkStr)) {
                isContain = true;
                break;
            }
        }

        return isContain;
    }

    /**
     * 将字符串str按间隔符sep分隔，返回分隔后的字符串
     * @param str 字符串
     * @param sep 间隔符
     * @return 会自动过滤掉空白(blank)的字符串；并且会自动trim()
     */
    public static List<String> split(String str, String sep) {
        if (str == null || str.isEmpty()) {
            return new ArrayList<>();
        }

        String[] splits = str.split(sep);
        List<String> result = new ArrayList<>();
        for (String s : splits) {
            if (s != null && !s.trim().isEmpty()) {
                result.add(s.trim());
            }
        }
        return result;
    }


}
