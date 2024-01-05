package io.jboot.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.SecureRandom;


/**
 * DESUtil 对称加密工具类，非对称加密请参考 RSAUtil
 */
public class DESUtil {

    public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("DES");
        DESKeySpec ds = new DESKeySpec(key);
        SecureRandom sr = new SecureRandom();
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey skey = secretKeyFactory.generateSecret(ds);
        cipher.init(Cipher.ENCRYPT_MODE, skey, sr);
        return cipher.doFinal(data);
    }

}