
package com.iiordanov.bVNC;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class implements "Mac Authentication", which uses Diffie-Hellman
 * key agreement (along with MD5 and AES128) to authenticate users to
 * Apple Remote Desktop, the VNC server which is built-in to Mac OS X.
 *
 * This authentication technique is based on the following steps:
 *
 * 1. Perform Diffie-Hellman key agreement, so both sides have
 *    a shared secret key which can be used for further encryption.
 * 2. Take the MD5 hash of this DH secret key to produce a 128-bit
 *    value which we will use as the actual encryption key.
 * 3. Encrypt the username and password with this key using the AES
 *    128-bit symmetric cipher in electronic codebook (ECB) mode.  The
 *    username/password credentials are stored in a 128-byte structure,
 *    with 64 bytes for each, null-terminated.  Ideally, write random
 *    values into the portion of this 128-byte structure which is not
 *    occupied by the username or password, but no further padding for
 *    this block cipher.
 *
 * The ciphertext from step 3 and the DH public key from step 2
 * are sent to the server.
 */
public class RFBSecurityARD {
    
    // The type and name identifies this authentication scheme to
    // the rest of the RFB code.

    private static final String NAME = "Mac Authentication";

    public byte getType() {
        return RfbProto.SecTypeArd;
    }
    public String getTypeName() {
        return NAME;
    }

    // credentials
    private String username;
    private String password;


    private byte[] performAES128(byte[] key, byte[] plaintext) throws IOException {
        byte[] ciphertext;

        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            ciphertext = cipher.doFinal(plaintext);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new IOException(MSG_ERROR + " (AES128)");
        }

        return ciphertext;
    }

}