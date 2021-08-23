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
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Informer context that starts up the {@link SharedInformerFactory}.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
@Requires(beans = SharedInformerFactory.class)
@Context
@BootstrapContextCompatible
public class InformerContext implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(InformerContext.class);

    private final List<ResourceEventHandler<? extends KubernetesObject>> handlerList;
    private final SharedInformerFactory sharedInformerFactory;

    public InformerContext(@Nullable List<ResourceEventHandler<? extends KubernetesObject>> handlerList, SharedInformerFactory sharedInformerFactory) {
        this.handlerList = handlerList;
        this.sharedInformerFactory = sharedInformerFactory;
        startInformerFactory();
    }

    private void startInformerFactory() {
        if (handlerList.isEmpty()) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("The SharedInformerFactory won't be started because there are no ResourceEventHandlers in the context");
            }
            return;
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("Starting shared informer factory");
        }
        sharedInformerFactory.startAllRegisteredInformers();
    }

    @PreDestroy
    @Override
    public void close() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Closing shared informer factory");
        }
        sharedInformerFactory.stopAllRegisteredInformers(false);
    }
}
