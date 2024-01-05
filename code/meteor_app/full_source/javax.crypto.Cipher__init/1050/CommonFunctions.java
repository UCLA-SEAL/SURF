/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package RMIChatServer.CommonFunctions;

import RMIChatServer.Benutzer.MyUser;
import RMIChatServer.Exception.InternalServerErrorException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Die Klasse umfasst eine Reihe an Funktionen, die auf dem Server und dem
 * Client gebraucht werden.
 *
 * @author Pascal
 */
public class CommonFunctions {

    private Cipher AESCipher;
    private Cipher RSACipher;
    private MessageDigest MD5;

    public CommonFunctions() {
        try {
            //Verschlüsslungs-Objekte anlegen
            AESCipher = Cipher.getInstance("AES");
            RSACipher = Cipher.getInstance("RSA");
            //Hash-Objekt anlegen
            MD5 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CommonFunctions.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchPaddingException ex) {
            Logger.getLogger(CommonFunctions.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Die Funktion generiert aus einem Byte-Array einen PrivateKey.
     *
     * @param key Das Byte-Array, das zu einem PublicKey umgewandelt werden
     * soll.
     * @return Die Funktion gibt einen PublicKey zurück.
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @author Pascal Lacmann
     */
    public PublicKey byteToPublicKey(byte[] key) throws InternalServerErrorException {
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
            return publicKey;
        } catch (NoSuchAlgorithmException ex) {
            throw new InternalServerErrorException("NoSuchAlgorithmException: " + ex.getMessage());
        } catch (InvalidKeySpecException ex) {
            throw new InternalServerErrorException("InvalidKeySpecException: " + ex.getMessage());
        }
    }

    /**
     * Die Funktion generiert aus einem Byte-Array einen PrivateKey.
     *
     * @param key Das Byte-Array, das zu einem PrivateKey umgewandelt werden
     * soll.
     * @return Die Funktion gibt einen PrivateKey zurück.
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @author Pascal Lacmann
     */
    public PrivateKey byteToPrivateKey(byte[] key) throws InternalServerErrorException {
        try {
            PrivateKey PrivateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key));
            return PrivateKey;
        } catch (NoSuchAlgorithmException ex) {
            throw new InternalServerErrorException("NoSuchAlgorithmException: " + ex.getMessage());
        } catch (InvalidKeySpecException ex) {
            throw new InternalServerErrorException("InvalidKeySpecException: " + ex.getMessage());
        }

    }

    /**
     * Diese Funktion VERschlüsselt ein Byte-Array.
     *
     * @param code Das ist der zu verschlüsselnde Text.
     * @param key Mit dem symmetrischen Key wird der Code verschlüsselt.
     * @return Die Funktion gibt ein byte-Array zurück.
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @author Pascal Lacmann
     */
    public byte[] AESEncrypt(byte[] code, Key key) throws InternalServerErrorException {
        try {
            AESCipher.init(Cipher.ENCRYPT_MODE, key);
            return AESCipher.doFinal(code);
        } catch (InvalidKeyException ex) {
            throw new InternalServerErrorException("InvalidKeySpecException: " + ex.getMessage());
        } catch (IllegalBlockSizeException ex) {
            throw new InternalServerErrorException("IllegalBlockSizeException: " + ex.getMessage());
        } catch (BadPaddingException ex) {
            throw new InternalServerErrorException("BadPaddingException: " + ex.getMessage());
        }
    }

    /**
     * Diese Funktion ENTschlüsselt ein Byte-Array. Methode: AES
     *
     * @param code Das ist der zu entschlüsselnde Text.
     * @param key Mit dem symmetrischen Key wird der Code entschlüsselt.
     * @return Die Funktion gibt ein byte-Array zurück.
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @author Pascal Lacmann
     */
    public byte[] AESDecrypt(byte[] code, Key key) throws InternalServerErrorException {
        try {
            AESCipher.init(Cipher.DECRYPT_MODE, key);
            return AESCipher.doFinal(code);
        } catch (IllegalBlockSizeException ex) {
            throw new InternalServerErrorException("IllegalBlockSizeException: " + ex.getMessage());
        } catch (BadPaddingException ex) {
            throw new InternalServerErrorException("BadPaddingException: " + ex.getMessage());
        } catch (InvalidKeyException ex) {
            throw new InternalServerErrorException("InvalidKeyException: " + ex.getMessage());
        }
    }

    /**
     * Diese Funktion VERschlüsselt ein Byte-Array. Methode: RSA
     *
     * @param code Das ist der zu verschlüsselnde Text.
     * @param key Mit dem PublicKey wird der Code verschlüsselt.
     * @return Die Funktion gibt ein byte-Array zurück.
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @author Pascal Lacmann
     */
    public byte[] RSAEncrypt(byte[] code, PublicKey key) throws InternalServerErrorException {
        try {
            RSACipher.init(Cipher.ENCRYPT_MODE, key);
            return RSACipher.doFinal(code);
        } catch (IllegalBlockSizeException ex) {
            throw new InternalServerErrorException("IllegalBlockSizeException: " + ex.getMessage());
        } catch (BadPaddingException ex) {
            throw new InternalServerErrorException("BadPaddingException: " + ex.getMessage());
        } catch (InvalidKeyException ex) {
            throw new InternalServerErrorException("InvalidKeyException: " + ex.getMessage());
        }
    }

