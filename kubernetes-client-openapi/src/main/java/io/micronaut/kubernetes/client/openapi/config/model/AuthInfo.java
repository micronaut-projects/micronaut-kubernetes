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

import java.util.Arrays;
import java.util.Objects;

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
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthInfo authInfo = (AuthInfo) o;
        return Arrays.equals(clientCertificateData, authInfo.clientCertificateData)
            && Arrays.equals(clientKeyData, authInfo.clientKeyData)
            && Objects.equals(token, authInfo.token)
            && Objects.equals(username, authInfo.username)
            && Objects.equals(password, authInfo.password)
            && Objects.equals(exec, authInfo.exec);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(token, username, password, exec);
        result = 31 * result + Arrays.hashCode(clientCertificateData);
        result = 31 * result + Arrays.hashCode(clientKeyData);
        return result;
    }

    @Override
    public String toString() {
        return "AuthInfo{" +
            "clientCertificateData=" + Arrays.toString(clientCertificateData) +
            ", clientKeyData=" + Arrays.toString(clientKeyData) +
            ", token='" + token + '\'' +
            ", username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", exec=" + exec +
            '}';
    }
}
