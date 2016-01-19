/*
 * Copyright 2014, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.nv.fre.api;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

public class GrpcTlsUtils {

  /**
   * Returns the ciphers preferred to use during tests. They may be chosen because they are widely
   * available or because they are fast. There is no requirement that they provide confidentiality
   * or integrity.
   */
  public static List<String> preferredCiphers() {
    String[] ciphers;
    try {
      ciphers = SSLContext.getDefault().getDefaultSSLParameters().getCipherSuites();
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
    List<String> ciphersMinusGcm = new ArrayList<String>();
    for (String cipher : ciphers) {
      // The GCM implementation in Java is _very_ slow (~1 MB/s)
      if (cipher.contains("_GCM_")) {
        continue;
      }
      ciphersMinusGcm.add(cipher);
    }
    return Collections.unmodifiableList(ciphersMinusGcm);
  }

  /**
   * Creates an SSLSocketFactory which contains {@code certChainFile} as its only root certificate.
   */
  public static SSLSocketFactory newSslSocketFactoryForCa(InputStream certChain) throws Exception {
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null, null);
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    X509Certificate cert = (X509Certificate) cf.generateCertificate(
        new BufferedInputStream(certChain));
    X500Principal principal = cert.getSubjectX500Principal();
    ks.setCertificateEntry(principal.getName("RFC2253"), cert);
//    ks.setCertificateEntry("ca", cert);

    // Set up trust manager factory to use our key store.
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(ks);
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, trustManagerFactory.getTrustManagers(), null);
    return context.getSocketFactory();
  }

  private GrpcTlsUtils() {}
}
