package org.hunau.common;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.SM2;

public class Sm2KeyGenDemo {
    public static void main(String[] args) {
        SM2 sm2 = SmUtil.sm2();

        // SmUtil.sm2(String,String) expects encoded key material (PKCS8 private, X509 public)
        String privateEncodedHex = HexUtil.encodeHexStr(sm2.getPrivateKey().getEncoded()).toUpperCase();
        String publicEncodedHex = HexUtil.encodeHexStr(sm2.getPublicKey().getEncoded()).toUpperCase();

        System.out.println("PRIVATE_PKCS8_HEX: " + privateEncodedHex);
        System.out.println("PRIVATE_PKCS8_LEN: " + privateEncodedHex.length());
        System.out.println("PUBLIC_X509_HEX: " + publicEncodedHex);
        System.out.println("PUBLIC_X509_LEN: " + publicEncodedHex.length());
    }
}
