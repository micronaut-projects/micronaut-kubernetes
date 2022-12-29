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
import io.micronaut.core.bind.annotation.Bindable;

import java.util.Optional;

/**
 * {@link io.kubernetes.client.openapi.ApiClient} configuration.
 *
 * @author Pavol Gressa
 * @since 2.2
 */
@ConfigurationProperties(ApiClientConfiguration.PREFIX)
@BootstrapContextCompatible
public interface ApiClientConfiguration {

    String PREFIX = "kubernetes.client";

    /**
     * @return kubernetes api base path
     */
    Optional<String> getBasePath();

    /**
     * @return kubernetes ca file path
     */
    Optional<String> getCaPath();

    /**
     * @return kubernetes auth token file path
     */
    Optional<String> getTokenPath();

    /**
     * @return kubernetes kube config path
     */
    Optional<String> getKubeConfigPath();

    /**
     * @return kubernetes client namespace
     */
    Optional<String> getNamespace();

    /**
     * @return should verify ssl
     */
    @Bindable(defaultValue = "true")
    boolean getVerifySsl();

    /**
     * {@link io.kubernetes.client.Discovery} configuration.
     *
     * @author Pavol Gressa
     * @since 2.2
     */
    @ConfigurationProperties(ApiDiscoveryCacheConfiguration.PREFIX)
    interface ApiDiscoveryCacheConfiguration {
        String PREFIX = "api-discovery.cache";
        String DEFAULT_REFRESH_INTERVAL = "30";

        /**
         * Default refresh interval of API discovery.
         *
         * @return refresh interval in minutes
         */
        @Bindable(defaultValue = DEFAULT_REFRESH_INTERVAL)
        long getRefreshInterval();
    }
}
