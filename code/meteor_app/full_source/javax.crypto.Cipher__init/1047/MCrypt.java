package uk.co.thomasc.codmw.util;

import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MCrypt {

	public static String key = "lKgTKRtEUfi6iWX13T";
	private String iv = "LW){FtI)}x5C.~S+";
	private IvParameterSpec ivspec;
	private SecretKeySpec keyspec;
	private Cipher cipher;
	private String SecretKey = "T%EA,cU&}xt&^$8e";


	public MCrypt() {
		this.ivspec = new IvParameterSpec(this.iv.getBytes());
		this.keyspec = new SecretKeySpec(this.SecretKey.getBytes(), "AES");

		try {
			this.cipher = Cipher.getInstance("AES/CBC/NoPadding");
		} catch (NoSuchAlgorithmException var2) {
			var2.printStackTrace();
		} catch (NoSuchPaddingException var3) {
			var3.printStackTrace();
		}

	}

	public byte[] encrypt(String text) throws Exception {
		if(text != null && text.length() != 0) {
			byte[] encrypted = (byte[])null;

			try {
				this.cipher.init(1, this.keyspec, this.ivspec);
				encrypted = this.cipher.doFinal(padString(text).getBytes());
				return encrypted;
			} catch (Exception var4) {
				throw new Exception("[encrypt] " + var4.getMessage());
			}
		} else {
			throw new Exception("Empty string");
		}
	}

	public byte[] decrypt(String code) throws Exception {
		if(code != null && code.length() != 0) {
			byte[] decrypted = (byte[])null;

			try {
				this.cipher.init(2, this.keyspec, this.ivspec);
				decrypted = this.cipher.doFinal(hexToBytes(code.toCharArray()));
				return decrypted;
			} catch (Exception var4) {
				throw new Exception("[decrypt] " + var4.getMessage());
			}
		} else {
			throw new Exception("Empty string");
		}
	}

	public static String bytesToHex(byte[] data) {
		if(data == null) {
			return null;
		} else {
			int len = data.length;
			String str = "";

			for(int i = 0; i < len; ++i) {
				if((data[i] & 255) < 16) {
					str = str + "0" + Integer.toHexString(data[i] & 255);
				} else {
					str = str + Integer.toHexString(data[i] & 255);
				}
			}

			return str;
		}
	}

	public static byte[] hexToBytes(char[] hex) {
		int length = hex.length / 2;
		byte[] raw = new byte[length];

		for(int i = 0; i < length; ++i) {
			int high = Character.digit(hex[i * 2], 16);
			int low = Character.digit(hex[i * 2 + 1], 16);
			int value = high << 4 | low;
			if(value > 127) {
				value -= 256;
			}

			raw[i] = (byte)value;
		}

		return raw;
	}

	private static String padString(String source) {
		char paddingChar = 32;
		byte size = 16;
		int x = source.length() % size;
		int padLength = size - x;

		for(int i = 0; i < padLength; ++i) {
			source = source + paddingChar;
		}

		return source;
	}
}
