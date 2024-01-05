/*******************************************************************************
 * Copyright (c) May 22, 2011 NetXForge.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU Lesser General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * Contributors:
 *    Christophe Bouhier - initial API and implementation and/or initial documentation
 *******************************************************************************/
package com.netxforge.base.security;

import java.io.IOException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 * @author Christophe Bouhier christophe.bouhier@netxforge.com
 */
public class JCAService implements IJCAService {

	// See:
	// http://download.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html

	// "algorithm/mode/padding"
	// We use DES here.
	private static final String CIPHER_TYPE = "DES/ECB/PKCS5Padding";

	/* (non-Javadoc)
	 * @see com.netxforge.netxstudio.common.IJCAService#encrypt(java.lang.String, java.security.Key)
	 */
	public String encrypt(String password, Key key) {
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] outputBytes = cipher.doFinal(password.getBytes());
			return byteArrayToHexString(outputBytes);
		} catch (Exception e) {
			throw new RuntimeException("Failed to encrypt password", e);
		}
	}
	
	public String digest(String password){
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(password.getBytes());
			byte[] outputBytes = md.digest();
			return byteArrayToHexString(outputBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Failed to digest password", e);
		}
		
	}
	

	/* (non-Javadoc)
	 * @see com.netxforge.netxstudio.common.IJCAService#decrypt(java.lang.String, java.security.Key)
	 */
	public String decrypt(String password, Key key) {
		try {
			byte encrypted[] = this.hexStringToByteArray(password);
			Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] outputBytes = cipher.doFinal(encrypted);
			String ret = new String(outputBytes);
			return ret;
		} catch (Exception e) {
			throw new RuntimeException("Failed to decrypt password", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.netxforge.netxstudio.common.IJCAService#generateRandomKey()
	 */
	public Key generateRandomKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("DES");
		keyGenerator.init(new SecureRandom());
		SecretKey secretKey = keyGenerator.generateKey();
		return secretKey;
	}

	/* (non-Javadoc)
	 * @see com.netxforge.netxstudio.common.IJCAService#encodeKey(java.security.Key)
	 */
	public String encodeKey(Key key) {
		return this.byteArrayToHexString(key.getEncoded());
	}

	/* (non-Javadoc)
	 * @see com.netxforge.netxstudio.common.IJCAService#decodeKey(java.lang.String)
	 */
	public Key decodeKey(String encodedKey) throws IOException {
		byte raw[] = this.hexStringToByteArray(encodedKey);
		SecretKey key = new SecretKeySpec(raw, "DES");
		return key;
	}
	
	
	/**
	 * A Byte Array to a Hexadecimal string.
	 * @param b
	 * @return
	 */
	private String byteArrayToHexString(byte[] b) {
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
	}
	
	/**
	 * An Hexadecimal string to A Byte Array.
	 */
	private byte[] hexStringToByteArray(String s) {
		byte[] b = new byte[s.length() / 2];
		for (int i = 0; i < b.length; i++) {
			int index = i * 2;
			int v = Integer.parseInt(s.substring(index, index + 2), 16);
			b[i] = (byte) v;
		}
		return b;
	}

}
