
package org.parosproxy.paros.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.security.SecureRandom;


import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Logger;
import org.parosproxy.paros.security.CachedSslCertifificateServiceImpl;
import org.parosproxy.paros.security.SslCertificateService;

import ch.csnc.extension.httpclient.SSLContextManager;

public class SSLConnector implements SecureProtocolSocketFactory {

	public SSLSocketFactory getTunnelSSLSocketFactory(String hostname) {

		//	SSLServerSocketFactory ssf = null;
		// set up key manager to do server authentication

		//	KeyStore ks;
		try {
			SSLContext ctx = SSLContext.getInstance(SSL);
			// Normally "SunX509", "IbmX509"...
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

			SslCertificateService scs = CachedSslCertifificateServiceImpl.getService();
			KeyStore ks = scs.createCertForHost(hostname);

			kmf.init(ks, SslCertificateService.PASSPHRASE);
			SecureRandom x = new SecureRandom();
			x.setSeed(System.currentTimeMillis());
			ctx.init(kmf.getKeyManagers(), null, x);

			SSLSocketFactory tunnelSSLFactory = createDecoratedServerSslSocketFactory(ctx.getSocketFactory());

			return tunnelSSLFactory;

        } catch (NoSuchAlgorithmException  e) {
            // Turn into RuntimeException. How to handle this error in a user
            // friendly way?
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

	}
}