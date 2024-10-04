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

import io.micronaut.core.annotation.Nullable;

/**
 * AuthInfo contains information that describes identity information.
 *
 * @param clientCertificateData the PEM-encoded data from a client cert file for TLS
 * @param clientKeyData the PEM-encoded data from a client key file for TLS
 * @param token the bearer token for authentication to the kubernetes cluster
 * @param username the username for basic authentication to the kubernetes cluster
 * @param password the password for basic authentication to the kubernetes cluster
 * @param exec the custom exec-based authentication plugin for the kubernetes cluster
 */
public record AuthInfo(
    @Nullable byte[] clientCertificateData,
    @Nullable byte[] clientKeyData,
    @Nullable String token,
    @Nullable String username,
    @Nullable String password,
    @Nullable ExecConfig exec
) {
}
