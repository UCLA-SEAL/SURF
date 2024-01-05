
package sun.security.ssl;

import java.security.*;

/**
 * The "KeyManager" for ephemeral RSA keys. Ephemeral DH and ECDH keys
 * are handled by the DHCrypt and ECDHCrypt classes, respectively.
 *
 * @author  Andreas Sterbenz
 */
final class EphemeralKeyManager {
    private KeyPair getRSAKeyPair(boolean export, SecureRandom random) {
        int length, index;
        if (export) {
            length = 512;
            index = 0;
        } else {
            length = 1024;
            index = 1;
        }

        synchronized (keys) {
            KeyPair kp = keys[index].getKeyPair();
            if (kp == null) {
                try {
                    KeyPairGenerator kgen = JsseJce.getKeyPairGenerator("RSA");
                    kgen.initialize(length, random);
                    keys[index] = new EphemeralKeyPair(kgen.genKeyPair());
                    kp = keys[index].getKeyPair();
                } catch (Exception e) {
                    // ignore
                }
            }
            return kp;
        }
    }
}
