package org.sonar.api.config.internal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.annotation.Nullable;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

final class AesGCMCipher extends AesCipher {
  @Override
  public String encrypt(String clearText) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      byte[] iv = new byte[GCM_IV_LENGTH_IN_BYTES];
      new SecureRandom().nextBytes(iv);
      cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, loadSecretFile(), new GCMParameterSpec(GCM_TAG_LENGTH_IN_BITS, iv));
      byte[] encryptedText = cipher.doFinal(clearText.getBytes(StandardCharsets.UTF_8));
      return Base64.encodeBase64String(
        ByteBuffer.allocate(GCM_IV_LENGTH_IN_BYTES + encryptedText.length)
          .put(iv)
          .put(encryptedText)
          .array());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

}