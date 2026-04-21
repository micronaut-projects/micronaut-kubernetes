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
package io.micronaut.kubernetes.client.openapi.configuration.imports;

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.core.convert.ConversionContext;

import java.util.Optional;

abstract sealed class AbstractKubernetesConfigWatcherCondition implements Condition
    permits KubernetesConfigMapWatcherCondition, KubernetesSecretWatcherCondition {

    @Override
    public boolean matches(ConditionContext context) {
        Optional<Boolean> isImporterContext = context.getProperty(KubernetesPropertySourceImporter.KUBERNETES_IMPORTER_CONTEXT_PROP, ConversionContext.BOOLEAN);
        if (isImporterContext.isPresent() && isImporterContext.get()) {
            return false;
        }
        return isWatchEnabled();
    }

    abstract boolean isWatchEnabled();
}
