
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;

import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;


import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;


public class SendCalibrations {

    public static byte[] encrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes) {
        try {
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
            return cipher.doFinal(textBytes);
        } catch (Exception e) {
            System.err.println("Error during encryption: " + e.toString());
            return errorbyte;
        }
    }

}