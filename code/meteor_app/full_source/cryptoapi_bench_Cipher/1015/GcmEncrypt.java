package burp.Bootstrap.Encrypt;

import org.apache.shiro.codec.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class GcmEncrypt implements EncryptInterface {
    @Override
    public String encrypt(String key, byte[] payload) {
        try {
            byte[] raw = Base64.decode(key);
            byte[] ivs = generateInitializationVector();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec iv = new GCMParameterSpec(128, ivs);
            cipher.init(1, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(pad(payload));
            return new String(Base64.encode(byteMerger(ivs, encrypted)));
        } catch (Exception exception) {
            return "0";
        }
    }
}