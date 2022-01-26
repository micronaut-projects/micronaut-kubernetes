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
package io.micronaut.kubernetes.client.operator.controller;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.extended.controller.LeaderElectingController;
import io.kubernetes.client.extended.leaderelection.LeaderElectionConfig;
import io.kubernetes.client.extended.leaderelection.LeaderElector;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.operator.ResourceReconciler;
import io.micronaut.kubernetes.client.operator.ControllerConfiguration;
import io.micronaut.kubernetes.client.operator.event.LeaseAcquiredEvent;
import io.micronaut.kubernetes.client.operator.event.LeaseLostEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link LeaderElectingControllerBuilder}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Singleton
public class DefaultLeaderElectingControllerBuilder implements LeaderElectingControllerBuilder {

    public static final Logger LOG = LoggerFactory.getLogger(DefaultControllerManagerBuilder.class);

    private final LeaderElectionConfig leaderElectionConfig;
    private final ApplicationEventPublisher<LeaseLostEvent> leaseLostEventApplicationEventPublisher;
    private final ApplicationEventPublisher<LeaseAcquiredEvent> leaseAcquiredEventApplicationEventPublisher;

    public DefaultLeaderElectingControllerBuilder(LeaderElectionConfig leaderElectionConfig, ApplicationEventPublisher<LeaseLostEvent> leaseLostEventApplicationEventPublisher, ApplicationEventPublisher<LeaseAcquiredEvent> leaseAcquiredEventApplicationEventPublisher) {
        this.leaderElectionConfig = leaderElectionConfig;
        this.leaseLostEventApplicationEventPublisher = leaseLostEventApplicationEventPublisher;
        this.leaseAcquiredEventApplicationEventPublisher = leaseAcquiredEventApplicationEventPublisher;
    }

    @Override
    @NonNull
    public LeaderElectingController build(@NonNull ControllerConfiguration operator,
                                          @NonNull ResourceReconciler<?> resourceReconciler,
                                          @NonNull ControllerManager controllerManager) {
        final LeaderElector leaderElector = new LeaderElector(leaderElectionConfig);
        final ApplicationContextEventEmitterController eventEmitterController = new ApplicationContextEventEmitterController(
                leaseLostEventApplicationEventPublisher,
                leaseAcquiredEventApplicationEventPublisher,
                controllerManager,
                operator);
        final LeaderElectingController leaderElectingController = new LeaderElectingController(leaderElector, eventEmitterController);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Created leader electing controller for operator: " + operator.getName());
        }
        return leaderElectingController;
    }

    static class ApplicationContextEventEmitterController implements Controller {

        private final ApplicationEventPublisher<LeaseLostEvent> leaseLostEventApplicationEventPublisher;
        private final ApplicationEventPublisher<LeaseAcquiredEvent> leaseAcquiredEventApplicationEventPublisher;
        private final ControllerManager delegateController;
        private final ControllerConfiguration controllerConfiguration;

        public ApplicationContextEventEmitterController(ApplicationEventPublisher<LeaseLostEvent> leaseLostEventApplicationEventPublisher,
                                                        ApplicationEventPublisher<LeaseAcquiredEvent> leaseAcquiredEventApplicationEventPublisher,
                                                        ControllerManager delegateController,
                                                        ControllerConfiguration controllerConfiguration) {
            this.leaseAcquiredEventApplicationEventPublisher = leaseAcquiredEventApplicationEventPublisher;
            this.leaseLostEventApplicationEventPublisher = leaseLostEventApplicationEventPublisher;
            this.delegateController = delegateController;
            this.controllerConfiguration = controllerConfiguration;
        }

        @Override
        public void shutdown() {
            leaseLostEventApplicationEventPublisher.publishEvent(new LeaseLostEvent(controllerConfiguration, delegateController));
            delegateController.shutdown();
        }

        @Override
        public void run() {
            leaseAcquiredEventApplicationEventPublisher.publishEvent(new LeaseAcquiredEvent(controllerConfiguration, delegateController));
            delegateController.run();
        }
    }
}
