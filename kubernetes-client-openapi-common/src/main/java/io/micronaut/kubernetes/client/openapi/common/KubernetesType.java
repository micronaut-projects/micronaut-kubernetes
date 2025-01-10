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
package io.micronaut.kubernetes.client.openapi.common;

/**
 * Equivalence to TypeMeta from kubernetes/client-go.
 *
 * <p>TypeMeta describes an individual object in an API response or request with strings
 * representing the type of the object and its API schema version. Structures that are versioned or
 * persisted should inline TypeMeta.
 *
 * <p>
 * This has been taken from the official client:
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.2/kubernetes/src/main/java/io/kubernetes/client/common/KubernetesType.java">KubernetesType</a>
 * </p>
 */
public interface KubernetesType {

    /**
     * Gets api version.
     *
     * <p>APIVersion defines the versioned schema of this representation of an object. Servers should
     * convert recognized schemas to the latest internal value, and may reject unrecognized values.
     * More info: <a href="https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#resources">Resources</a>
     *
     * @return the api version
     */
    String getApiVersion();

    /**
     * Gets kind.
     *
     * <p>Kind is a string value representing the REST resource this object represents. Servers may
     * infer this from the endpoint the client submits requests to. Cannot be updated. In CamelCase.
     * More info: <a href="https://git.k8s.io/community/contributors/devel/sig-architecture/api-conventions.md#types-kinds">Types (Kinds)</a>
     *
     * @return the kind
     */
    String getKind();
}
