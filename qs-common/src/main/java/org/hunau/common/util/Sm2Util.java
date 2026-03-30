package org.hunau.common.util;

public final class Sm2Util {

    private Sm2Util() {
    }

    // Placeholder implementation: combine payload and key material then hash.
    public static String sign(String payload, String privateKey) {
        return HashUtil.sha256Hex(payload + "|" + privateKey);
    }

    public static boolean verify(String payload, String signature, String publicKey) {
        String expected = HashUtil.sha256Hex(payload + "|" + publicKey);
        return expected.equals(signature);
    }
}

