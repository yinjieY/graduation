package org.hunau.trace.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class SignUtil {
    private SignUtil() {
    }

    /**
     * 论文实现阶段可替换为真实SM2签名，这里先使用SHA-256作为占位签名。
     */
    public static String mockSm2Sign(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("签名算法不可用", e);
        }
    }
}
