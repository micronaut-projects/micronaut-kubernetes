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
package io.micronaut.kubernetes.client.openapi.informer;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ShutdownEvent;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts up and shuts down the {@link SharedIndexInformerFactory}.
 */
@Singleton
@Requires(beans = KubernetesClientConfiguration.class)
@Requires(property = InformerConfiguration.PREFIX + ".enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
final class InformerFactoryLifecycleListener {

    private static final Logger LOG = LoggerFactory.getLogger(InformerFactoryLifecycleListener.class);

    private final SharedIndexInformerFactory informerFactory;

    InformerFactoryLifecycleListener(SharedIndexInformerFactory informerFactory) {
        this.informerFactory = informerFactory;
    }

    /**
     * Start informer factory on startup event.
     *
     * @param startupEvent startup event
     */
    @EventListener
    void startInformerFactoryOnStartupEvent(StartupEvent startupEvent) {
        LOG.info("Starting all registered informers");
        informerFactory.startAllRegisteredInformers();
    }

    /**
     * Shutdown informer factory on shutdown event.
     *
     * @param shutdownEvent shutdown event
     */
    @EventListener
    void shutdown(ShutdownEvent shutdownEvent) {
        LOG.info("Stopping all registered informers on shutdown");
        informerFactory.stopAllRegisteredInformers();
    }
}
