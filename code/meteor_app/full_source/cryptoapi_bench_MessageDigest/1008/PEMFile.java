/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.net.jsse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.tomcat.util.buf.Asn1Parser;
import org.apache.tomcat.util.buf.Asn1Writer;
import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.tomcat.util.file.ConfigFileLoader;
import org.apache.tomcat.util.res.StringManager;

/**
 * RFC 1421 PEM file containing X509 certificates or private keys.
 */
public class PEMFile {

    private static final StringManager sm = StringManager.getManager(PEMFile.class);

    private static final byte[] OID_EC_PUBLIC_KEY =
            new byte[] { 0x06, 0x07, 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x02, 0x01 };

    private static final String PBES2 = "PBES2";



    private byte[] deriveKey(int keyLength, String password, byte[] iv) throws NoSuchAlgorithmException {
        // PBKDF1-MD5 as specified by PKCS#5
        byte[] key = new byte[keyLength];

        int insertPosition = 0;

        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] pw = password.getBytes(StandardCharsets.UTF_8);

        while (insertPosition < keyLength) {
            digest.update(pw);
            digest.update(iv, 0, 8);
            byte[] round = digest.digest();
            digest.update(round);

            System.arraycopy(round, 0, key, insertPosition, Math.min(keyLength - insertPosition, round.length));
            insertPosition += round.length;
        }

        return key;
    }



    private enum Format {
        PKCS1,
        PKCS8,
        RFC5915
    }
}