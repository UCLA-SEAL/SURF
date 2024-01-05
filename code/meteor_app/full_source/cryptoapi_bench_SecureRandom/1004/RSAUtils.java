package io.mycat.util;


import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import lombok.extern.slf4j.Slf4j;
import okio.ByteString;


public class RSAUtils {
  public static Map<Integer, String> genKeyPair()
      throws NoSuchAlgorithmException, UnsupportedEncodingException {
    // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
    // 初始化密钥对生成器，密钥大小为96-1024位
    keyPairGen.initialize(1024, new SecureRandom());
    // 生成一个密钥对，保存在keyPair中
    KeyPair keyPair = keyPairGen.generateKeyPair();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate(); // 得到私钥
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic(); // 得到公钥
    String publicKeyString = encodeBase64(publicKey.getEncoded());
    // 得到私钥字符串
    String privateKeyString = encodeBase64((privateKey.getEncoded()));
    Map<Integer, String> keyMap = new HashMap<>(); // 用于封装随机产生的公钥与私钥
    // 将公钥和私钥保存到Map
    keyMap.put(0, publicKeyString); // 0表示公钥
    keyMap.put(1, privateKeyString); // 1表示私钥
    return keyMap;
  }
}