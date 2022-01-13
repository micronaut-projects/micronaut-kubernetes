/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.client;

import jakarta.inject.Singleton;

import java.util.Optional;

/**
 * The default implementation of {@link PodNameResolver}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Singleton
public class DefaultPodNameResolver implements PodNameResolver {

    private static final String HOSTNAME_ENV_VARIABLE = "HOSTNAME";

    @Override
    public Optional<String> getPodName() {
        return Optional.ofNullable(System.getenv(HOSTNAME_ENV_VARIABLE));
    }
}
