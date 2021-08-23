/*
 * Copyright 2021 original authors
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

import io.kubernetes.client.informer.SharedInformerFactory;
import io.micronaut.core.annotation.Internal;
import io.micronaut.discovery.event.ServiceReadyEvent;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerShutdownEvent;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pavol Gressa
 * @since 2.5
 */
@Singleton
@Internal
public class SharedInformerListener {

    private static final Logger LOG = LoggerFactory.getLogger(SharedInformerListener.class);
    private final boolean isService;
    private final SharedInformerFactory sharedInformerFactory;

    /**
     * Default constructor.
     *
     * @param applicationConfiguration The application configuration.
     */
    @Internal
    protected SharedInformerListener(ApplicationConfiguration applicationConfiguration, SharedInformerFactory sharedInformerFactory) {
        this.isService = applicationConfiguration.getName().isPresent();
        this.sharedInformerFactory = sharedInformerFactory;
    }

    /**
     * Event listener triggered when a service is ready.
     *
     * @param event The event
     */
    @EventListener
    void onServiceStarted(ServiceReadyEvent event) {
        startInformerFactory();
    }

    /**
     * Event listener triggered when the server starts up.
     *
     * @param event The event
     */
    @EventListener
    void onServerStarted(ServerStartupEvent event) {
        if (!isService) {
            startInformerFactory();
        }
    }

    @EventListener
    void stopInformerFactory(ServerShutdownEvent shutdownEvent) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Stopping shared informer factory");
        }
        sharedInformerFactory.stopAllRegisteredInformers(false);
    }

    private void startInformerFactory() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Starting shared informer factory");
        }
        sharedInformerFactory.startAllRegisteredInformers();
    }
}
