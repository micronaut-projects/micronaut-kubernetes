/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.client;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.credentials.TokenFileAuthentication;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * {@link ApiClient} bean factory that creates either in cluster {@link ClientBuilder#cluster()} client or
 * {@link ClientBuilder#kubeconfig(KubeConfig)} client.
 *
 * @author Pavol Gressa
 * @since 2.2
 */
@Factory
@BootstrapContextCompatible
public class ApiClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ApiClientFactory.class);

    /**
     * Creates {@link ClientBuilder} that is either configured from specified configuration options or automatically
     * detected by {@link ClientBuilder#standard()}.
     *
     * @param apiClientConfiguration api client configuration that overrides default configuration
     * @return client builder
     * @throws IOException if the CA or Token files were not found
     * @since 3.0
     */
    @Singleton
    public ClientBuilder clientBuilder(ApiClientConfiguration apiClientConfiguration) throws IOException {
        ClientBuilder clientBuilder = null;

        if (apiClientConfiguration.getKubeConfigPath().isPresent()) {
            final String customKubeConfigPath = apiClientConfiguration.getKubeConfigPath().get();
            if (new File(customKubeConfigPath).exists()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Using custom kube config from path: {}", customKubeConfigPath);
                }
                final KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new FileReader(customKubeConfigPath));
                clientBuilder = ClientBuilder.kubeconfig(kubeConfig);
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Custom kube config path '{}' defined but file doesn't exists", customKubeConfigPath);
                }
            }
        }

        if (clientBuilder == null) {
            clientBuilder = ClientBuilder.standard();
        }
        updateBuilderConfiguration(apiClientConfiguration, clientBuilder);
        return clientBuilder;
    }

    /**
     * Creates ApiClient.
     *
     * @param clientBuilder client builder
     * @return ApiClient api client
     * @throws IOException if the CA or Token files were not found
     */
    @Singleton
    public ApiClient apiClient(ClientBuilder clientBuilder) throws IOException {
        ApiClient apiClient = clientBuilder.build();
        Configuration.setDefaultApiClient(apiClient);
        OkHttpClient.Builder builder = apiClient.getHttpClient().newBuilder();
        builder.addInterceptor(new OkHttpClientLogging());
        return apiClient;
    }

    private void updateBuilderConfiguration(ApiClientConfiguration apiClientConfiguration, ClientBuilder builder) {
        builder.setVerifyingSsl(apiClientConfiguration.getVerifySsl());

        if (apiClientConfiguration.getBasePath().isPresent()) {
            final String basePath = apiClientConfiguration.getBasePath().get();
            if (LOG.isInfoEnabled()) {
                LOG.info("Configuring basePath '{}'", basePath);
            }
            builder.setBasePath(basePath);
        }

        if (apiClientConfiguration.getCaPath().isPresent()) {
            final String caPath = apiClientConfiguration.getCaPath().get();
            try {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Configuring caPath '{}'", caPath);
                }
                builder.setCertificateAuthority(Files.readAllBytes(Paths.get(caPath)));
            } catch (IOException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to load caPath from '{}': {}", caPath, e.getMessage(), e);
                }
            }
        }

        if (apiClientConfiguration.getTokenPath().isPresent()) {
            final String tokenPath = apiClientConfiguration.getTokenPath().get();
            if (LOG.isInfoEnabled()) {
                LOG.info("Configuring tokenPath '{}'", tokenPath);
            }
            builder.setAuthentication(new TokenFileAuthentication(tokenPath));
        }
    }
}
