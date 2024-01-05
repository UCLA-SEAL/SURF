package test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.util.encoders.Base64;

public class SuperSecureSocketTest {
	
	public byte[] encrypt(byte[] input, String algorithm, Key key) {
		byte[] output = null;
		Cipher crypt;
		try {
			crypt = Cipher.getInstance(algorithm);
			crypt.init(Cipher.ENCRYPT_MODE, key);
			output = crypt.doFinal(input);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("ERROR: Algorithm unknown!");
		} catch (NoSuchPaddingException e) {
			System.err.println("ERROR: No padding!");
		} catch (IllegalBlockSizeException e) {
			System.err.println("ERROR: Illegal block size!");
		} catch (BadPaddingException e) {
			System.err.println("ERROR: Bad padding!");
		} catch (InvalidKeyException e) {
			System.err.println("ERROR: Invalid key!");
		}
		return output;
	}

	public byte[] decrypt(byte[] input, String algorithm, Key key) {
		byte[] output = null;
		Cipher crypt;
		try {
			crypt = Cipher.getInstance(algorithm);
			crypt.init(Cipher.DECRYPT_MODE, key);
			output = crypt.doFinal(input);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("ERROR: Algorithm unknown!");
		} catch (NoSuchPaddingException e) {
			System.err.println("ERROR: No padding!");
		} catch (IllegalBlockSizeException e) {
			System.err.println("ERROR: Illegal block size!");
		} catch (BadPaddingException e) {
			System.err.println("ERROR: Bad padding!");
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			System.err.println("ERROR: Invalid key!");
		}
		return output;
	}

	public byte[] encrypt(byte[] input, String algorithm, Key key, IvParameterSpec iv) {
		byte[] output = null;
		Cipher crypt;
		try {
			crypt = Cipher.getInstance(algorithm);
			crypt.init(Cipher.ENCRYPT_MODE, key, iv);
			output = crypt.doFinal(input);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("ERROR: Algorithm unknown!");
		} catch (NoSuchPaddingException e) {
			System.err.println("ERROR: No padding!");
		} catch (IllegalBlockSizeException e) {
			System.err.println("ERROR: Illegal block size!");
		} catch (BadPaddingException e) {
			System.err.println("ERROR: Bad padding!");
		} catch (InvalidKeyException e) {
			System.err.println("ERROR: Invalid key!");
		} catch (InvalidAlgorithmParameterException e) {
			System.err.println("ERROR: Invalid initialization vector!");
		}
		return output;
	}

	public byte[] decrypt(byte[] input, String algorithm, Key key, IvParameterSpec iv) {
		byte[] output = null;
		Cipher crypt;
		try {
			crypt = Cipher.getInstance(algorithm);
			crypt.init(Cipher.DECRYPT_MODE, key, iv);
			output = crypt.doFinal(input);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("ERROR: Algorithm unknown!");
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			System.err.println("ERROR: No padding!");
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			System.err.println("ERROR: Illegal block size!");
			e.printStackTrace();
		} catch (BadPaddingException e) {
			System.err.println("ERROR: Bad padding!");
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			System.err.println("ERROR: Invalid key!");
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			System.err.println("ERROR: Invalid initialization vector!");
			e.printStackTrace();
		}
		return output;
	}

	public byte[] generateSecureRandomNumber(int size) {
		SecureRandom secureRandom = new SecureRandom(); 
		final byte[] number = new byte[32];
		secureRandom.nextBytes(number);
		return number;
	}

	public PrivateKey getPEMPrivateKey(String pathToPrivateKey) {
		PEMReader in;
		PrivateKey privateKey = null;
		try {
			in = new PEMReader(new FileReader(pathToPrivateKey), new PasswordFinder() {
				@Override
				public char[] getPassword() {
					char[] privK = null;
					System.out.println("Enter pass phrase:");
					try {
						privK = new BufferedReader(new InputStreamReader(System.in)).readLine().toCharArray() ;
					} catch (IOException e) {
						System.err.println("Couldn't read password!");
					}
					return privK;
				}
			});
			KeyPair keyPair = (KeyPair) in.readObject(); 
			privateKey = keyPair.getPrivate();
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't find private key!");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Couldn't read Private Key!");
			e.printStackTrace();
		}
		return privateKey;
	}

	public PublicKey getPEMPublicKey(String pathToPublicKey) {
		PEMReader in;
		PublicKey publicKey = null;
		try {
			in = new PEMReader(new FileReader(pathToPublicKey));
			publicKey = (PublicKey) in.readObject();
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't find public key!");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Couldn't read public key!");
			e.printStackTrace();
		}
		return publicKey;
	}

	public SecretKey generateAESKey(int keySize) {
		KeyGenerator generator = null;
		try {
			generator = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Algorithm is unknown!");
			e.printStackTrace();
		} 
		generator.init(keySize); 
		SecretKey key = generator.generateKey();
		return key;
	}

	public static void main(String[] args) throws UnsupportedEncodingException
	{
		System.out.println(Charset.defaultCharset());
		SuperSecureSocketTest sss = new SuperSecureSocketTest();
		String message1 = new String("!login alice 10050");
		byte[] clientChallenge = sss.generateSecureRandomNumber(32);
		byte[] base64Challenge = Base64.encode(clientChallenge);
		base64Challenge = Base64.encode(sss.encrypt(base64Challenge, "RSA/NONE/OAEPWithSHA256AndMGF1Padding", sss.getPEMPublicKey("/Users/rene/Documents/workspace/DS_Lab1/keys/auction-server.pub.pem")));
		//message1 += " " + base64Challenge;
		String lulu = message1 + " " + new String(base64Challenge);
		//String firstMessage = new String(message1);
		//String firstMessage = new String(lulu);
		//final String B64 = "a-zA-Z0-9/+";
		//assert firstMessage.matches("!login [a-zA-Z0-9_\\-]+ [0-9]+ ["+B64+"]{43}=") : "1st message";
		byte[] base64message = Base64.encode(lulu.getBytes());
		byte[] message2 = Base64.decode(base64message);
		String message2S = new String(message2, Charset.defaultCharset());
		if (message2S.startsWith("!login")) {
			System.out.println("!login");
		}
		byte[] message3 = Base64.decode(message2S.split("\\s+")[3]);		
		byte[] message3Decrypted = sss.decrypt(message3, "RSA/NONE/OAEPWithSHA256AndMGF1Padding", sss.getPEMPrivateKey("/Users/rene/Documents/workspace/DS_Lab1/keys/auction-server.pem"));
		if (new String(Base64.decode(message3Decrypted)).equals(new String(clientChallenge))) {
			System.out.println("Challenge MATCH!");
		}
	}
}