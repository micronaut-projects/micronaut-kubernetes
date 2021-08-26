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
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.discovery.event.ServiceReadyEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Starts up and shuts down the {@link SharedInformerFactory}.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
@Requires(beans = SharedInformerFactory.class)
@Singleton
public class SharedInformerFactoryLifecycleListener {

    private static final Logger LOG = LoggerFactory.getLogger(SharedInformerFactoryLifecycleListener.class);

    private final List<ResourceEventHandler<? extends KubernetesObject>> handlerList;
    private final SharedInformerFactory sharedInformerFactory;

    public SharedInformerFactoryLifecycleListener(List<ResourceEventHandler<? extends KubernetesObject>> handlerList, SharedInformerFactory sharedInformerFactory) {
        this.handlerList = handlerList;
        this.sharedInformerFactory = sharedInformerFactory;
    }


    /**
     * Start informer factory on startup event.
     *
     * @param startupEvent startup event
     */
    @EventListener
    public void startInformerFactoryOnStartupEvent(StartupEvent startupEvent) {
        startInformers();
    }


    /**
     * Start informer factory on service ready event.
     *
     * @param serviceReadyEvent service ready event
     */
    @EventListener
    public void startInformerFactoryOnServiceReadyEvent(ServiceReadyEvent serviceReadyEvent) {
        startInformers();
    }

    private void startInformers() {
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

    /**
     * Shutdown informer factory on shutdown event.
     *
     * @param shutdownEvent shutdown event
     */
    @EventListener
    public void shutdown(ShutdownEvent shutdownEvent) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Closing shared informer factory on shutdown");
        }
        sharedInformerFactory.stopAllRegisteredInformers(false);
    }
}
