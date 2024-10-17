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
package io.micronaut.kubernetes.client.openapi.config;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.util.Toggleable;

import java.time.Duration;

/**
 * Kubernetes client configuration.
 */
@Internal
@BootstrapContextCompatible
@ConfigurationProperties(KubernetesClientConfiguration.PREFIX)
@Requires(property = KubernetesClientConfiguration.PREFIX + ".enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
public class KubernetesClientConfiguration implements Toggleable {

    public static final String PREFIX = "kubernetes.client";

    private String kubeConfigPath;

    private boolean enabled = true;

    private ServiceAccount serviceAccount = new ServiceAccount();

    /**
     * Path of the kube config file. Default: {@code file:$HOME/.kube/config}.
     *
     * @return kube config path
     */
    public String getKubeConfigPath() {
        return kubeConfigPath;
    }

    /**
     * Sets kube config path.
     *
     * @param kubeConfigPath kube config path
     */
    void setKubeConfigPath(String kubeConfigPath) {
        this.kubeConfigPath = kubeConfigPath;
    }

    /**
     * Enable/disable kubernetes client. Default: {@code true}.
     *
     * @return {@code true} if kubernetes client is enabled, {@code false} otherwise
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable/disable kubernetes client.
     *
     * @param enabled {@code true} to enable kubernetes client
     */
    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Service account authentication configuration.
     *
     * @return service account authentication configuration
     */
    public ServiceAccount getServiceAccount() {
        return serviceAccount;
    }

    /**
     * Sets service account authentication configuration.
     *
     * @param serviceAccount service account authentication configuration
     */
    public void setServiceAccount(ServiceAccount serviceAccount) {
        this.serviceAccount = serviceAccount;
    }

    /**
     * Service account authentication configuration.
     */
    @ConfigurationProperties("service-account")
    public static class ServiceAccount {
        private static final String SERVICE_ACCOUNT_DIR = "file:/var/run/secrets/kubernetes.io/serviceaccount/";
        private static final String CA_PATH = SERVICE_ACCOUNT_DIR + "ca.crt";
        private static final String TOKEN_PATH = SERVICE_ACCOUNT_DIR + "token";

        private boolean enabled = true;
        private String certificateAuthorityPath = CA_PATH;
        private String tokenPath = TOKEN_PATH;
        private Duration tokenReloadInterval = Duration.ofSeconds(60);

        /**
         * Enable/disable service account authentication. Default: {@code true}.
         *
         * @return {@code true} if service account authentication enabled, {@code false} otherwise
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Enable/disable service account authentication.
         *
         * @param enabled {@code true} to enable service account authentication
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Path to the certificate authority file. Default: {@code file:/var/run/secrets/kubernetes.io/serviceaccount/ca.crt}.
         *
         * @return path to the certificate authority file
         */
        public String getCertificateAuthorityPath() {
            return certificateAuthorityPath;
        }

        /**
         * Sets path to the certificate authority file.
         *
         * @param certificateAuthorityPath path to the certificate authority file
         */
        public void setCertificateAuthorityPath(String certificateAuthorityPath) {
            this.certificateAuthorityPath = certificateAuthorityPath;
        }

        /**
         * Path to the token file. Default: {@code file:/var/run/secrets/kubernetes.io/serviceaccount/token}.
         *
         * @return path to the token file
         */
        public String getTokenPath() {
            return tokenPath;
        }

        /**
         * Sets path to the token file.
         *
         * @param tokenPath path to the token file
         */
        public void setTokenPath(String tokenPath) {
            this.tokenPath = tokenPath;
        }

        /**
         * Token reload interval. Default: {@code 60s}.
         *
         * @return token reload interval
         */
        public Duration getTokenReloadInterval() {
            return tokenReloadInterval;
        }

        /**
         * Sets token reload interval.
         *
         * @param tokenReloadInterval token reload interval
         */
        public void setTokenReloadInterval(Duration tokenReloadInterval) {
            this.tokenReloadInterval = tokenReloadInterval;
        }
    }
}
