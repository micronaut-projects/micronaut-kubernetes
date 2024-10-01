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
package io.micronaut.kubernetes.client.openapi.config;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;

/**
 * Cluster contains information about how to communicate with a kubernetes cluster.
 *
 * @param server the address of the kubernetes cluster
 * @param certificateAuthorityData the PEM-encoded certificate authority certificates
 * @param insecureSkipTlsVerify skips the validity check for the server's certificate which makes your HTTPS connections insecure
 */
public record Cluster(
    @NonNull String server,
    @Nullable byte[] certificateAuthorityData,
    @Nullable Boolean insecureSkipTlsVerify
) {
}
