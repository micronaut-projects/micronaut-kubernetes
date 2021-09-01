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
package io.micronaut.kubernetes.client.informer;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts up and shuts down the {@link SharedIndexInformerFactory}.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
@Requires(beans = SharedIndexInformerFactory.class)
@Singleton
public class InformerFactoryLifecycleListener {

    private static final Logger LOG = LoggerFactory.getLogger(InformerFactoryLifecycleListener.class);

    private final List<ResourceEventHandler<? extends KubernetesObject>> handlerList;
    private final SharedIndexInformerFactory sharedSharedIndexInformerFactory;
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Creates the {@link InformerFactoryLifecycleListener}. All of the {@link ResourceEventHandler}s are intentionally
     * requested in order to initialize the {@link io.kubernetes.client.informer.SharedIndexInformer}s and receive the
     * notifications to the handlers.
     *
     * @param handlerList handler list
     * @param sharedSharedIndexInformerFactory informer factory
     */
    public InformerFactoryLifecycleListener(List<ResourceEventHandler<? extends KubernetesObject>> handlerList, SharedIndexInformerFactory sharedSharedIndexInformerFactory) {
        this.handlerList = handlerList;
        this.sharedSharedIndexInformerFactory = sharedSharedIndexInformerFactory;
    }

    /**
     * Start informer factory on startup event.
     *
     * @param startupEvent startup event
     */
    @EventListener
    public void startInformerFactoryOnStartupEvent(StartupEvent startupEvent) {
        startInformers();
        started.set(true);
    }

    /**
     * Start informer factory for informers created after the bean context startup.
     *
     * @param createdEvent informer created event
     */
    @EventListener
    public void startInformerFactoryOnInformerCreatedEvent(InformerCreatedEvent createdEvent) {
        if (started.get()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("New informer created after the bean context startup, starting new informer.");
            }
            startInformers();
        }
    }

    /**
     * Start registered informers.
     */
    public void startInformers() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting shared informer factory");
        }
        sharedSharedIndexInformerFactory.startAllRegisteredInformers();
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
        sharedSharedIndexInformerFactory.stopAllRegisteredInformers();
    }
}
