package pers.acp.tools.security.key;

import pers.acp.tools.file.common.FileCommon;
import pers.acp.tools.security.key.enums.KeyType;
import pers.acp.tools.security.key.enums.StorageMode;
import pers.acp.tools.security.key.factory.IStorageFactory;
import pers.acp.tools.utility.CommonUtility;
import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class KeyManagement {
   public static Object[] getDSAKeys() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DSA");
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.setSeed(getRandomString(0, 32).getBytes(encode));
        keyPairGen.initialize(1024, secureRandom);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        DSAPublicKey publicKey = (DSAPublicKey) keyPair.getPublic();
        DSAPrivateKey privateKey = (DSAPrivateKey) keyPair.getPrivate();
        Object[] keys = new Object[2];
        keys[0] = publicKey;
        keys[1] = privateKey;
        return keys;
    }

}