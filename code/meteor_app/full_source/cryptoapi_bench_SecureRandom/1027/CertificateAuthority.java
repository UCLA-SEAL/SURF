
package com.github.tomakehurst.wiremock.http.ssl;

import static com.github.tomakehurst.wiremock.common.ArrayFunctions.prepend;
import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Date;
import javax.net.ssl.SNIHostName;
import sun.security.x509.*;

public class CertificateAuthority {
  private static KeyPair generateKeyPair(String keyType) throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyType);
    keyGen.initialize(2048, new SecureRandom());
    return keyGen.generateKeyPair();
  }
}