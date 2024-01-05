package be.e_contract.mycarenet.certra;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.RSAKeyGenParameterSpec;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

public class CertRASession {

	public CertRASession(String emailPrivate, String phonePrivate) throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec params = new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4);

		SecureRandom secureRandom = new SecureRandom();
		secureRandom.setSeed(System.currentTimeMillis());

		keyPairGenerator.initialize(params, secureRandom);
		KeyPair keyPair = keyPairGenerator.genKeyPair();

		this.privateKey = keyPair.getPrivate();
		this.publicKey = keyPair.getPublic();

		this.emailPrivate = emailPrivate;
		this.phonePrivate = phonePrivate;
	}
}