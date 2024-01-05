package Null_Password;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Null_Password {

	static final Logger log = Logger.getLogger("logger");
	
	private String password; // bad  null密码
	
	public String bad() {
		
		password = null; 
		
		return password;
	}

	public String good(String sKey) throws UnsupportedEncodingException, NoSuchProviderException
    {
        String data = "key"; /* init data */
        
		//String sKey = "Skey";
		Cipher cipher = null;
		String pw = "";
		try {
			SecretKeySpec key = new SecretKeySpec(sKey.getBytes(), "AES");
			cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key);
		}catch (NoSuchAlgorithmException e) {
			log.info("error");
		} catch (InvalidKeyException e) {
			log.info("InvalidKeyException");
		} catch (NoSuchPaddingException e) {
			log.info("error");
		}
			
		try {
			if(cipher != null){
				pw = new String(cipher.doFinal(data.getBytes()),StandardCharsets.UTF_8);
			}
			
		} catch (IllegalBlockSizeException e) {
			log.info("error");
		} catch (BadPaddingException e) {
			log.info("error");
		}

		String password = pw;  // good null密码
		
		return password;
		       
    }
	
}
