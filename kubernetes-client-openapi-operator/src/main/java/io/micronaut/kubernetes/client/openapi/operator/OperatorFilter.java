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
package io.micronaut.kubernetes.client.openapi.operator;

import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Default implementations of operator filters.
 */
final class OperatorFilter {

    static class OnAdd implements Predicate<KubernetesObject> {
        @Override
        public boolean test(KubernetesObject kubernetesObject) {
            return true;
        }
    }

    static class OnUpdate implements BiPredicate<KubernetesObject, KubernetesObject> {
        @Override
        public boolean test(KubernetesObject kubernetesObject, KubernetesObject kubernetesObject2) {
            return true;
        }
    }

    static class OnDelete implements BiPredicate<KubernetesObject, Boolean> {
        @Override
        public boolean test(KubernetesObject kubernetesObject, Boolean aBoolean) {
            return true;
        }
    }
}
