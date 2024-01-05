package teammates.common.util;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.CharMatcher;

import teammates.common.exception.InvalidParametersException;

/**
 * Holds String-related helper functions.
 */

public final class StringHelper {
    private static final Logger log = Logger.getLogger();


    public static String decrypt(String message) throws InvalidParametersException {
        try {
            SecretKeySpec sks = new SecretKeySpec(hexStringToByteArray(Config.ENCRYPTION_KEY), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, sks);
            byte[] decrypted = cipher.doFinal(hexStringToByteArray(message));
            return new String(decrypted, Const.ENCODING);
        } catch (NumberFormatException | IllegalBlockSizeException | BadPaddingException e) {
            log.warning("Attempted to decrypt invalid ciphertext: " + message);
            throw new InvalidParametersException(e);
        } catch (Exception e) {
            assert false;
            return null;
        }
    }

}