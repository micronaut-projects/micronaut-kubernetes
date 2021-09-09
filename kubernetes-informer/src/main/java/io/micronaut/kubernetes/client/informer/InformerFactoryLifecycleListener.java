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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.annotation.Internal;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts up and shuts down the {@link SharedIndexInformerFactory}.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
@Requires(beans = SharedIndexInformerFactory.class)
@Singleton
@Internal
public class InformerFactoryLifecycleListener {

    private static final Logger LOG = LoggerFactory.getLogger(InformerFactoryLifecycleListener.class);

    private final SharedIndexInformerFactory sharedSharedIndexInformerFactory;

    /**
     * Creates the {@link InformerFactoryLifecycleListener}.
     *
     * @param sharedSharedIndexInformerFactory informer factory
     */
    public InformerFactoryLifecycleListener(SharedIndexInformerFactory sharedSharedIndexInformerFactory) {
        this.sharedSharedIndexInformerFactory = sharedSharedIndexInformerFactory;
    }

    /**
     * Start informer factory on startup event.
     *
     * @param startupEvent startup event
     */
    @EventListener
    public void startInformerFactoryOnStartupEvent(StartupEvent startupEvent) {
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