    /**
     * Diese Funktion ENTschlüsselt ein Byte-Array. Methode: RSA
     *
     * @param code Das ist der zu entschlüsselnde Text.
     * @param key Mit dem PrivateKey wird der Code entschlüsselt.
     * @return Die Funktion gibt ein byte-Array zurück.
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @author Pascal Lacmann
     */
    public byte[] RSADecrypt(byte[] code, PrivateKey key) throws InternalServerErrorException {
        try {
            RSACipher.init(Cipher.DECRYPT_MODE, key);
            return RSACipher.doFinal(code);
        } catch (IllegalBlockSizeException ex) {
            throw new InternalServerErrorException("IllegalBlockSizeException: " + ex.getMessage());
        } catch (BadPaddingException ex) {
            throw new InternalServerErrorException("BadPaddingException: " + ex.getMessage());
        } catch (InvalidKeyException ex) {
            throw new InternalServerErrorException("InvalidKeyException: " + ex.getMessage());
        }
    }

    /**
     * Die Funktion wandelt ein Byte-Array in einen String um.
     *
     * @param input Ein Byte-Array, das in einen String umgewandelt werden soll.
     * @return Die Funktion gibt einen String zurück.
     * @author Pascal Lacmann
     */
    public String byteToString(byte[] input) {
        String returnString = null;
        try {
            returnString = new String(input, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(CommonFunctions.class.getName()).log(Level.SEVERE, null, ex);
        }
        return returnString;
    }

    /**
     * Die Funktion wandelt einen String in ein Byte-Array um.
     *
     * @param s Ein String, der zu einem Byte-Array umgewandelt werden soll.
     * @return Es wird ein Byte-Array zurück gegeben.
     * @author Pascal Lacmann
     */
    public byte[] StringToByte(String s) {
        byte[] returnByte = null;
        try {
            returnByte = s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(CommonFunctions.class.getName()).log(Level.SEVERE, null, ex);
        }
        return returnByte;
    }

    /**
     * Diese Funktion generiert einen AES-Key. Die dafür genutzen Parameter sind
     * der der Username und das Passwort. Diese Funktion generiert einen
     * AES-Key.
     *
     * @param username Benutzername des Benutzers
     * @param password Passwort des Benutzers
     * @author Pascal Lacmann
     * @return Die Funktion gibt einen AES-Key zurück.
     */
    public Key generateBenutzerAESKey(String username, String password) throws InternalServerErrorException {
        try {
            //Generiere String für den Key
            String stringForKey = password + username;


            //Kürze oder Verlängere String auf die Keylänge
            if (stringForKey.length() > 16) {
                stringForKey = stringForKey.substring(0, 16);
            } else {
                for (int i = 0; stringForKey.length() < 16; i++) {
                    stringForKey = stringForKey + "0";
                }
            }

            //Erzeuge Key
            Key key = new SecretKeySpec(stringForKey.getBytes("UTF-8"), "AES");
            return key;
        } catch (UnsupportedEncodingException ex) {
            throw new InternalServerErrorException("UnsupportedEncodingException: " + ex.getMessage());
        }
    }

    /**
     * Überprüft ein Password auf die Einhaltung der Passwortrichtlinien.
     *
     * @param password Das Passwort, welches untersucht werden soll.
     * @return True, wenn die Richtlinien eingehalten wurden, sonst false.
     */
    public Boolean checkPassword(String password) {
        if ((password.length() > 8)
                && (password.matches(".*[a-zA-Z]+.*"))
                && (password.matches(".*[0-9]+.*"))
                && (password.matches(".*\\p{Punct}+.*"))) {
            return true;
        }
        return false;
    }

    /**
     * Überprüft ob der übergebene myUser den Userrichtlinien entspricht.
     *
     * @param myUser Das myUser Objekt, welches untersucht werden soll.
     * @return True, wenn die Richtlinien eingehalten wurden, sonst false.
     */
    public Boolean checkUserDetails(MyUser myUser) {
//        TODO: Mail auf gültigkeit überprüfen
        if (myUser.getUsername().length() > 0 && myUser.getUsername().length() <= 15
                && myUser.getMail().length() > 0 && myUser.getMail().length() <= 30
                && myUser.getForename().length() <= 15 && myUser.getLastname().length() <= 15
                && myUser.getResidence().length() <= 15) {
            return true;
        }
        return false;
    }

    /**
     * Hasht einen String.
     *
     * @param password String, der gehasht werden soll.
     * @param salt Salt, der an das Passwort angehängt wird.
     * @return Es wird der Hash zurückgegeben.
     */
    public byte[] HashPassword(String password, byte[] salt) {
        return MD5.digest(StringToByte(password + Arrays.toString(salt)));
    }
}
