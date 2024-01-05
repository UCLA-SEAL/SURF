
package org.jgroups.protocols;

import org.jgroups.*;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.ManagedAttribute;
import org.jgroups.annotations.Property;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.util.*;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ASYM_ENCRYPT  {
    /** Generates the public/private key pair from the init params */
    protected void initKeyPair() throws Exception {
        if (this.key_pair == null) {
            // generate keys according to the specified algorithms
            // generate publicKey and Private Key
            KeyPairGenerator KpairGen=null;
            if(provider != null && !provider.trim().isEmpty())
                KpairGen=KeyPairGenerator.getInstance(getAlgorithm(asym_algorithm), provider);
            else
                KpairGen=KeyPairGenerator.getInstance(getAlgorithm(asym_algorithm));
            KpairGen.initialize(asym_keylength,new SecureRandom());
            key_pair=KpairGen.generateKeyPair();
        }

        // set up the Cipher to decrypt secret key responses encrypted with our key
        if(provider != null && !provider.trim().isEmpty())
            asym_cipher=Cipher.getInstance(asym_algorithm, provider);
        else
            asym_cipher=Cipher.getInstance(asym_algorithm);
        asym_cipher.init(Cipher.DECRYPT_MODE, key_pair.getPrivate());
    }

}