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
package io.micronaut.kubernetes.client.operator;

import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.extended.controller.DefaultController;
import io.kubernetes.client.extended.controller.LeaderElectingController;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.kubernetes.client.operator.controller.ControllerBuilder;
import io.micronaut.kubernetes.client.operator.controller.ControllerManagerBuilder;
import io.micronaut.kubernetes.client.operator.controller.LeaderElectingControllerBuilder;
import io.micronaut.scheduling.TaskExecutors;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * Bean creates the controllers based on the {@link ControllerConfiguration} and registers them as singletons into
 * the bean context.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Requires(beans = SharedInformerFactory.class)
@Singleton
@Internal
public class ControllerFactory {
    public static final Logger LOG = LoggerFactory.getLogger(ControllerFactory.class);

    private final BeanContext beanContext;
    private final ControllerBuilder controllerBuilder;
    private final ControllerManagerBuilder controllerManagerBuilder;
    private final LeaderElectingControllerBuilder leaderElectingControllerBuilder;
    private final ExecutorService executorService;

    public ControllerFactory(@NonNull BeanContext beanContext,
                             @NonNull ControllerBuilder controllerBuilder,
                             @NonNull ControllerManagerBuilder controllerManagerBuilder,
                             @NonNull LeaderElectingControllerBuilder leaderElectingControllerBuilder,
                             @Named(TaskExecutors.IO) ExecutorService executorService) {
        this.beanContext = beanContext;
        this.controllerBuilder = controllerBuilder;
        this.controllerManagerBuilder = controllerManagerBuilder;
        this.leaderElectingControllerBuilder = leaderElectingControllerBuilder;
        this.executorService = executorService;
    }

    /**
     * Create the controllers.
     *
     * @param reconciler              the resource reconciler
     * @param controllerConfiguration the controller configuration
     */
    @NonNull
    public void createControllers(@NonNull ResourceReconciler<?> reconciler, @NonNull ControllerConfiguration controllerConfiguration) {

        final String controllerName = controllerConfiguration.getName();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Creating controllers for " + controllerName + " operator ");
        }

        final DefaultController controller = controllerBuilder.build(controllerConfiguration, reconciler);
        beanContext.registerSingleton(DefaultController.class, controller, Qualifiers.byName(controllerName));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Injected DefaultController with @Named qualifier: " + controllerName + " to the bean context");
        }

        final ControllerManager controllerManager = controllerManagerBuilder.build(controllerConfiguration, Sets.newHashSet(controller));
        beanContext.registerSingleton(ControllerManager.class, controllerManager, Qualifiers.byName(controllerName));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Injected ControllerManager with @Named qualifier: " + controllerName + " to the bean context");
        }

        LeaderElectingController leaderElectingController = leaderElectingControllerBuilder.build(controllerConfiguration, reconciler, controllerManager);
        beanContext.registerSingleton(LeaderElectingController.class, leaderElectingController, Qualifiers.byName(controllerName));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Injected leaderElectingController with @Named qualifier: " + controllerName + " to the bean context");
        }

        executorService.execute(leaderElectingController);

        if (LOG.isInfoEnabled()) {
            LOG.info("@Operator name: " + controllerName + " for type: " + controllerConfiguration.getApiType() + " started");
        }
    }
}
