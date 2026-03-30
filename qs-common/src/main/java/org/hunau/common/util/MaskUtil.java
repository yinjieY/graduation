package org.hunau.common.util;

public final class MaskUtil {

    private MaskUtil() {
    }

    public static String maskPhone(String phone) {
        if (StringUtil.isEmpty(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskIp(String ip) {
        if (StringUtil.isEmpty(ip)) {
            return ip;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return ip;
        }
        return parts[0] + "." + parts[1] + "." + parts[2] + ".*";
    }
}

