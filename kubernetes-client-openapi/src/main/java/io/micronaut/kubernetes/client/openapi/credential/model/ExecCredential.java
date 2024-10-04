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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

/**
 * ExecCredential is used by exec-based plugins to communicate credentials to HTTP transports.
 *
 * @param apiVersion the api version
 * @param kind the kind of exec credential
 * @param status the holder for credentials that should be used to contact the API
 */
@Serdeable.Deserializable
public record ExecCredential(
    @Nullable String apiVersion,
    @Nullable String kind,
    @Nullable ExecCredentialStatus status
) {
}
