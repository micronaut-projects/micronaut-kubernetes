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
package io.micronaut.kubernetes.client.openapi;

import org.gradle.api.provider.Property;

/**
 * Configuration for {@link KubernetesClientOpenApiPlugin}.
 */
public interface KubernetesClientOpenApiExtension {

    /**
     * The url of the kubernetes client OpenAPI spec. If set, it overrides the url created from the spec version.
     *
     * @return the url of the kubernetes client OpenAPI spec
     */
    Property<String> getSpecUrl();

    /**
     * The kubernetes client version used for creating url of the kubernetes client OpenAPI spec.
     *
     * @return the kubernetes client version
     */
    Property<String> getSpecVersion();

    /**
     * The new name for the downloaded OpenApi spec file.
     *
     * @return the new name for the downloaded OpenApi spec file
     */
    Property<String> getFileName();
}
