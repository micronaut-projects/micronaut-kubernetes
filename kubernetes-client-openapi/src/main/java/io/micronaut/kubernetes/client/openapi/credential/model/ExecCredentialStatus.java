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
package io.micronaut.kubernetes.client.openapi.credential.model;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import java.time.ZonedDateTime;

/**
 * Holds credentials for the transport to use.
 *
 * @param token the bearer token used by the client for request authentication
 * @param clientCertificateData the PEM-encoded client TLS certificates (including intermediates, if any)
 * @param clientKeyData the PEM-encoded private key for the above certificate
 * @param expirationTimestamp the time when the provided credentials expire
 */
@Serdeable.Deserializable
public record ExecCredentialStatus(
    @NonNull String token,
    @Nullable byte[] clientCertificateData,
    @Nullable byte[] clientKeyData,
    @Nullable ZonedDateTime expirationTimestamp
) {
}
