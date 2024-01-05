package com.mjeanroy.springhub.commons.crypto;

import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;
import static java.security.MessageDigest.getInstance;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Crypto {

	private Crypto() {
	}

	/** Class logger */
	private static final Logger log = LoggerFactory.getLogger(Crypto.class);

	/** MD5 Code. */
	private static final String MD5 = "MD5";

	/** SHA-256 Code. */
	private static final String SHA256 = "SHA-256";

	/** AES Code. */
	private static final String AES = "AES";

	/** Array of alpha numerics characters. */
	private static final char[] alphaNumerics = new char[]{
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
			'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
	};

	/**
	 * Encrypt string with md5 encryption.
	 *
	 * @param str String to encrypt.
	 * @return Encrypted string.
	 */
	public static String md5(String str) {
		return crypt(str, MD5);
	}

	/**
	 * Encrypt string with SHA-256 encryption.
	 *
	 * @param str String to encrypt.
	 * @return Encrypted string.
	 */
	public static String sha256(String str) {
		return crypt(str, SHA256);
	}

	/**
	 * Encrypt string with AES encryption.
	 *
	 * @param data Data to encrypt.
	 * @param secret Secret use for encryption.
	 * @return Encrypted data.
	 */
	public static String encryptAES(String data, String secret) {
		try {
			Key key = generateAESKey(secret);
			Cipher c = Cipher.getInstance(AES);
			c.init(Cipher.ENCRYPT_MODE, key);
			byte[] encVal = c.doFinal(data.getBytes());
			return new String(Base64.encodeBase64(encVal));
		}
		catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return data;
		}
	}

	/**
	 * Encrypt string with AES encryption and URL encode result in UTF-8.
	 *
	 * @param data Data to encrypt.
	 * @param salt Salt use to encrypt data.
	 * @param secret Secret use for encryption.
	 * @return Encrypted data.
	 */
	public static String encryptAES_UTF8(String data, String salt, String secret) {
		return encryptAES_UTF8(salt + data, secret);
	}

	/**
	 * Encrypt string with AES encryption and URL encode result in UTF-8.
	 *
	 * @param data Data to encrypt.
	 * @param secret Secret use for encryption.
	 * @return Encrypted data.
	 */
	public static String encryptAES_UTF8(String data, String secret) {
		try {
			return encode(Crypto.encryptAES(data, secret), "UTF-8");
		}
		catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return data;
		}
	}

	/**
	 * Encrypt string with AES encryption.
	 *
	 * @param data Data to encrypt.
	 * @param salt Salt use to encrypt data.
	 * @param secret Secret use for encryption.
	 * @return Encrypted data.
	 */
	public static String encryptAES(String data, String salt, String secret) {
		return Crypto.encryptAES(salt + data, secret);
	}

	/**
	 * Decrypt string with AES encryption.
	 *
	 * @param encryptedData Data to decrypt.
	 * @param secret Secret use for decryption.
	 * @return Decrypted data.
	 */
	public static String decryptAES(String encryptedData, String secret) {
		try {
			Key key = generateAESKey(secret);
			Cipher c = Cipher.getInstance(AES);
			c.init(Cipher.DECRYPT_MODE, key);
			byte[] decodedValue = Base64.decodeBase64(encryptedData.getBytes());
			byte[] decValue = c.doFinal(decodedValue);
			return new String(decValue);
		}
		catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return encryptedData;
		}
	}

	/**
	 * Generate AES key.
	 *
	 * @param secret Secret use to generate AES Key.
	 * @return Generated key.
	 * @throws Exception
	 */
	private static Key generateAESKey(String secret) throws Exception {
		return new SecretKeySpec(secret.getBytes(), AES);
	}

	/**
	 * Build random string.
	 *
	 * @return Random string.
	 */
	public static String buildRandom() {
		SecureRandom random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}

	/**
	 * Build random string with only alpha numeric letters and with size of 32.
	 *
	 * @return Random string.
	 */
	public static String generateAlphaNumericRandom() {
		return generateAlphaNumericRandom(32);
	}

	/**
	 * Build random string with only alpha numeric letters and with size of 32.
	 *
	 * @param size Size of string.
	 * @return Random string.
	 */
	public static String generateAlphaNumericRandom(int size) {
		SecureRandom random = new SecureRandom();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < size; ++i) {
			int index = random.nextInt(alphaNumerics.length);
			sb.append(alphaNumerics[index]);
		}
		return sb.toString();
	}

	/**
	 * Encrypt string with specified algorithm.
	 *
	 * @param str String to encrypt.
	 * @param algorithm Algorithm.
	 * @return Encrypted string.
	 */
	private static String crypt(String str, String algorithm) {
		try {
			MessageDigest md = getInstance(algorithm);
			md.update(str.getBytes());

			byte byteData[] = md.digest();

			StringBuilder sb = new StringBuilder();
			for (byte aByteData : byteData) {
				sb.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
			}
			return sb.toString();
		}
		catch (NoSuchAlgorithmException ex) {
			log.error(ex.getMessage(), ex);
			return null;
		}
	}

	/**
	 * Decrypt string encrypted with AES encryption.
	 * String is first URL decoded using UTF-8 encoding and then decrypted using salt and secret.
	 *
	 * @param encrypted String to decrypt.
	 * @param salt Salt used to encrypt string.
	 * @param secret Secret used to encrypt string.
	 * @return Decrypted string.
	 */
	public static String decryptAES_UTF8(String encrypted, String salt, String secret) {
		try {
			String value = decode(encrypted, "UTF-8");
			String decrypted = decryptAES(value, secret);
			if (!decrypted.startsWith(salt)) {
				return null;
			}
			return decrypted.substring(salt.length());
		}
		catch (Exception ex) {
			log.debug(ex.getMessage(), ex);
			return encrypted;
		}
	}

	/**
	 * Decrypt string encrypted with AES encryption. <br />
	 * String is first URL decoded using UTF-8 encoding and then decrypted using secret.
	 *
	 * @param encrypted String to decrypt.
	 * @param secret Secret used to encrypt string.
	 * @return Decrypted string.
	 */
	public static String decryptAES_UTF8(String encrypted, String secret) {
		try {
			String value = decode(encrypted, "UTF-8");
			return decryptAES(value, secret);
		}
		catch (Exception ex) {
			log.debug(ex.getMessage(), ex);
			return encrypted;
		}
	}
}
