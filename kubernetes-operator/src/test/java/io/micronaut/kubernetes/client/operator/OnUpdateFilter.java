/*
 * Copyright 2021 original authors
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
package io.micronaut.kubernetes.client.operator;
//tag::reconciler[]
import io.kubernetes.client.openapi.models.V1ConfigMap;
import jakarta.inject.Singleton;

import java.util.function.BiPredicate;

@Singleton
public class OnUpdateFilter implements BiPredicate<V1ConfigMap, V1ConfigMap> {

    @Override
    public boolean test(V1ConfigMap oldObj, V1ConfigMap newObj) {
        if (newObj.getMetadata().getAnnotations() != null) {
            return newObj.getMetadata().getAnnotations().containsKey("io.micronaut.operator");
        }
        return false;
    }
}
//end::reconciler[]
