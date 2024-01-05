package info.guardianproject.pixelknot.crypto;

import android.util.Base64;
import android.util.Log;

import info.guardianproject.pixelknot.Constants.Logger;

import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Aes {
	
	public static String DecryptWithPassword(String password, byte[] iv, byte[] message, byte[] salt) {

		initIV();

		String new_message = null;
		
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey secret_key = new SecretKeySpec(tmp.getEncoded(), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			IvParameterSpec ivSpec = new IvParameterSpec(iv);
			cipher.init(Cipher.DECRYPT_MODE, secret_key, ivSpec);
			
			new_message = new String(cipher.doFinal(message));
			
		} catch (IllegalBlockSizeException e) {
			Log.e(Logger.UI, e.toString());
			e.printStackTrace();
		} catch (BadPaddingException e) {
			Log.e(Logger.UI, e.toString());
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			Log.e(Logger.UI, e.toString());
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			Log.e(Logger.UI, e.toString());
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			Log.e(Logger.UI, e.toString());
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			Log.e(Logger.UI, e.toString());
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			Log.e(Logger.UI, e.toString());
			e.printStackTrace();
		}
		
		return new_message;
	}
	
}