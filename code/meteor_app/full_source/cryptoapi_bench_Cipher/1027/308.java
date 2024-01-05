package cn.neptu.neplog.utils;

import cn.neptu.neplog.config.security.SecurityConfig;
import cn.neptu.neplog.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import javax.annotation.Resource;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Security;


public class AESUtil implements InitializingBean {


  
    public String encrypt(String content){
        Cipher encryptor;
        try {
            Key key = new SecretKeySpec(securityConfig.getAESKey().getBytes(StandardCharsets.UTF_8), "AES");
            encryptor = Cipher.getInstance("AES/ECB/PKCS7Padding");
            encryptor.init(Cipher.ENCRYPT_MODE, key);
        } catch (NoSuchAlgorithmException  e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            return new String(Base64Utils.encode(encryptor.doFinal(content.getBytes(StandardCharsets.UTF_8))));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
