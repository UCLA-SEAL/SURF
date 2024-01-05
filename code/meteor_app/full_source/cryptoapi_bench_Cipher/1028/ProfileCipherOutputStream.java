package org.session.libsignal.streams;

import static org.session.libsignal.crypto.CipherUtil.CIPHER_LOCK;

import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ProfileCipherOutputStream extends DigestingOutputStream {

  private final Cipher cipher;

  public ProfileCipherOutputStream(OutputStream out, byte[] key) throws IOException {
    super(out);
    try {
      this.cipher = Cipher.getInstance("AES/GCM/NoPadding");

      byte[] nonce  = generateNonce();
      this.cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));

      super.write(nonce, 0, nonce.length);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    } catch (NoSuchPaddingException e) {
      throw new AssertionError(e);
    } catch (InvalidAlgorithmParameterException e) {
      throw new AssertionError(e);
    } catch (InvalidKeyException e) {
      throw new IOException(e);
    }
  }
}