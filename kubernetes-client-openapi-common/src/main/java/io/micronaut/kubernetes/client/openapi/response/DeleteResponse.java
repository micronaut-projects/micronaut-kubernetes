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
package io.micronaut.kubernetes.client.openapi.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jspecify.annotations.Nullable;
import io.micronaut.kubernetes.client.openapi.model.V1Status;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Response which is used by kubernetes client delete methods can contain {@link V1Status} instance
 * or an instance of the kubernetes object which is being deleted depending on kubernetes api server configuration.
 *
 * @param <T> the object type
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind", defaultImpl = ResourceDeleteResponse.class, visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StatusDeleteResponse.class, name = "Status")
})
@Serdeable
public sealed interface DeleteResponse<T> permits StatusDeleteResponse, ResourceDeleteResponse {
    @Nullable
    T object();

    @Nullable
    V1Status status();
}
