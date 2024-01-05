
package hyper.keygen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.ws.commons.util.Base64;
import org.cornell.hyper.overlay.MovieEntry.TimedPublicKey;

// TODO: Add method to generate TimedPublicKey and its associate private key to a 
// file given a expiry time and the master private key.
// 
// Modify the crawler so that it reads the TimedPublicKey/private key from a file
// instead of generating it on the fly.
public class Generator {
	public static List<Object> GeneratePublisherKey(long timeoutMS) {
		byte[] rawPrivKey = MovieAdder.getMasterPrivate();
		if (rawPrivKey == null) {
			System.err.println("Cannot retrieve private key, exiting");
			return null;
		}
		PKCS8EncodedKeySpec privKeySpec = 
			new PKCS8EncodedKeySpec(rawPrivKey);
		PrivateKey masterPrivKey = null;
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(MovieAdder.defaultKeyInst);
			masterPrivKey = keyFactory.generatePrivate(privKeySpec);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Cannot parse private key, exiting");
			e.printStackTrace();
			return null;
		} catch (InvalidKeySpecException e) {
			System.err.println("Cannot parse private key, exiting");
			e.printStackTrace();
			return null;
		}
					
		// Generate new public/private key pair
		KeyPairGenerator keyGen = null;
		try {
			keyGen = KeyPairGenerator.getInstance("DSA");
			SecureRandom random = new SecureRandom();
			keyGen.initialize(1024, random);			
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Cannot generate keypair, exiting");
			e.printStackTrace();
			return null;
		} catch (NoSuchProviderException e) {
			System.err.println("Cannot generate keypair, exiting");
			e.printStackTrace();
			return null;
		}		
		KeyPair pair = keyGen.generateKeyPair();
		PrivateKey prodPriv = pair.getPrivate();
		PublicKey prodPub = pair.getPublic();
		
		// Have a timeout of 30 days
		//long timeoutMS = 30L * 24L * 60L * 60L * 1000L;
		//System.out.println("Timeout in MS is: " + timeoutMS);
		Date dueDate = new Date(System.currentTimeMillis() + timeoutMS);
		//System.out.println("Due dates is: " + dueDate.getTime());
		//System.out.println("Cur time is: " + new Date().getTime());
		TimedPublicKey timedKey = new TimedPublicKey(prodPub.getEncoded(), dueDate);
				
		// Generate signature for timedKey
		byte[] prodSig = null;
		try {
			Signature dsa = Signature.getInstance("SHA1withDSA");
			dsa.initSign(masterPrivKey);
			/* Update and sign the data */
			dsa.update(timedKey.toByteArray());
			prodSig = dsa.sign();			
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Cannot generate signature for timed key, exiting");
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			System.err.println("Cannot generate signature for timed key, exiting");
			e.printStackTrace();
			return null;
		} catch (SignatureException e) {
			System.err.println("Cannot generate signature for timed key, exiting");
			e.printStackTrace();
			return null;
		}		
		List<Object> keyList = new ArrayList<Object>();		
		keyList.add(prodSig);
		keyList.add(timedKey);
		keyList.add(prodPriv);	
		return keyList;
	}
}