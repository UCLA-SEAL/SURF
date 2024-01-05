package crypt;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * AES-GCMを用いた暗号化/復号処理を行うクラス
 *
 * @author いっぺー
 */
public class AES {
    private static final int tagLen = 16;

    /**
     * 暗号化/復号処理
     *
     * <p>暗号化および復号の処理は、変数opmodeによって切り替えられる。
     * opmodeをCipher.ENCRYPT_MODEとすることで暗号化、Cipher.DECRYPT_MODEで復号となる。
     *
     * <p>鍵長は256ビットのみ受け付けるため、SHA256.hashTextを用いてmasterKeyのハッシュ化を行った上で利用すること。
     *
     * @param opmode    暗号化/復号モードの指定
     * @param bytesText 平文および暗号文
     * @param bytesKey  256ビットのmasterKey
     * @param iv        初期化ベクトル
     * @return byte[] 暗号化/復号の結果
     * @author いっぺー
     * @see SHA256
     */
    public static byte[] EnDeCrypt(int opmode, byte[] bytesText, byte[] bytesKey, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(bytesKey, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(tagLen * 8, iv);
            cipher.init(opmode, keySpec, gcmParameterSpec);
            return cipher.doFinal(bytesText);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }
}