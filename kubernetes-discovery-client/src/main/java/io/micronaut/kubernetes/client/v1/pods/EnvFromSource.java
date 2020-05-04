/*
 * Copyright 2017-2019 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.kubernetes.client.v1.pods;

import io.micronaut.core.annotation.Introspected;

/**
 * A source of environment variables for a container.
 *
 * @author <Miguel Ferreira>
 */
@Introspected
public class EnvFromSource {
    private ConfigMapEnvSource configMapRef;
    private SecretEnvSource secretRef;

    /**
     * @return The ConfigMap to select from
     */
    public ConfigMapEnvSource getConfigMapRef() {
        return configMapRef;
    }

    /**
     * @param configMapRef The ConfigMap to select from
     */
    public void setConfigMapRef(final ConfigMapEnvSource configMapRef) {
        this.configMapRef = configMapRef;
    }

    /**
     * @return The Secret to select from
     */
    public SecretEnvSource getSecretRef() {
        return secretRef;
    }

    /**
     * @param secretRef The Secret to select from
     */
    public void setSecretRef(final SecretEnvSource secretRef) {
        this.secretRef = secretRef;
    }

    @Override
    public String toString() {
        return "EnvFromSource{" +
                "configMapRef=" + configMapRef +
                ", secretRef=" + secretRef +
                '}';
    }
}
