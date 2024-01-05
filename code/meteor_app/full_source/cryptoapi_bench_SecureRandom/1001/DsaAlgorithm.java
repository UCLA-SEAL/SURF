package com.soft.token.asymmetric.dsa;

import com.soft.token.constant.DsaConstant;
import org.apache.commons.codec.binary.Base64;

import java.security.*;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * dsa 算法
 *
 * @author suphowe
 */
public class DsaAlgorithm {
    public static HashMap<String, Object> initKey(String seed) throws Exception {
        KeyPairGenerator keygen = KeyPairGenerator.getInstance(DsaConstant.ALGORITHM);
        // 初始化随机产生器
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.setSeed(seed.getBytes());
        keygen.initialize(DsaConstant.DSA_KEY_SIZE, secureRandom);

        KeyPair keys = keygen.genKeyPair();

        DSAPublicKey publicKey = (DSAPublicKey) keys.getPublic();
        DSAPrivateKey privateKey = (DSAPrivateKey) keys.getPrivate();

        HashMap<String, Object> map = new HashMap<>(4);
        map.put(DsaConstant.DEFAULT_PUBLIC_KEY, Base64.encodeBase64String(publicKey.getEncoded()));
        map.put(DsaConstant.DEFAULT_PRIVATE_KEY, Base64.encodeBase64String(privateKey.getEncoded()));
        return map;
    }

}