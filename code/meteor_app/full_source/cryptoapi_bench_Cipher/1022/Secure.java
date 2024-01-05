package org.aoju.bus.pay;

import org.aoju.bus.core.codec.Base64;
import org.aoju.bus.core.lang.Charset;
import org.aoju.bus.core.toolkit.StringKit;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA 非对称加密工具类
 */
public class Secure {

    /**
     * 证书和回调报文解密
     *
     * @param associatedData associated_data
     * @param nonce          nonce
     * @param cipherText     ciphertext
     * @return {String} 平台证书明文
     * @throws GeneralSecurityException 异常
     */
    public String decryptToString(byte[] associatedData, byte[] nonce, String cipherText) throws GeneralSecurityException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            SecretKeySpec key = new SecretKeySpec(aesKey, "AES");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce);

            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            cipher.updateAAD(associatedData);

            return new String(cipher.doFinal(Base64.decode(cipherText)), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException  e) {
            throw new IllegalStateException(e);
        } catch(NoSuchPaddingException e) {
            throw new IllegalStateException(e);
        } catch (InvalidKeyException  e) {
            throw new IllegalArgumentException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new IllegalArgumentException(e);
        }
    }

}