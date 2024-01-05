package org.tron.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SymmEncoder {

  private static final Logger LOG = LoggerFactory.getLogger(SymmEncoder.class);

  private static byte[] AesEcbDecode(byte[] encodedText, SecretKey key) {
    try {
      Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key);
      return cipher.doFinal(encodedText);
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

}