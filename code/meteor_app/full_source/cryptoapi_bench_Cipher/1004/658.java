package com.netzwerk.savechat;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Crypt {


    public static String encrypt(String string, PublicKey publicKey, SecretKey secKey) {
        int AES_BIT = 256;
        int AES_LEN = AES_BIT * 2;

        byte[] iv = new byte[16];
        byte[] aes_key = new byte[AES_LEN];
        byte[] data = new byte[40];
        try {
            // generate initialization vector
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            IvParameterSpec ivspec = new IvParameterSpec(iv);
            // encrypt data
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secKey, ivspec);
            data = cipher.doFinal(string.getBytes(StandardCharsets.UTF_16));
            // wrap key
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.WRAP_MODE, publicKey);
            aes_key = cipher.wrap(secKey);
        } catch (InvalidAlgorithmParameterException ex) {
            System.out.println("WTF how did this happen??! " + ex.getMessage());
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("WTF how did this happen??! " + ex.getMessage());
            ex.printStackTrace();
        } catch (IllegalBlockSizeException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } catch ( BadPaddingException ex ) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } catch (NoSuchPaddingException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } catch (InvalidKeyException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        }

        // concatenate encrypted key
        byte[] result = new byte[16 + AES_LEN + data.length];
        System.arraycopy(iv, 0, result, 0, 16);
        System.arraycopy(aes_key, 0, result, 16, AES_LEN);
        System.arraycopy(data, 0, result, AES_LEN + 16, data.length);

        return encode(result);
    }

}