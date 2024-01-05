/*******************************************************************************
 * Copyright (c) 2009, 2016 GreenVulcano ESB Open Source Project.
 * All rights reserved.
 *
 * This file is part of GreenVulcano ESB.
 *
 * GreenVulcano ESB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GreenVulcano ESB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GreenVulcano ESB. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package it.greenvulcano.util.crypto;

import it.greenvulcano.util.ArgsManager;
import it.greenvulcano.util.ArgsManagerException;
import it.greenvulcano.util.bin.BinaryUtils;
import it.greenvulcano.util.bin.Dump;
import it.greenvulcano.util.metadata.PropertiesHandler;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

/**
 * CryptoUtils class
 * 
 * @version 3.0.0 Feb 17, 2010
 * @author GreenVulcano Developer Team
 * 
 * 
 **/
public final class CryptoUtils {
    /**
     * Generate a key pair.
     * 
     * @param type
     * the algorithm type
     * @return the key pair
     * @throws CryptoUtilsException
     * if error occurs
     */
    public static KeyPair generateKeyPair(String type) throws CryptoUtilsException {

        String kType = getTypeI(type);
        int kSize = 0;
        if (kType.equals(DSA_TYPE)) {
            kSize = DSA_KEY_SIZE;
        } else if (kType.equals(RSA_TYPE)) {
            kSize = RSA_KEY_SIZE;
        } else {
            throw new CryptoUtilsException("Invalid algorithm : " + kType);
        }
        /*
         * Generate the key pair
         */
        KeyPairGenerator keyGen = null;
        SecureRandom random = null;
        try {
            keyGen = KeyPairGenerator.getInstance(kType);
            random = new SecureRandom();
        } catch (Exception exc) {
            throw new CryptoUtilsException("Error initializing KeyPairGenerator for key type '" + type + "'", exc);
        }
        random.setSeed(System.currentTimeMillis());
        keyGen.initialize(kSize, random);

        KeyPair sKey = keyGen.generateKeyPair();

        return sKey;
    }

}