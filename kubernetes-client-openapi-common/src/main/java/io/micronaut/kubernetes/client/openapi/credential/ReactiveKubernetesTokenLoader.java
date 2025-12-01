/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.kubernetes.client.openapi.credential;

import org.jspecify.annotations.NonNull;
import io.micronaut.core.async.annotation.SingleResult;
import org.reactivestreams.Publisher;

/**
 * The loader for bearer token used in kubernetes api service authentication.
 */
public interface ReactiveKubernetesTokenLoader extends TokenLoader {

    /**
     * Gets a bearer token for request authentication.
     *
     * @return bearer token
     */
    @SingleResult
    @NonNull Publisher<String> getToken();
}
