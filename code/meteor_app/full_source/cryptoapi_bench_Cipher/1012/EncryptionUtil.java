package org.hive2hive.core.security;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.hive2hive.core.model.versioned.HybridEncryptedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides fundamental functionalities for data encryption, decryption, signing and verification.
 * Provided are both symmetric as well as asymmetric encryption approaches. Furthermore, it provides methods
 * to generate various parameters, such as keys and key pairs.
 * 
 * @author Christian
 * @author Nico
 * 
 */
public final class EncryptionUtil {

	private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);

	// This variable is not final in order to swap the provider with another (like SpongyCastle)
	// public static String SECURITY_PROVIDER = "BC";

	private static final String SINGATURE_ALGORITHM = "SHA1withRSA";
	// Fermat F4, largest known fermat prime
	private static final BigInteger RSA_PUBLIC_EXP = new BigInteger("10001", 16);
	private static final int IV_LENGTH = 16;

	private static byte[] processAESCiphering(boolean forEncrypting, byte[] data, SecretKey key, byte[] initVector,
			String securityProvider) throws GeneralSecurityException {
		IvParameterSpec ivSpec = new IvParameterSpec(initVector);
		SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", securityProvider);
		int encryptMode = forEncrypting ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
		cipher.init(encryptMode, keySpec, ivSpec);

		// process ciphering
		byte[] output = new byte[cipher.getOutputSize(data.length)];

		int bytesProcessed1 = cipher.update(data, 0, data.length, output, 0);
		int bytesProcessed2 = cipher.doFinal(output, bytesProcessed1);

		byte[] result = new byte[bytesProcessed1 + bytesProcessed2];
		System.arraycopy(output, 0, result, 0, result.length);
		return result;

	}
}