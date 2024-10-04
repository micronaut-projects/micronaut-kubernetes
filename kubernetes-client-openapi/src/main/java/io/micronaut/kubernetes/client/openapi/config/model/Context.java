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
package io.micronaut.kubernetes.client.openapi.config.model;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

/**
 * Context is a tuple of references to a cluster, a user and a namespace.
 *
 * @param cluster the name of the cluster for this context
 * @param user the name of the authInfo for this context
 * @param namespace the default namespace to use on unspecified requests
 */
public record Context(
    @NonNull String cluster,
    @NonNull String user,
    @Nullable String namespace
) {
}
