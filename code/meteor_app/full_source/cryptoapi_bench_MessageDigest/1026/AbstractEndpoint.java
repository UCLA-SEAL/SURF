/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.util.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.apache.juli.logging.Log;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.buf.HexUtils;
import org.apache.tomcat.util.collections.SynchronizedStack;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.net.Acceptor.AcceptorState;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.StoreType;
import org.apache.tomcat.util.net.openssl.ciphers.Cipher;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.threads.LimitLatch;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.apache.tomcat.util.threads.TaskQueue;
import org.apache.tomcat.util.threads.TaskThreadFactory;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;

/**
 * @param <S> The type used by the socket wrapper associated with this endpoint.
 *            May be the same as U.
 * @param <U> The type of the underlying socket used by this endpoint. May be
 *            the same as S.
 *
 * @author Mladen Turk
 * @author Remy Maucherat
 */
public abstract class AbstractEndpoint<S,U> {

    protected String generateCertificateDebug(X509Certificate certificate) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n[");
        try {
            byte[] certBytes = certificate.getEncoded();
            // SHA-256 fingerprint
            sb.append("\nSHA-256 fingerprint: ");
            MessageDigest sha512Digest = MessageDigest.getInstance("SHA-256");
            sha512Digest.update(certBytes);
            sb.append(HexUtils.toHexString(sha512Digest.digest()));
            // SHA-1 fingerprint
            sb.append("\nSHA-1 fingerprint: ");
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            sha1Digest.update(certBytes);
            sb.append(HexUtils.toHexString(sha1Digest.digest()));
        } catch (CertificateEncodingException e) {
            getLogCertificate().warn(sm.getString("endpoint.tls.cert.encodingError"), e);
        } catch (NoSuchAlgorithmException e) {
            // Unreachable code
            // All JREs are required to support SHA-1 and SHA-256
            throw new RuntimeException(e);
        }
        sb.append("\n");
        sb.append(certificate);
        sb.append("\n]");
        return sb.toString();
    }
}
