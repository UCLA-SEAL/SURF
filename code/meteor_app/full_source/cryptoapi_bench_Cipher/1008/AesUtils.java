package com.github.binarywang.wxpay.v3.util;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class AesUtils {

  public static byte[] decryptToByte(byte[] associatedData, byte[] nonce, byte[] cipherData, byte[] key)
    throws GeneralSecurityException {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

      SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
      GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, nonce);

      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);
      if (associatedData != null) {
        cipher.updateAAD(associatedData);
      }
      return cipher.doFinal(cipherData);
    } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(e);
    } catch (NoSuchPaddingException e) {
        throw new IllegalStateException(e);
    } catch (InvalidKeyException e) {
        throw new IllegalArgumentException(e);
    } catch (InvalidAlgorithmParameterException e) {
        throw new IllegalArgumentException(e);
    }
  }
}