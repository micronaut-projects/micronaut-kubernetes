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
package io.micronaut.kubernetes.client.openapi.operator.controller;

import io.micronaut.context.annotation.Requires;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.event.LeaseAcquiredEvent;
import io.micronaut.kubernetes.client.openapi.operator.leaderelection.event.LeaseLostEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages lifecycle of registered controllers using leader elector implementation.
 */
@Singleton
@Requires(beans = KubernetesClientConfiguration.class)
final class ControllersLifecycleListener {
    private static final Logger LOG = LoggerFactory.getLogger(ControllersLifecycleListener.class);

    private final ControllerFactory controllerFactory;

    ControllersLifecycleListener(ControllerFactory controllerFactory) {
        this.controllerFactory = controllerFactory;
    }

    @EventListener
    void startControllers(LeaseAcquiredEvent leaseAcquiredEvent) {
        LOG.info("Lease acquired, starting controllers");
        controllerFactory.startControllers();
    }

    @EventListener
    void stopControllers(LeaseLostEvent leaseLostEvent) {
        LOG.info("Lease lost, stopping controllers");
        controllerFactory.stopControllers();
    }
}
