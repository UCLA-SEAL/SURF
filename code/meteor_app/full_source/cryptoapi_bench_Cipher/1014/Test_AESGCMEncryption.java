 *******************************************************************************/
package org.osc.core.broker.util.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.osc.core.broker.service.api.server.EncryptionException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class AESGCMEncryption {
    private byte[] process(int optMode, byte[] plainText, SecretKey key, byte[] iv, byte[] aad) throws EncryptionException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(iv.length * 8, iv);
            cipher.init(optMode, key, spec);
            cipher.updateAAD(aad);
            return cipher.doFinal(plainText);
        } catch (NoSuchAlgorithmException e) {
            String msg = "AES/GCM/NoPadding algorithm is not supported.";
            LOG.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (NoSuchPaddingException e) {
            String msg = "Failed to create cipher.";
            LOG.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (InvalidKeyException e) {
            String msg = "The key used for AES GCM encryption is invalid.";
            LOG.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (InvalidAlgorithmParameterException e) {
            String msg = "IV used for AES GCM encryption is invalid.";
            LOG.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (IllegalBlockSizeException e) {
            String msg = "Failed to perform encryption (i.e. data length doesn't match block size).";
            LOG.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (AEADBadTagException e) {
            String msg = "Failed to perform encryption. Additional password is incorrect.";
            LOG.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (BadPaddingException e) {
            String msg = "Failed to perform encryption. Plain text is not padded properly.";
            LOG.error(msg, e);
            throw new EncryptionException(msg, e);
        } catch (Exception e) {
            String msg = "Failed to perform encryption.";
            LOG.error(msg, e);
            throw new EncryptionException(msg, e);
        }
    }
}