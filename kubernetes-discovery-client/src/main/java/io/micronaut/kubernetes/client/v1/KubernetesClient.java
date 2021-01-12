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

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.retry.annotation.Retryable;

/**
 * Provides a HTTP Client against Kubernetes API.
 *
 * {@link Client} implementation of {@link KubernetesOperations}.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
@Client(id = KubernetesClient.SERVICE_ID, path = "/api/v1")
@Requires(property = KubernetesConfiguration.PREFIX + ".enabled", notEquals = StringUtils.FALSE)
@BootstrapContextCompatible
@Retryable(attempts = "5", multiplier = "2.0")
public interface KubernetesClient extends KubernetesOperations {
    String SERVICE_ID = "kubernetes";
}
