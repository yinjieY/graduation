package org.hunau.common.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.SM2;

import java.nio.charset.StandardCharsets;

public final class Sm2Util {

    private static final String PREFIX = "SM2:";

    private Sm2Util() {
    }

    public static String sign(String payload, String privateKey) {
        String normalizedPayload = requireText(payload, "Sm2Util#sign payload不能为空");
        String normalizedPrivateKey = requireText(privateKey, "Sm2Util#sign 配置项 app.crypto.sm2.private-key 不能为空");
        try {
            SM2 sm2 = SmUtil.sm2(normalizeHex(normalizedPrivateKey), null);
            byte[] signBytes = sm2.sign(normalizedPayload.getBytes(StandardCharsets.UTF_8));
            return PREFIX + HexUtil.encodeHexStr(signBytes).toUpperCase();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Sm2Util#sign 执行失败，请检查 app.crypto.sm2.private-key 格式（建议使用 PKCS8 HEX）。detail="
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
        }
    }

    public static boolean verify(String payload, String signature, String publicKey) {
        String normalizedPayload = requireText(payload, "Sm2Util#verify payload不能为空");
        String normalizedSignature = normalize(signature);
        if (normalizedSignature == null || normalizedSignature.isBlank()) {
            throw new IllegalArgumentException("Sm2Util#verify signature不能为空");
        }
        String normalizedPublicKey = requireText(publicKey, "Sm2Util#verify 配置项 app.crypto.sm2.public-key 不能为空");

        try {
            SM2 sm2 = SmUtil.sm2(null, normalizeHex(normalizedPublicKey));
            return sm2.verify(normalizedPayload.getBytes(StandardCharsets.UTF_8), HexUtil.decodeHex(normalizedSignature));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Sm2Util#verify 执行失败，请检查 app.crypto.sm2.public-key 格式（建议使用 X509 HEX）或 signature 编码。detail="
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
        }
    }

    private static String requireText(String value, String errorMsg) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMsg);
        }
        return value.trim();
    }

    private static String normalize(String signature) {
        if (signature == null) {
            return null;
        }
        String trimmed = signature.trim();
        if (trimmed.startsWith(PREFIX)) {
            trimmed = trimmed.substring(PREFIX.length());
        }
        if (trimmed.startsWith("SM2-")) {
            trimmed = trimmed.substring("SM2-".length());
        }
        return trimmed;
    }

    private static String normalizeHex(String hex) {
        if (hex == null) {
            return null;
        }
        String trimmed = hex.trim();
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            trimmed = trimmed.substring(2);
        }
        return trimmed.isEmpty() ? null : trimmed;
    }
}
