package ixcode.platform.cryptography;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.security.Key;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

public class AesEncryption implements Encryption {
    private final KeyGenerator keyGenerator;
    private final Cipher cipher;
    private final Key key;

    public AesEncryption() {
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
            cipher = Cipher.getInstance("AES");
            key = keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override public String encrypt(String source) {
        try {
            cipher.init(ENCRYPT_MODE, key);
            return base64Encode(cipher.doFinal(source.getBytes("UTF8")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override public String decrypt(String source) {
        try {
            cipher.init(DECRYPT_MODE, key);
            return new String(cipher.doFinal(base64Decode(source)), "UTF8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String base64Encode(byte[] bytes) {
        return new String(Base64.encodeBase64(bytes));
    }

    private byte[] base64Decode(String string) {
        return Base64.decodeBase64(string);
    }


}