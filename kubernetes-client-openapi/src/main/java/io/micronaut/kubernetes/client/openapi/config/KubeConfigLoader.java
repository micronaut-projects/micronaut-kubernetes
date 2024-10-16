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

import io.micronaut.core.annotation.Nullable;

/**
 * The loader for kube config file.
 */
public interface KubeConfigLoader {

    /**
     * Returns {@link KubeConfig} instance which contains data from the kube config file. The method is
     * called multiple times during the application context startup so the {@link KubeConfig} instance
     * should be created when the method is called for the first time, cached and then returned by subsequent calls.
     * Since it is called only in the context startup, it doesn't require thread synchronization.
     *
     * @return kube config
     */
    @Nullable KubeConfig getKubeConfig();
}
