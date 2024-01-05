
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

# https://github.com/rodbate/bouncycastle-examples/blob/2ccf1030a741fdf0d7dcd94ec0e15afc8c525812/src/main/java/bcfipsin100/base/Aes.java#L151
public class Aes
{

    public static Object[] aeadEncrypt(SecretKey key, byte[] data, byte[] associatedData)
        throws GeneralSecurityException
    {
        Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding", "BCFIPS");

        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, Hex.decode("000102030405060708090a0b")));

        cipher.updateAAD(associatedData);

        return new Object[] { cipher.getParameters(), cipher.doFinal(data) };
    }
}
