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

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.extended.controller.ControllerWatch;
import io.kubernetes.client.extended.controller.Controllers;
import io.kubernetes.client.extended.controller.DefaultController;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.RateLimitingQueue;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.kubernetes.client.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.operator.OperatorResourceLister;
import io.micronaut.kubernetes.client.operator.ResourceReconciler;
import io.micronaut.kubernetes.client.operator.ControllerConfiguration;
import io.micronaut.kubernetes.client.operator.configuration.OperatorConfigurationProperties;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Default implementation of the {@link ControllerBuilder}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Singleton
@Internal
public class DefaultControllerBuilder implements ControllerBuilder {
    public static final Logger LOG = LoggerFactory.getLogger(DefaultControllerBuilder.class);

    private final BeanContext beanContext;
    private final ControllerWatchBuilder controllerWatchBuilder;
    private final SharedIndexInformerFactory sharedIndexInformerFactory;
    private final OperatorConfigurationProperties operatorConfiguration;

    public DefaultControllerBuilder(
            @NonNull BeanContext beanContext,
            @NonNull ControllerWatchBuilder controllerWatchBuilder,
            @NonNull SharedIndexInformerFactory sharedIndexInformerFactory,
            @NonNull OperatorConfigurationProperties operatorConfiguration) {
        this.beanContext = beanContext;
        this.controllerWatchBuilder = controllerWatchBuilder;
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.operatorConfiguration = operatorConfiguration;
    }

    @NonNull
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DefaultController build(@NonNull ControllerConfiguration controllerConfiguration, @NonNull ResourceReconciler<?> resourceReconciler) {
        final Set<String> namespaces = controllerConfiguration.getNamespaces();
        final String name = controllerConfiguration.getName();

        final RateLimitingQueue<Request> workQueue = beanContext.createBean(RateLimitingQueue.class);
        beanContext.registerSingleton(RateLimitingQueue.class, workQueue, Qualifiers.byName(name));

        final ControllerWatch<? extends KubernetesObject> controllerWatch = controllerWatchBuilder.buildControllerWatch(controllerConfiguration, workQueue);
        final Set<Supplier<Boolean>> readyFuncs = new HashSet<>(namespaces.size());

        if (LOG.isInfoEnabled()) {
            LOG.info("Creating controller for " + controllerConfiguration.getName());
        }

        namespaces.forEach(namespace -> {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Creating controller[" + name + "] informer in namespace " + namespace);
            }
            SharedIndexInformer<? extends KubernetesObject> informer = sharedIndexInformerFactory.sharedIndexInformerFor(
                    controllerConfiguration.getApiType(),
                    controllerConfiguration.getApiListType(),
                    controllerConfiguration.getResourcePlural(),
                    controllerConfiguration.getApiGroup(),
                    namespace,
                    controllerConfiguration.getLabelSelector(),
                    controllerConfiguration.getResyncCheckPeriod(),
                    false);
            informer.addEventHandler((ResourceEventHandler) controllerWatch.getResourceEventHandler());
            readyFuncs.add(informer::hasSynced);
        });

        final DefaultController controller = new DefaultController(
                controllerConfiguration.getName(),
                request ->
                        resourceReconciler.reconcile(request, new OperatorResourceLister<>(controllerConfiguration, sharedIndexInformerFactory)),
                workQueue,
                readyFuncs.toArray(new Supplier[0]));

        operatorConfiguration.getReadyTimeout().ifPresent(controller::setReadyTimeout);
        int workerCount = operatorConfiguration.getWorkerCount();
        controller.setWorkerCount(workerCount);
        controller.setWorkerThreadPool(
                Executors.newScheduledThreadPool(
                        workerCount, Controllers.namedControllerThreadFactory(name + "-controller")));

        return controller;
    }
}
