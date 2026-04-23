/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.kubernetes.configuration.imports;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Describes a single Kubernetes config import declaration.
 *
 * @param type                        The Kubernetes resource type, either {@code config-map} or {@code secret}
 * @param name                        The resource name to query directly
 * @param labels                      The label selector used to match resources
 * @param podLabels                   Pod label keys used to derive a selector from the current pod
 * @param watch                       Whether property source should be recreated when kubernetes resource gets modified
 * @param exceptionOnPodLabelsMissing Whether missing pod labels should raise an exception
 * @param terminateStartupOnException Whether import failures should terminate application startup
 *
 * @since 8.0.0
 */
public record ImportDeclaration(
    @NonNull String type,
    @Nullable String name,
    @Nullable Map<String, String> labels,
    @Nullable List<String> podLabels,
    boolean watch,
    boolean exceptionOnPodLabelsMissing,
    boolean terminateStartupOnException) {
}
