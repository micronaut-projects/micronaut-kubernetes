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
package io.micronaut.kubernetes.client.openapi.configuration;

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.kubernetes.client.openapi.KubernetesConfiguration;

/**
 * Condition evaluates when the {@link AbstractKubernetesConfigWatcherCondition} is enabled.
 *
 * @author Pavol Gressa
 */
abstract sealed class AbstractKubernetesConfigWatcherCondition implements Condition
    permits KubernetesConfigMapWatcherCondition, KubernetesSecretWatcherCondition  {

    @Override
    public boolean matches(ConditionContext context) {
        KubernetesConfiguration.AbstractConfigConfiguration configConfiguration = getConfig(context);
        String propertyPrefix = getPropertyPrefix();

        if (isExplicitImportEnabled()) {
            context.fail("explicit config import disables legacy watcher for '" + propertyPrefix + "'");
            return false;
        }

        if (!configConfiguration.isEnabled()) {
            context.fail("configuration client is disabled for '" + propertyPrefix + "'");
            return false;
        }

        if (!configConfiguration.isWatch()) {
            context.fail("watching functionality is disabled for '" + propertyPrefix + "'");
            return false;
        }

        if (!configConfiguration.getPaths().isEmpty() && !configConfiguration.isUseApi()) {
            context.fail("mounted volume paths are specified and use api is disabled for '" + propertyPrefix + "'");
            return false;
        }

        return true;
    }

    abstract KubernetesConfiguration.AbstractConfigConfiguration getConfig(ConditionContext context);

    abstract String getPropertyPrefix();

    abstract boolean isExplicitImportEnabled();
}
