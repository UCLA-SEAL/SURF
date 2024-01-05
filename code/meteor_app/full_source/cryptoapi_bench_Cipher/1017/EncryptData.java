package com.netease.qa.emmagee.utils;

import java.security.Key;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * 提供加密算法，可以对输入的字符串进行加密、解密操作
 * 
 * @author andrewleo
 */
public class EncryptData {
    private static String strDefaultKey = "emmagee";

	private Cipher encryptCipher = null;

	private Cipher decryptCipher = null;

    public EncryptData(String strKey) {
		try {
			Key key = getKey(strKey.getBytes());

			encryptCipher = Cipher.getInstance("DES");
			encryptCipher.init(Cipher.ENCRYPT_MODE, key);

			decryptCipher = Cipher.getInstance("DES");
			decryptCipher.init(Cipher.DECRYPT_MODE, key);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}