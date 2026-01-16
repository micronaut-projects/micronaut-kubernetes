/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.kubernetes.client.openapi.watcher.mapper;

import io.micronaut.core.order.Ordered;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * Provides mapping of kubernetes list type names to theirs item type names.
 *
 * <p>
 * The main purpose is to provide mapping for cases when a kubernetes item type name cannot be derived
 * from a kubernetes list type name by removing the {@code List} suffix. For example, we can get
 * the item type name {@code V1Secret} from the list type name {@code V1SecretList} by removing
 * the {@code List} suffix, but that can't be applied to {@code V1ResourceClaimList} list type name
 * to get its {@code ResourceV1ResourceClaim} item type name.
 * </p>
 *
 * @since 8.0
 */
public interface TypeNameMapper extends Ordered {

    /**
     * Gets mapping of kubernetes list type names to theirs item type names.
     *
     * @return list type names to item type names mapping
     */
    @NonNull
    Map<String, String> getMappings();
}
