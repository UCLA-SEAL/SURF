import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestHolder;

/*
 * HISTORICAL:
 * Run the DSTC Certificate Authority console after installing the provider.
 * Install the provider here, rather than in the java.security file, since it
 * conflicts with the RSAJCA provider that comes with the JDK 1.3.
 */
/**
 * args must be one of two arguments:
 * 
 * -CA Generate Certificate Authority.
 * -CR Process Certification Requests.
 * 
 * @author peter
 */
public class CA {
    private static void generateCertificateAuthorityCerts() throws Exception{
        Properties p = readProperties();
        
        // Generate CA key pair
        KeyPairGenerator keyGen = null;
        String algorithm = p.getProperty("jcsi.ca.keyAlg", "DSA");
        int keyLen = Integer.parseInt(p.getProperty("jcsi.ca.keyLength", "512"));
        keyGen = KeyPairGenerator.getInstance(algorithm, "BC");
        SecureRandom random = new SecureRandom();
        keyGen.initialize(keyLen, random);
        KeyPair keys = keyGen.generateKeyPair();
        PublicKey publicKey = keys.getPublic();
        PrivateKey privKey = keys.getPrivate(); // The key used to sign our Certificate.
        
        String issuerDN = p.getProperty("jcsi.ca.issuerDN");
        long validDays 
          = Integer.parseInt(p.getProperty("jcsi.ca.validityPeriod"));
        String signerAlgorithm = p.getProperty("jcsi.ca.sigAlg", "SHA1withDSA");
        
        // Generate root certificate
        ContentSigner sigGen = new JcaContentSignerBuilder(signerAlgorithm).setProvider("BC").build(privKey);
        X500Principal issuer = new X500Principal(issuerDN);
        
        X500Principal subject = issuer; // Self signed.
        long time = System.currentTimeMillis();
        BigInteger serial = BigInteger.valueOf(time);
        Date notBefore = new Date(time - 50000);
        Date notAfter = new Date(time + validDays* 86400000L);
        Certificate rootCert = build(sigGen,issuer,serial, notBefore, notAfter, subject, publicKey);
        
        //Write Private key and Certificate to file.
        writePrivateKey(privKey, p, random);
        writeRootCertificate(rootCert, p);
   
    }

}