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
package io.micronaut.kubernetes.imports;

import io.micronaut.core.annotation.Internal;
import io.micronaut.kubernetes.discovery.client.core.imports.AbstractKubernetesImport;

import java.util.Map;

/**
 * Typed ConfigMap import declaration for the classic Kubernetes discovery client.
 *
 * @param namespace The resolved namespace
 * @param name The exact ConfigMap name, or {@code null} when importing by labels
 * @param labels The label selector map, or {@code null} when importing by exact name
 * @param optional Whether the import is optional
 * @since 8.0.0
 */
@Internal
record KubernetesConfigMapImport(String namespace,
                                 String name,
                                 Map<String, String> labels,
                                 boolean optional) implements AbstractKubernetesImport {
}
