/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.kubernetes.client.openapi;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.http.client.DefaultHttpClientConfiguration.DefaultConnectionPoolConfiguration;
import io.micronaut.http.client.DefaultHttpClientConfiguration.DefaultHttp2ClientConfiguration;
import io.micronaut.http.client.DefaultHttpClientConfiguration.DefaultWebSocketCompressionConfiguration;
import io.micronaut.http.client.HttpClientConfiguration;
import io.micronaut.http.ssl.ClientSslConfiguration;
import io.micronaut.runtime.ApplicationConfiguration;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

@BootstrapContextCompatible
@ConfigurationProperties(KubernetesHttpClientConfiguration.PREFIX)
final class KubernetesHttpClientConfiguration extends HttpClientConfiguration {

    static final String PREFIX = "micronaut.http.client.kubernetes";

    private final ConnectionPoolConfiguration connectionPoolConfiguration;

    private final WebSocketCompressionConfiguration webSocketCompressionConfiguration;

    private final Http2ClientConfiguration http2ClientConfiguration;

    KubernetesHttpClientConfiguration(@Nullable DefaultConnectionPoolConfiguration defaultConnectionPoolConfiguration,
                                      @Nullable DefaultWebSocketCompressionConfiguration defaultWebSocketCompressionConfiguration,
                                      @Nullable DefaultHttp2ClientConfiguration defaultHttp2ClientConfiguration,
                                      @Nullable ApplicationConfiguration applicationConfiguration) {
        super(applicationConfiguration);
        connectionPoolConfiguration = defaultConnectionPoolConfiguration == null
            ? new DefaultConnectionPoolConfiguration()
            : defaultConnectionPoolConfiguration;
        webSocketCompressionConfiguration = defaultWebSocketCompressionConfiguration == null
            ? new DefaultWebSocketCompressionConfiguration()
            : defaultWebSocketCompressionConfiguration;
        http2ClientConfiguration = defaultHttp2ClientConfiguration == null
            ? new DefaultHttp2ClientConfiguration()
            : defaultHttp2ClientConfiguration;
    }

    /**
     * Uses the default SSL configuration.
     *
     * @param sslConfiguration The SSL configuration
     */
    @Inject
    public void setClientSslConfiguration(@Nullable ClientSslConfiguration sslConfiguration) {
        if (sslConfiguration != null) {
            super.setSslConfiguration(sslConfiguration);
        }
    }

    @Override
    public ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        return connectionPoolConfiguration;
    }

    @Override
    public WebSocketCompressionConfiguration getWebSocketCompressionConfiguration() {
        return webSocketCompressionConfiguration;
    }

    @Override
    public Http2ClientConfiguration getHttp2Configuration() {
        return http2ClientConfiguration;
    }
}
