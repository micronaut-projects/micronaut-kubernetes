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

import io.micronaut.discovery.ServiceInstance;
import org.reactivestreams.Publisher;

import java.util.List;

/**
 * Kubernetes service instance provider.
 *
 * @author Pavol Gressa
 * @since 2.3
 */
public interface KubernetesServiceInstanceProvider {

    /**
     * @return the provider mode name.
     */
    String getMode();

    /**
     * @param serviceConfiguration service discovery configuration
     * @return discovered service instances
     */
    Publisher<List<ServiceInstance>> getInstances(KubernetesServiceConfiguration serviceConfiguration);

    /**
     * @param namespace namespace
     * @return provider service ids for given namespace
     */
    Publisher<String> getServiceIds(String namespace);
}
