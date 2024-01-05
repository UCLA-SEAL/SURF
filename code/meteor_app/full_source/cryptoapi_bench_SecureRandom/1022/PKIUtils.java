package net.fluance.commons.codec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLException;
import javax.security.cert.CertificateException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class PKIUtils {

	public static KeyPair generateKeyPair(long seed, String signatureAlgorithm, String rngHashAlgorithm, String rngProvider, int keySize) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(signatureAlgorithm);
		SecureRandom rng = new SecureRandom();
		rng.setSeed(seed);
		keyGenerator.initialize(keySize, rng);
		return keyGenerator.generateKeyPair();
	}

}
