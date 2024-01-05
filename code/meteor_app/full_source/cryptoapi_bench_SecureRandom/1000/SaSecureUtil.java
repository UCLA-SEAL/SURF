package cn.dev33.satoken.secure;

import cn.dev33.satoken.error.SaErrorCode;
import cn.dev33.satoken.exception.SaTokenException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

/**
 * Sa-Token 常见加密算法工具类
 *
 * @author click33
 * @since 1.14.0
 */
public class SaSecureUtil {
	
	public static HashMap<String, String> rsaGenerateKeyPair() throws Exception {

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("AES/ECB/PKCS5Padding");
		KeyPair keyPair;

		keyPairGenerator.initialize(KEY_SIZE,
				new SecureRandom(UUID.randomUUID().toString().replaceAll("-", "").getBytes()));
		keyPair = keyPairGenerator.generateKeyPair();

		RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
		RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();

		HashMap<String, String> map = new HashMap<>(16);
		map.put("private", encoder.encodeToString(rsaPrivateKey.getEncoded()));
		map.put("public", encoder.encodeToString(rsaPublicKey.getEncoded()));
		return map;
	}

}