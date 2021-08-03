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
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;
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
public class ApiClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ApiClientFactory.class);

    /**
     * Creates in-cluster ApiClient.
     *
     * @param apiClientConfiguration api client configuration that overrides default configuration
     * @return ApiClient api client
     * @throws IOException if the CA or Token files were not found
     */
    @Requires(env = Environment.KUBERNETES)
    @Singleton
    ApiClient inClusterApiClient(ApiClientConfiguration apiClientConfiguration) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("In cluster api client configuration used.");
        }
        ClientBuilder clientBuilder = ClientBuilder.cluster();
        updateBuilderConfiguration(apiClientConfiguration, clientBuilder);
        ApiClient apiClient = clientBuilder.build();
        Configuration.setDefaultApiClient(apiClient);
        return apiClient;
    }

    /**
     * Creates ApiClient that is configured from ~/.kube/config when the {@link Environment} is Kubernetes.
     *
     * @param apiClientConfiguration api client configuration that overrides default configuration
     * @return ApiClient api client
     * @throws IOException if the CA or Token files were not found
     */
    @Requires(notEnv = Environment.KUBERNETES)
    @Singleton
    ApiClient kubeConfigFileApiClient(ApiClientConfiguration apiClientConfiguration) throws IOException {
        String kubeConfigPath = System.getenv("HOME") + "/.kube/config";
        if (apiClientConfiguration.getKubeConfigPath().isPresent()) {
            kubeConfigPath = apiClientConfiguration.getKubeConfigPath().get();
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Kube config [" + kubeConfigPath + "] api client configuration used.");
        }

        final KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath));
        ClientBuilder clientBuilder = ClientBuilder.kubeconfig(kubeConfig);
        updateBuilderConfiguration(apiClientConfiguration, clientBuilder);
        ApiClient client = clientBuilder.build();
        return client;
    }

    private void updateBuilderConfiguration(ApiClientConfiguration apiClientConfiguration, ClientBuilder builder) throws IOException {
        builder.setVerifyingSsl(apiClientConfiguration.getVerifySsl());

        if (apiClientConfiguration.getBasePath().isPresent()) {
            builder.setBasePath(apiClientConfiguration.getBasePath().get());
        }

        if (apiClientConfiguration.getCaPath().isPresent()) {
            builder.setCertificateAuthority(Files.readAllBytes(Paths.get(apiClientConfiguration.getCaPath().get())));
        }

        if (apiClientConfiguration.getTokenPath().isPresent()) {
            builder.setAuthentication(new TokenFileAuthentication(apiClientConfiguration.getTokenPath().get()));
        }
    }
}
