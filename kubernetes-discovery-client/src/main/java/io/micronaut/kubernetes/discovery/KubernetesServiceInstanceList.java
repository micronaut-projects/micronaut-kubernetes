/*
 * Copyright 2017-2020 original authors
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
package io.micronaut.kubernetes.discovery;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.discovery.ServiceInstance;
import io.micronaut.discovery.ServiceInstanceList;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * A {@link io.micronaut.discovery.ServiceInstanceList} implementation for Kubernetes.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Singleton
@Requires(env = Environment.KUBERNETES)
@BootstrapContextCompatible
public class KubernetesServiceInstanceList implements ServiceInstanceList {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesServiceInstanceList.class);

    private KubernetesConfiguration configuration;

    /**
     * @param configuration The {@link KubernetesConfiguration}.
     */
    public KubernetesServiceInstanceList(KubernetesConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String getID() {
        return KubernetesClient.SERVICE_ID;
    }

    @Override
    public List<ServiceInstance> getInstances() {
        String spec = (configuration.isSecure() ? "https" : "http") + "://" + configuration.getHost() + ":" + configuration.getPort();
        return Collections.singletonList(
                ServiceInstance.builder(getID(), URI.create(spec)).build()
        );
    }

}
