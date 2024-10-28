/*
 * Copyright 2017-2021 original authors
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
package io.micronaut.kubernetes.configuration;

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.kubernetes.KubernetesConfiguration;

/**
 * Condition evaluates when the {@link AbstractKubernetesConfigWatcherCondition} is enabled.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
public abstract class AbstractKubernetesConfigWatcherCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context) {
        final KubernetesConfiguration.AbstractConfigConfiguration configMapsConfiguration =
            getConfig(context);

        if (!configMapsConfiguration.isEnabled()) {
            context.fail("configuration client for the ConfigMaps is disabled");
            return false;
        }

        if (!configMapsConfiguration.isWatch()) {
            context.fail("watch for the ConfigMap changes is disabled");
            return false;
        }

        if (!configMapsConfiguration.getPaths().isEmpty() && !configMapsConfiguration.isUseApi()) {
            context.fail("config maps paths configuration for mounted volumes is specified and use api is disabled");
            return false;
        }

        return true;
    }

    abstract KubernetesConfiguration.AbstractConfigConfiguration getConfig(ConditionContext context);
}
