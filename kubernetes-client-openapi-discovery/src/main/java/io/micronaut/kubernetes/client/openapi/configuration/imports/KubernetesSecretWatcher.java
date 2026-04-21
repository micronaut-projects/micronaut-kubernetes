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

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import io.micronaut.kubernetes.client.openapi.reactor.api.CoreV1ApiReactor;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;

@Context
@Requires(env = Environment.KUBERNETES)
@Requires(beans = CoreV1ApiReactor.class)
@Requires(condition = KubernetesSecretWatcherCondition.class)
@Informer(apiType = V1Secret.class)
final class KubernetesSecretWatcher extends AbstractKubernetesConfigWatcher<V1Secret> {

    KubernetesSecretWatcher(Environment environment,
                            ApplicationEventPublisher<RefreshEvent> eventPublisher) {
        super(environment, eventPublisher);
    }
}
