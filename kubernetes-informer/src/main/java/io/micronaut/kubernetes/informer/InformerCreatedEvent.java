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
package io.micronaut.kubernetes.informer;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.SharedInformer;
import io.micronaut.context.event.ApplicationEvent;

/**
 * An event fired when {@link io.kubernetes.client.informer.SharedInformer} is created after the bean
 * context startup.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
public class InformerCreatedEvent extends ApplicationEvent {
    public InformerCreatedEvent(SharedInformer<? extends KubernetesObject> source) {
        super(source);
    }

    @Override
    public SharedInformer<? extends KubernetesObject> getSource() {
        return (SharedInformer<? extends KubernetesObject>) super.getSource();
    }
}
