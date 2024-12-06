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
package io.micronaut.kubernetes.client.openapi.watcher;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.kubernetes.client.openapi.model.V1Status;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Holder for deserialized kubernetes streamed event.
 *
 * @param type the type of event (ADDED, MODIFIED, DELETED, ERROR, BOOKMARK)
 * @param object the object
 * @param status the instance of V1Status
 * @param <T> the object type
 */
@Serdeable
public record WatchEvent<T>(
    @NonNull String type,
    @Nullable T object,
    @Nullable V1Status status
) {
}
