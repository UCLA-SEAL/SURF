package com.lamfire.code;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class DSA {

    public static final String ALGORITHM = "DSA";
    private static final int   KEY_SIZE  = 1024;

    public static KeyPair genKeyPair(String seed) throws Exception {
        KeyPairGenerator keygen = KeyPairGenerator.getInstance(ALGORITHM);
        // 初始化随机产生器
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.setSeed(seed.getBytes());
        keygen.initialize(KEY_SIZE, secureRandom);
        KeyPair keyPair = keygen.genKeyPair();
        return keyPair;
    }

}