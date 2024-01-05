package com.wechat.pay.java.core.cipher;

import com.wechat.pay.java.core.exception.DecryptionException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

/** 带有关联数据的认证加密器 */
public abstract class AbstractAeadCipher implements AeadCipher {
  /**
   * 解密并转换为字符串
   *
   * @param associatedData AAD，额外的认证加密数据，可以为空
   * @param nonce IV，随机字符串初始化向量
   * @param ciphertext 密文
   * @return UTF-8编码的明文
   */
  public String decrypt(byte[] associatedData, byte[] nonce, byte[] ciphertext) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key, algorithm),
          new GCMParameterSpec(tagLengthBit, nonce));
      if (associatedData != null) {
        cipher.updateAAD(associatedData);
      }
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (InvalidKeyException e) {
        throw new IllegalArgumentException(e);
    } catch(InvalidAlgorithmParameterException e) {
        throw new IllegalArgumentException(e);
    } catch(NoSuchAlgorithmException e) {
        throw new IllegalArgumentException(e);
    } catch(NoSuchPaddingException e) {
        throw new IllegalArgumentException(e);
    } catch (BadPaddingException e) {
        throw new DecryptionException("Decryption failed", e);
    } catch (IllegalBlockSizeException e) {
        throw new DecryptionException("Decryption failed", e);
    }
  }
}