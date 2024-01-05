package fr.jerep6.ogi.framework.security;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Cryp and decrypt data according to AES 128 bits.
 * Source insprired from http://www.code2learn.com/2011/06/encryption-and-decryption-of-data-using.html
 * 
 * To crypt in AES 256 download and install Java Cryptography Extension
 * http://deveshsharma.info/2012/10/09/fixing-java-security-invalidkeyexception-illegal-key-size-exception/
 * 
 * Why base64 ? :
 * The encrypted data returned by doFinal is binary, and so it cannot be printed (it'll appear as a bunch of gibberish.)
 * The Base64 encoding converts the binary to a set of ASCII characters, this makes it easily readable and also makes it
 * possible to use the encrypted data in situations where only plaintext data can be used.
 * 
 * The Base64 encoding doesn't add any extra encryption or security, it simply makes the encrypted data usable in
 * situations where you can't use binary.
 * 
 * @author jerep6 16 févr. 2014
 * 
 */
public class CryptageAES {
	private static final String	ALGO	= "AES";

	private final Key			key;

	public CryptageAES(String skey) {
		// Key size must be 16, 24 or 32 bytes
		// 16 bytes when no JCE
		String s = padRight(skey, 16);
		key = new SecretKeySpec(s.getBytes(), ALGO);
	}

	public String decrypt(String encryptedData) throws Exception {
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.DECRYPT_MODE, key);
		byte[] decordedValue = new BASE64Decoder().decodeBuffer(encryptedData);
		byte[] decValue = c.doFinal(decordedValue);
		String decryptedValue = new String(decValue);
		return decryptedValue;
	}

	public String encrypt(String Data) throws Exception {
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.ENCRYPT_MODE, key);
		byte[] encVal = c.doFinal(Data.getBytes());
		String encryptedValue = new BASE64Encoder().encode(encVal);
		return encryptedValue;
	}

	private String padLeft(String s, int n) {
		return String.format("%1$" + n + "s", s);
	}

	private String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}
}
