package com.microsoft.windowsazure.services.media;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.microsoft.windowsazure.core.utils.Base64;

public final class EncryptionUtils {
    
    public static String calculateChecksum(byte[] contentKey, UUID contentKeyIdUuid) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(contentKey, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptionResult = cipher.doFinal(contentKeyIdUuid.toString().getBytes(StandardCharsets.UTF_8));
            byte[] checksumByteArray = new byte[8];
            System.arraycopy(encryptionResult, 0, checksumByteArray, 0, 8);
            return Base64.encode(checksumByteArray);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}