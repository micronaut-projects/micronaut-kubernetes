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
package io.micronaut.kubernetes.client.openapi.operator.controller.reconciler;

import org.jspecify.annotations.Nullable;

/**
 * Contains information necessary to uniquely identify the resource object.
 *
 * @param name      the name of the resource object.
 * @param namespace the namespace
 */
public record Request(
    String name,
    @Nullable String namespace
) {
}
