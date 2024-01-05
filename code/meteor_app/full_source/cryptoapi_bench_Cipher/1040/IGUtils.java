package com.github.instagram4j.instagram4j.utils;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.instagram4j.instagram4j.IGConstants;
import lombok.SneakyThrows;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class IGUtils {
        public static String encryptPassword(String password, String enc_id, String enc_pub_key) {
                byte[] rand_key = new byte[32], iv = new byte[12];
                SecureRandom sran = new SecureRandom();
                sran.nextBytes(rand_key);
                sran.nextBytes(iv);
                String time = String.valueOf(System.currentTimeMillis() / 1000);

                // Encrypt random key
                String decoded_pub_key =
                        new String(Base64.getDecoder().decode(enc_pub_key), StandardCharsets.UTF_8)
                                .replaceAll("-(.*)-|\n", "");
                Cipher rsa_cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
                rsa_cipher.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA")
                        .generatePublic(
                                new X509EncodedKeySpec(Base64.getDecoder().decode(decoded_pub_key))));
                byte[] rand_key_encrypted = rsa_cipher.doFinal(rand_key);

                // Encrypt password
                Cipher aes_gcm_cipher = Cipher.getInstance("AES/GCM/NoPadding");
                aes_gcm_cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(rand_key, "AES"),
                        new GCMParameterSpec(128, iv));
                aes_gcm_cipher.updateAAD(time.getBytes());
                byte[] password_encrypted = aes_gcm_cipher.doFinal(password.getBytes());

                // Write to final byte array
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                out.write(Integer.valueOf(1).byteValue());
                out.write(Integer.valueOf(enc_id).byteValue());
                out.write(iv);
                out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                        .putChar((char) rand_key_encrypted.length).array());
                out.write(rand_key_encrypted);
                out.write(Arrays.copyOfRange(password_encrypted, password_encrypted.length - 16,
                        password_encrypted.length));
                out.write(Arrays.copyOfRange(password_encrypted, 0, password_encrypted.length - 16));

                return String.format("#PWD_INSTAGRAM:%s:%s:%s", "4", time,
                        Base64.getEncoder().encodeToString(out.toByteArray()));
        }

}