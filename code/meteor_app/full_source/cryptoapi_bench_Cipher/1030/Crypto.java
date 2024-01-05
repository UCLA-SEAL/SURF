package webtend.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import webtend.exceptions.UnexpectedException;

/**
 * Cryptography utils
 */
public class Crypto {

public static String encryptAES(String value, String privateKey) {
		try {
			byte[] raw = privateKey.getBytes();
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			return Codec.byteToHexString(cipher.doFinal(value.getBytes()));
		} catch (Exception ex) {
			throw new UnexpectedException(ex);
		}
	}



public static String decryptAES(String value, String privateKey) {
		try {
			byte[] raw = privateKey.getBytes();
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec);
			return new String(cipher.doFinal(Codec.hexStringToByte(value)));
		} catch (Exception ex) {
			throw new UnexpectedException(ex);
		}
	}

}
