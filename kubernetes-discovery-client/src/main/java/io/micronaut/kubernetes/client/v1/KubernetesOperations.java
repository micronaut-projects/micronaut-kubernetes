/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.kubernetes.client.v1;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Get;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapList;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapWatchEvent;
import io.micronaut.kubernetes.client.v1.endpoints.Endpoints;
import io.micronaut.kubernetes.client.v1.endpoints.EndpointsList;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.micronaut.kubernetes.client.v1.secrets.SecretList;
import io.micronaut.kubernetes.client.v1.services.Service;
import io.micronaut.kubernetes.client.v1.services.ServiceList;
import org.reactivestreams.Publisher;

import javax.annotation.Nullable;

/**
 * Defines the HTTP requests to query the Kubernetes API.
 *
 * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.13/">Kubernetes API overview</a>
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public interface KubernetesOperations {

    String DEFAULT_NAMESPACE = "default";

    /**
     * List services in the default namespace.
     * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.13/#list-all-namespaces-service-v1-core">List all namespaces</a>
     * @return A list of services
     */
    default Publisher<ServiceList> listServices() {
        return listServices(DEFAULT_NAMESPACE);
    }

    /**
     * List services in the specified namespace.
     * @param namespace Object name and auth scope, such as for teams and projects
     * @return A list of services
     */
    @Get("/namespaces/{namespace}/services")
    Publisher<ServiceList> listServices(String namespace);


    /**
     * Read the specified Service.
     * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.13/#read-service-v1-core">Service v1 core - Read operations - Read</a>
     * @param namespace Object name and auth scope, such as for teams and projects
     * @param serviceName The name of the service
     * @return A Service
     */
    @Get("/namespaces/{namespace}/services/{serviceName}")
    Publisher<Service> getService(String namespace, String serviceName);

    /**
     * @param serviceName The name of the service
     * @return Service for the specified name in the namespace ({@value #DEFAULT_NAMESPACE}).
     */
    default Publisher<Service> getService(String serviceName) {
        return getService(DEFAULT_NAMESPACE, serviceName);
    }

    /**
     * List or watch objects of kind Endpoints.
     * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.13/#list-all-namespaces-endpoints-v1-core">Endpoints v1 core - Read Operations - List All NameSpaces</a>
     * @return A list of endpoints
     */
    default Publisher<EndpointsList> listEndpoints() {
        return listEndpoints(DEFAULT_NAMESPACE);
    }

    /**
     * List endpoints in the given namespace.
     * @param namespace Object name and auth scope, such as for teams and projects
     * @return A list of endpoints
     */
    @Get("/namespaces/{namespace}/endpoints")
    Publisher<EndpointsList> listEndpoints(String namespace);

    /**
     * Read the specified Endpoints.
     *
     * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.13/#read-endpoints-v1-core">Endpoints v1 core - Read operations</a>
     * @param namespace object name and auth scope, such as for teams and projects
     * @param serviceName name of the endpoints
     * @return Endpoints
     */
    @Get("/namespaces/{namespace}/endpoints/{serviceName}")
    Publisher<Endpoints> getEndpoints(String namespace, String serviceName);

    /**
     * @param serviceName name of the endpoints.
     * @return Returns the endpoints for the specified name in the namespace ({@value #DEFAULT_NAMESPACE}).
     */
    default Publisher<Endpoints> getEndpoints(String serviceName) {
        return getEndpoints(DEFAULT_NAMESPACE, serviceName);
    }

    /**
     * List objects of kind ConfigMap.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param labelSelector A selector to restrict the list of returned objects by their labels
     * @return a {@link ConfigMapList}
     */
    @Get("/namespaces/{namespace}/configmaps?labelSelector={labelSelector}")
    Publisher<ConfigMapList> listConfigMaps(String namespace, @Nullable String labelSelector);

    /**
     * @param namespace object name and auth scope, such as for teams and projects
     * @return a {@link ConfigMapList} of the given namespace
     */
    default Publisher<ConfigMapList> listConfigMaps(String namespace) {
        return listConfigMaps(namespace, null);
    }

    /**
     * @return a {@link ConfigMapList} of the default namespace
     */
    default Publisher<ConfigMapList> listConfigMaps() {
        return listConfigMaps(DEFAULT_NAMESPACE);
    }

    /**
     * Watch objects of kind ConfigMap.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param resourceVersion the resource version to receive events from. If set to 0, Kubernetes will send all events
     * @param labelSelector A selector to restrict the list of returned objects by their labels
     * @return a {@link ConfigMapList}
     */
    @Get("/watch/namespaces/{namespace}/configmaps?resourceVersion={resourceVersion}&labelSelector={labelSelector}")
    @Consumes(value = {MediaType.APPLICATION_JSON_STREAM, MediaType.APPLICATION_JSON})
    Publisher<ConfigMapWatchEvent> watchConfigMaps(String namespace, Integer resourceVersion, @Nullable String labelSelector);

    /**
     * Read the specified ConfigMap.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param configMapName name of the ConfigMap
     * @return a {@link ConfigMap}
     */
    @Get("/namespaces/{namespace}/configmaps/{configMapName}")
    Publisher<ConfigMap> getConfigMap(String namespace, String configMapName);

    /**
     * List objects of kind Secret.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param labelSelector A selector to restrict the list of returned objects by their labels
     * @return a {@link SecretList}
     */
    @Get("/namespaces/{namespace}/secrets?labelSelector={labelSelector}")
    Publisher<SecretList> listSecrets(String namespace, @Nullable String labelSelector);

    /**
     * @param namespace object name and auth scope, such as for teams and projects
     * @return a {@link SecretList}
     */
    default Publisher<SecretList> listSecrets(String namespace) {
        return listSecrets(namespace, null);
    }

    /**
     * @return a {@link SecretList} of the default namespace
     */
    default Publisher<SecretList> listSecrets() {
        return listSecrets(DEFAULT_NAMESPACE);
    }

    /**
     * Returns a {@link Secret} of the given namespace.
     * @param namespace object name and auth scope, such as for teams and projects
     * @param secretName the secret name
     * @return A {@link Secret} instance
     */
    @Get("/namespaces/{namespace}/secrets/{secretName}")
    Publisher<Secret> getSecret(String namespace, String secretName);

}
