/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkalex.pay;

import java.io.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.redkale.annotation.AutoLoad;
import org.redkale.annotation.Comment;
import org.redkale.annotation.*;
import org.redkale.annotation.ResourceListener;
import org.redkale.convert.json.JsonConvert;
import org.redkale.service.Local;
import org.redkale.util.*;
import static org.redkalex.pay.PayRetCodes.*;
import static org.redkalex.pay.Pays.*;

/**
 *
 * 详情见: https://redkale.org
 *
 * @author zhangjx
 */
public class WeiXinPayService {

   
    protected static String decryptToString(WeixinPayElement element, String associated_data, String nonce, String ciphertext) throws Exception {
        SecretKeySpec key = new SecretKeySpec(element.apiv3key.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        cipher.updateAAD(associated_data.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
    }

}
