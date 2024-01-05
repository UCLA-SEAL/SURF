/*
 * Copyright (C) 2018 The DNA Authors
 * This file is part of The DNA library.
 *
 *  The DNA is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The DNA is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with The DNA.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.DNAProject.account;


import com.github.DNAProject.common.ErrorCode;
import com.github.DNAProject.crypto.*;
import com.github.DNAProject.common.Helper;
import com.github.DNAProject.common.Address;
import com.github.DNAProject.crypto.Base58;
import com.github.DNAProject.crypto.Signature;
import com.github.DNAProject.crypto.Digest;
import com.github.DNAProject.sdk.exception.SDKException;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.spec.SM2ParameterSpec;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.util.Strings;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;


public class Account {

    public static String getGcmDecodedPrivateKey(String encryptedPriKey, String passphrase,String address, byte[] salt, int n, SignatureScheme scheme) throws Exception {
        if (encryptedPriKey == null) {
            throw new SDKException(ErrorCode.EncryptedPriKeyError);
        }
        if (salt.length != 16) {
            throw new SDKException(ErrorCode.ParamError);
        }
        byte[] encryptedkey = new byte[]{};
        try{
            encryptedkey = Base64.getDecoder().decode(encryptedPriKey);
        }catch (Exception e){
            throw new SDKException(ErrorCode.ParamErr("encryptedPriKey is wrong"));
        }


        int N = n;
        int r = 8;
        int p = 8;
        int dkLen = 64;

        byte[] derivedkey = SCrypt.generate(passphrase.getBytes(StandardCharsets.UTF_8), salt, N, r, p, dkLen);
        byte[] derivedhalf2 = new byte[32];
        byte[] iv = new byte[12];
        System.arraycopy(derivedkey, 0, iv, 0, 12);
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32);

        byte[] rawkey = new byte[0];
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(derivedhalf2, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new GCMParameterSpec(128,iv));
            cipher.updateAAD(address.getBytes());
            rawkey = cipher.doFinal(encryptedkey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SDKException(ErrorCode.encryptedPriKeyAddressPasswordErr);
        }
        Account account = new Account(rawkey, scheme);
        if (!address.equals(account.getAddressU160().toBase58())) {
            throw new SDKException(ErrorCode.encryptedPriKeyAddressPasswordErr);
        }
        return Helper.toHexString(rawkey);
    }
}