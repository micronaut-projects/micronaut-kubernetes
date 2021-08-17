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

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;

import java.util.Optional;

/**
 * {@link io.kubernetes.client.openapi.ApiClient} configuration.
 *
 * @author Pavol Gressa
 * @since 2.2
 */
@ConfigurationProperties(ApiClientConfiguration.PREFIX)
@BootstrapContextCompatible
public class ApiClientConfiguration {

    public static final String PREFIX = "kubernetes.client";

    private String basePath;
    private String caPath;
    private String tokenPath;
    private String kubeConfigPath;
    private boolean verifySsl = true;

    /**
     * @return kubernetes api base path
     */
    Optional<String> getBasePath() {
        return Optional.ofNullable(basePath);
    }

    /**
     * @param basePath optional base path of kubernetes api
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    /**
     * @return kubernetes ca file path
     */
    Optional<String> getCaPath() {
        return Optional.ofNullable(caPath);
    }

    /**
     * @param caPath optional ca path
     */
    public void setCaPath(String caPath) {
        this.caPath = caPath;
    }

    /**
     * @return kubernetes auth token file path
     */
    Optional<String> getTokenPath() {
        return Optional.ofNullable(tokenPath);
    }

    /**
     * @param tokenPath optional token path
     */
    public void setTokenPath(String tokenPath) {
        this.tokenPath = tokenPath;
    }

    /**
     * @return kubernetes kube config path
     */
    Optional<String> getKubeConfigPath() {
        return Optional.ofNullable(kubeConfigPath);
    }

    /**
     * @param kubeConfigPath kubernetes config path other than default {@code $HOME/.kube/config}
     */
    public void setKubeConfigPath(String kubeConfigPath) {
        this.kubeConfigPath = kubeConfigPath;
    }

    /**
     * @return should verify ssl
     */
    public boolean getVerifySsl() {
        return verifySsl;
    }

    /**
     * @param verifySsl sets verify ssl
     */
    public void setVerifySsl(boolean verifySsl) {
        this.verifySsl = verifySsl;
    }
}
