// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.security.hpke;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * AES-128 GCM implementation of AEAD
 *
 * @author vekterli
 */
final class Aes128Gcm implements Aead {

    /**
     * @param key Symmetric key bytes for encryption/decryption
     * @param nonce Nonce to use for the encryption/decrytion
     * @param aad Associated authenticated data that will <em>not</em> be encrypted
     * @param text Plaintext to seal or ciphertext to open, depending on cipherMode
     * @return resulting ciphertext or plaintext, depending on cipherMode
     */
    private byte[] aeadImpl(int cipherMode, byte[] key, byte[] nonce, byte[] aad, byte[] text) {
        try {
            Cipher cipher  = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(nT() * 8/* in bits */, nonce);
            SecretKeySpec aesKey  = new SecretKeySpec(key, "AES");
            cipher.init(cipherMode, aesKey, gcmSpec);
            cipher.updateAAD(aad);
            return cipher.doFinal(text);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}
