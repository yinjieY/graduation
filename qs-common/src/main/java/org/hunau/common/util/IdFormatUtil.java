package org.hunau.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class IdFormatUtil {

    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private IdFormatUtil() {
    }

    public static String newBatchId() {
        return "B" + timestampPart() + randomPart(6);
    }

    public static String newQsId() {
        return "QS" + timestampPart() + randomPart(6);
    }

    private static String timestampPart() {
        return LocalDateTime.now().format(TS_FORMATTER);
    }

    private static String randomPart(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int idx = ThreadLocalRandom.current().nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(idx));
        }
        return sb.toString();
    }
}

