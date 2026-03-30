package org.hunau.common.util;

import org.hunau.common.exception.BusinessException;

public class AssertUtil {
    public static void notEmpty(String s, String msg) {
        if (StringUtil.isEmpty(s)) {
            throw new BusinessException(msg);
        }
    }

    public static void isTrue(boolean b, String msg) {
        if (!b) {
            throw new BusinessException(msg);
        }
    }

    public static void notNull(Object obj, String msg) {
        if (obj == null) {
            throw new BusinessException(msg);
        }
    }
}
