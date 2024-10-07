/*
 * Copyright 2017-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.client.openapi.ssl;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.client.netty.ssl.NettyClientSslBuilder;
import io.micronaut.http.ssl.ClientSslConfiguration;
import io.micronaut.http.ssl.SslConfiguration;
import io.micronaut.kubernetes.client.openapi.config.model.AuthInfo;
import io.micronaut.kubernetes.client.openapi.config.model.Cluster;
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Optional;

/**
 * The ssl builder which uses data from a kube config file to create a key store and trust store.
 */
@Internal
public class KubernetesClientSslBuilder extends NettyClientSslBuilder {

    private KubeConfig kubeConfig;
    private KubernetesPrivateKeyLoader kubernetesPrivateKeyLoader;

    public KubernetesClientSslBuilder(ResourceResolver resourceResolver,
                                      KubeConfig kubeConfig,
                                      KubernetesPrivateKeyLoader kubernetesPrivateKeyLoader) {
        super(resourceResolver);
        this.kubeConfig = kubeConfig;
        this.kubernetesPrivateKeyLoader = kubernetesPrivateKeyLoader;
    }

    @Override
    protected Optional<KeyStore> getKeyStore(SslConfiguration ssl) throws Exception {
        AuthInfo user = kubeConfig.getUser();
        byte[] clientCert = user.clientCertificateData();
        byte[] clientKey = user.clientKeyData();
        if (clientCert == null || clientKey == null) {
            return Optional.empty();
        }

        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        Collection<? extends Certificate> certs = certFactory.generateCertificates(new ByteArrayInputStream(clientCert));

        String keyAlias;
        if (ssl.getKey().getAlias().isPresent()) {
            keyAlias = ssl.getKey().getAlias().get();
        } else {
            keyAlias = ((X509Certificate) certs.stream().findFirst().get()).getSubjectX500Principal().getName();
        }

        PrivateKey privateKey = kubernetesPrivateKeyLoader.loadPrivateKey(clientKey);

        String keyPass;
        if (ssl.getKey().getPassword().isPresent()) {
            keyPass = ssl.getKey().getPassword().get();
        } else if (ssl.getKeyStore().getPassword().isPresent()) {
            keyPass = ssl.getKeyStore().getPassword().get();
        } else {
            keyPass = "";
            ssl.getKey().setPassword("");
        }

        KeyStore keyStore = KeyStore.getInstance(ssl.getKeyStore().getType().orElse("JKS"));
        keyStore.load(null);
        keyStore.setKeyEntry(keyAlias, privateKey, keyPass.toCharArray(), certs.toArray(new X509Certificate[0]));
        return Optional.of(keyStore);
    }

    @Override
    protected Optional<KeyStore> getTrustStore(SslConfiguration ssl) throws Exception {
        Cluster cluster = kubeConfig.getCluster();

        Boolean insecureSkipTlsVerify = cluster.insecureSkipTlsVerify();
        if (insecureSkipTlsVerify != null && insecureSkipTlsVerify) {
            ((ClientSslConfiguration) ssl).setInsecureTrustAllCertificates(true);
            return Optional.empty();
        }

        byte[] caBytes = cluster.certificateAuthorityData();
        if (caBytes == null) {
            return Optional.empty();
        }

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certs = certificateFactory.generateCertificates(new ByteArrayInputStream(caBytes));
        if (certs.isEmpty()) {
            throw new IllegalArgumentException("Expected non-empty set of trusted certificates");
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        int i = 0;
        for (Certificate cert : certs) {
            keyStore.setCertificateEntry("ca" + i, cert);
            i++;
        }
        return Optional.of(keyStore);
    }
}
