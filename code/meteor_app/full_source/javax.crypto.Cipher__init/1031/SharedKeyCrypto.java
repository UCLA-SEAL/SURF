package crypto;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

import shared.ProjectConfig;

public class SharedKeyCrypto {
	
	private static final boolean DEBUG = ProjectConfig.DEBUG;
	
	private static final int IV_STRING_LENGTH = 12;
	
	private static Key key = null;
	
	public static boolean initSharedKeyCrypto(byte[] key_string) {
		try {
			key = unwrapKey(key_string);
		} catch (InvalidKeyException e) {
			if (DEBUG) e.printStackTrace();
			return false;
		} catch (InvalidKeySpecException e) {
			if (DEBUG) e.printStackTrace();
			return false;
		}
		String testString = "Hello World!";
		String result = "";
		Cipher c;
		try {
			c = Cipher.getInstance("DES");
			c.init(Cipher.ENCRYPT_MODE, key);
			byte[] ciphertext = c.doFinal(testString.getBytes());
			c.init(Cipher.DECRYPT_MODE, key);
			result = new String(c.doFinal(ciphertext), "UTF8");
		}
		catch (Exception e) {
			if (DEBUG) e.printStackTrace();
			return false;
		}
		return testString.equals(result);
	}
	
	public static String generateKey() throws InvalidKeySpecException {
		try {
			KeyGenerator generator = KeyGenerator.getInstance("DES");
			generator.init(new SecureRandom());
			SecretKey key = generator.generateKey();
			
			SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
			DESKeySpec keySpec = (DESKeySpec) factory.getKeySpec(key, DESKeySpec.class);
			byte[] wrappedKey = keySpec.getKey();
			
			return encode(wrappedKey);
		} catch (NoSuchAlgorithmException e1) {
			if (DEBUG) e1.printStackTrace();
		}
		return null;
	}
	
	public static SecretKey unwrapKey(byte[] wrappedKey) throws InvalidKeyException, InvalidKeySpecException {
		try {
			DESKeySpec keySpec = new DESKeySpec(wrappedKey);
			SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
			SecretKey key = factory.generateSecret(keySpec);
			return key;
		} catch (NoSuchAlgorithmException e) {
			if (DEBUG) e.printStackTrace();
		}
		return null;
	}
	
	public static String encrypt(String txt) {
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			String iv = encode(cipher.getIV());
			byte[] encrypted = cipher.doFinal(txt.getBytes("UTF8"));
			return iv+encode(encrypted);
		} catch (BadPaddingException e) {
			if (DEBUG) e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			if (DEBUG) e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			if (DEBUG) e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			if (DEBUG) e.printStackTrace();
		} catch (InvalidKeyException e) {
			if (DEBUG) e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			if (DEBUG) e.printStackTrace();
		}
		return null;
	}
	
	public static String decrypt(String secret) {
		Cipher cipher;
		try {
			byte[] iv = decode(secret.substring(0, IV_STRING_LENGTH));
			cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			IvParameterSpec ivSpec = new IvParameterSpec(iv);
			cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
			
			byte[] byteString = cipher.doFinal(decode(secret.substring(IV_STRING_LENGTH)));

			return new String(byteString, "UTF8");
		} catch (UnsupportedEncodingException e) {
			if (DEBUG) e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			if (DEBUG) e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			if (DEBUG) e.printStackTrace();
		} catch (BadPaddingException e) {
			if (DEBUG) e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			if (DEBUG) e.printStackTrace();
		} catch (InvalidKeyException e) {
			if (DEBUG) e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			if (DEBUG) e.printStackTrace();
		}
		return null;
	}
	
	private static String encode(byte[] bytes) {
		return CryptoUtil.encode(bytes);
	}
	
	private static byte[] decode(String str) {
		return CryptoUtil.decode(str);
	}
}
