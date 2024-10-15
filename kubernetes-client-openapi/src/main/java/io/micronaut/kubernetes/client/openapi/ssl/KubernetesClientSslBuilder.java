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
import io.micronaut.kubernetes.client.openapi.config.KubeConfig;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.config.model.AuthInfo;
import io.micronaut.kubernetes.client.openapi.config.model.Cluster;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
public final class KubernetesClientSslBuilder extends NettyClientSslBuilder {
    private static final String X509_CERTIFICATE_TYPE = "X509";

    private final ResourceResolver resourceResolver;
    private final KubeConfig kubeConfig;
    private final KubernetesPrivateKeyLoader kubernetesPrivateKeyLoader;
    private final KubernetesClientConfiguration kubernetesClientConfiguration;

    public KubernetesClientSslBuilder(ResourceResolver resourceResolver,
                                      KubeConfig kubeConfig,
                                      KubernetesPrivateKeyLoader kubernetesPrivateKeyLoader,
                                      KubernetesClientConfiguration kubernetesClientConfiguration) {
        super(resourceResolver);
        this.resourceResolver = resourceResolver;
        this.kubeConfig = kubeConfig;
        this.kubernetesPrivateKeyLoader = kubernetesPrivateKeyLoader;
        this.kubernetesClientConfiguration = kubernetesClientConfiguration;
    }

    @Override
    protected Optional<KeyStore> getKeyStore(SslConfiguration ssl) throws Exception {
        if (kubeConfig == null || kubeConfig.getUser() == null) {
            return Optional.empty();
        }
        AuthInfo user = kubeConfig.getUser();
        byte[] clientCert = user.clientCertificateData();
        byte[] clientKey = user.clientKeyData();
        if (clientCert == null || clientKey == null) {
            return Optional.empty();
        }

        CertificateFactory certFactory = CertificateFactory.getInstance(X509_CERTIFICATE_TYPE);
        Collection<? extends Certificate> certs = certFactory.generateCertificates(new ByteArrayInputStream(clientCert));

        String keyAlias;
        Optional<String> keyAliasOpt = ssl.getKey().getAlias();
        if (keyAliasOpt.isPresent()) {
            keyAlias = keyAliasOpt.get();
        } else {
            keyAlias = ((X509Certificate) certs.iterator().next()).getSubjectX500Principal().getName();
        }

        PrivateKey privateKey = kubernetesPrivateKeyLoader.loadPrivateKey(clientKey);

        Optional<String> keyPassOpt = ssl.getKey().getPassword();
        Optional<String> keyStorePassOpt = ssl.getKeyStore().getPassword();
        String keyPass;
        if (keyPassOpt.isPresent()) {
            keyPass = keyPassOpt.get();
        } else if (keyStorePassOpt.isPresent()) {
            keyPass = keyStorePassOpt.get();
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
        byte[] caBytes = null;
        if (kubeConfig != null) {
            Cluster cluster = kubeConfig.getCluster();
            Boolean insecureSkipTlsVerify = cluster.insecureSkipTlsVerify();
            if (insecureSkipTlsVerify != null && insecureSkipTlsVerify) {
                ((ClientSslConfiguration) ssl).setInsecureTrustAllCertificates(true);
                return Optional.empty();
            }
            caBytes = cluster.certificateAuthorityData();
        } else if (kubernetesClientConfiguration.getServiceAccount().isEnabled()) {
            String caPath = kubernetesClientConfiguration.getServiceAccount().getCertificateAuthorityPath();
            Optional<InputStream> inputStreamOpt = resourceResolver.getResourceAsStream(caPath);
            if (inputStreamOpt.isEmpty()) {
                return Optional.empty();
            }
            InputStream inputStream = inputStreamOpt.get();
            caBytes = inputStream.readAllBytes();
        }

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
