package com.bt.pi.app.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.Locale;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.springframework.stereotype.Component;


public class SecurityUtils {
    public KeyPair getNewKeyPair(String keyAlgorithm, int keySize) throws GeneralSecurityException {
        LOG.debug(String.format("getNewKeyPair(%s, %d)", keyAlgorithm, keySize));
        KeyPairGenerator keyGen = null;
        keyGen = KeyPairGenerator.getInstance(keyAlgorithm);
        SecureRandom random = new SecureRandom();
        random.setSeed(System.currentTimeMillis());
        keyGen.initialize(keySize, random);
        KeyPair keyPair = keyGen.generateKeyPair();
        return keyPair;
    }
}