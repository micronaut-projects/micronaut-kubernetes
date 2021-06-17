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
package io.micronaut.kubernetes.client.v1;



import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapList;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMapWatchEvent;
import io.micronaut.kubernetes.client.v1.endpoints.Endpoints;
import io.micronaut.kubernetes.client.v1.endpoints.EndpointsList;
import io.micronaut.kubernetes.client.v1.pods.Pod;
import io.micronaut.kubernetes.client.v1.pods.PodList;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.micronaut.kubernetes.client.v1.secrets.SecretList;
import io.micronaut.kubernetes.client.v1.secrets.SecretWatchEvent;
import io.micronaut.kubernetes.client.v1.services.Service;
import io.micronaut.kubernetes.client.v1.services.ServiceList;
import org.reactivestreams.Publisher;

/**
 * Defines the HTTP requests to query the Kubernetes API.
 *
 * @see <a href="https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.13/">Kubernetes API overview</a>
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public interface KubernetesOperations {

    /**
     * List services in the specified namespace.
     * @param namespace Object name and auth scope, such as for teams and projects
     * @param labelSelector A selector to restrict the list of returned objects by their labels
     * @return A list of services
     */
    @Get("/namespaces/{namespace}/services?labelSelector={labelSelector}")
    Publisher<ServiceList> listServices(String namespace, @Nullable String labelSelector);

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
     * List endpoints in the given namespace.
     * @param namespace Object name and auth scope, such as for teams and projects
     * @param labelSelector A selector to restrict the list of returned objects by their labels
     * @return A list of endpoints
     */
    @Get("/namespaces/{namespace}/endpoints?labelSelector={labelSelector}")
    Publisher<EndpointsList> listEndpoints(String namespace, @Nullable String labelSelector);

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
     * Watch objects of kind ConfigMap.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param resourceVersion the resource version to receive events from. If set to 0, Kubernetes will send all events
     * @param labelSelector A selector to restrict the list of returned objects by their labels
     * @return a {@link ConfigMapList}
     */
    @Get("/namespaces/{namespace}/configmaps?resourceVersion={resourceVersion}&labelSelector={labelSelector}&watch=true")
    @Consumes(value = {MediaType.APPLICATION_JSON_STREAM, MediaType.APPLICATION_JSON})
    Publisher<ConfigMapWatchEvent> watchConfigMaps(String namespace, Long resourceVersion, @Nullable String labelSelector);

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
     * Create the specified ConfigMap.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param configMap to create
     * @return a {@link ConfigMap}
     */
    @Post("/namespaces/{namespace}/configmaps")
    Publisher<ConfigMap> createConfigMap(String namespace, @Body ConfigMap configMap);

    /**
     * Replace the specified ConfigMap.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param configMapName name of the ConfigMap
     * @param configMap to replace
     * @return a {@link ConfigMap}
     */
    @Put("/namespaces/{namespace}/configmaps/{configMapName}")
    Publisher<ConfigMap> replaceConfigMap(String namespace, String configMapName, @Body ConfigMap configMap);

    /**
     * Delete the specified ConfigMap.
     *
     * @param namespace     object name and auth scope, such as for teams and projects
     * @param configMapName name of the ConfigMap
     * @return a {@link ConfigMap}
     */
    @Delete("/namespaces/{namespace}/configmaps/{configMapName}")
    Publisher<ConfigMap> deleteConfigMap(String namespace, String configMapName);

    /**
     * List objects of kind Secret.
     *
     * @param namespace     object name and auth scope, such as for teams and projects
     * @param labelSelector A selector to restrict the list of returned objects by their labels
     * @return a {@link SecretList}
     */
    @Get("/namespaces/{namespace}/secrets?labelSelector={labelSelector}")
    Publisher<SecretList> listSecrets(String namespace, @Nullable String labelSelector);

    /**
     * Watch objects of kind Secret.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param resourceVersion the resource version to receive events from. If set to 0, Kubernetes will send all events
     * @param labelSelector A selector to restrict the list of returned objects by their labels
     * @return a {@link ConfigMapList}
     */
    @Get("/namespaces/{namespace}/secrets?resourceVersion={resourceVersion}&labelSelector={labelSelector}&watch=true")
    @Consumes(value = {MediaType.APPLICATION_JSON_STREAM, MediaType.APPLICATION_JSON})
    Publisher<SecretWatchEvent> watchSecrets(String namespace, Long resourceVersion, @Nullable String labelSelector);

    /**
     * @param namespace object name and auth scope, such as for teams and projects
     * @return a {@link SecretList}
     */
    default Publisher<SecretList> listSecrets(String namespace) {
        return listSecrets(namespace, null);
    }

    /**
     * Returns a {@link Secret} of the given namespace.
     * @param namespace object name and auth scope, such as for teams and projects
     * @param secretName the secret name
     * @return A {@link Secret} instance
     */
    @Get("/namespaces/{namespace}/secrets/{secretName}")
    Publisher<Secret> getSecret(String namespace, String secretName);

    /**
     * Create the specified Secret.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param secret    the secret
     * @return A {@link Secret} instance
     */
    @Post("/namespaces/{namespace}/secrets/")
    Publisher<Secret> createSecret(String namespace, @Body Secret secret);

    /**
     * Replace the specified Secret.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param secretName the secret name
     * @param secret    the secret to replace
     * @return A {@link Secret} instance
     */
    @Put("/namespaces/{namespace}/secrets/{secretName}")
    Publisher<Secret> replaceSecret(String namespace, String secretName, @Body Secret secret);

    /**
     * Delete the specified Secret.
     *
     * @param namespace  object name and auth scope, such as for teams and projects
     * @param secretName the secret name
     * @return A {@link Secret} instance
     */
    @Delete("/namespaces/{namespace}/secrets/{secretName}")
    Publisher<Secret> deleteSecret(String namespace, String secretName);

    /**
     * Returns a {@link Pod} of the given name in the given namespace.
     * @param namespace object name and auth scope, such as for teams and projects
     * @param podName the pod name
     * @return A {@link Pod} instance
     */
    @Get("/namespaces/{namespace}/pods/{podName}")
    Publisher<Pod> getPod(String namespace, String podName);

    /**
     * List pods in the given namespace.
     *
     * @param namespace     Object name and auth scope, such as for teams and projects
     * @param labelSelector A selector to restrict the list of returned objects by their labels
     * @return a {@link PodList} of the given namespace
     */
    @Get("/namespaces/{namespace}/pods?labelSelector={labelSelector}")
    Publisher<PodList> listPods(String namespace, @Nullable String labelSelector);

    /**
     * @param namespace object name and auth scope, such as for teams and projects
     * @return a {@link PodList} of the given namespace
     */
    default Publisher<PodList> listPods(String namespace) {
        return listPods(namespace, null);
    }

    /**
     * Returns the created {@link Pod} in the given namespace.
     * @param namespace object name and auth scope, such as for teams and projects
     * @param pod the pod to create
     * @return A {@link Pod} instance
     */
    @Post("/namespaces/{namespace}/pods")
    Publisher<Pod> createPod(String namespace, @Body Pod pod);

    /**
     * Returns the deleted {@link Pod} in the given namespace.
     *
     * @param namespace object name and auth scope, such as for teams and projects
     * @param podName   the pod name
     * @return A {@link Pod} instance
     */
    @Delete("/namespaces/{namespace}/pods/{podName}")
    Publisher<Pod> deletePod(String namespace, String podName);
}
