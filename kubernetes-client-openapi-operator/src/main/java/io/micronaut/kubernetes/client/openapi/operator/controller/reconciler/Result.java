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

import io.micronaut.core.annotation.Nullable;

import java.time.Duration;

/**
 * The type Result contains the result of a Reconciler invocation.
 *
 * @param requeue      the info which determines whether the processing request should be returned to the queue
 * @param requeueAfter the info which determines when the processing request should be returned to the queue
 */
public record Result(
    boolean requeue,
    @Nullable Duration requeueAfter
) {
    public Result(boolean requeue) {
        this(requeue, null);
    }
}
