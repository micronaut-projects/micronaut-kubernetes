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

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister;
import io.micronaut.kubernetes.client.openapi.operator.configuration.OperatorConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.ResourceReconciler;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.DefaultRateLimitingQueue;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.RateLimitingQueue;
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Default controller factory implementation.
 */
@Singleton
final class DefaultControllerFactory implements ControllerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultControllerFactory.class);

    private final SharedIndexInformerFactory sharedIndexInformerFactory;
    private final ThreadFactoryUtil threadFactoryUtil;
    private final OperatorConfiguration operatorConfiguration;
    private final MeterRegistry meterRegistry;
    private final ExecutorService executorService;

    private final AtomicBoolean starting = new AtomicBoolean(false);
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final Map<String, ControllerHolder<? extends KubernetesObject>> controllers = new ConcurrentHashMap<>();

    DefaultControllerFactory(SharedIndexInformerFactory sharedIndexInformerFactory,
                             ThreadFactoryUtil threadFactoryUtil,
                             OperatorConfiguration operatorConfiguration,
                             MeterRegistry meterRegistry) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.threadFactoryUtil = threadFactoryUtil;
        this.operatorConfiguration = operatorConfiguration;
        this.meterRegistry = meterRegistry;
        executorService = Executors.newCachedThreadPool(threadFactoryUtil.getNamedThreadFactory("controller-factory-%d"));
    }

    @Override
    public <ApiType extends KubernetesObject> Controller createController(
        Class<ApiType> apiTypeClass,
        Set<String> namespaces,
        ResourceReconciler<ApiType> resourceReconciler) {
        return createController(null, apiTypeClass, namespaces, resourceReconciler);
    }

    @Override
    public <ApiType extends KubernetesObject> Controller createController(
        String name,
        Class<ApiType> apiTypeClass,
        Set<String> namespaces,
        ResourceReconciler<ApiType> resourceReconciler) {
        return createController(name, apiTypeClass, namespaces, resourceReconciler, null);
    }

    @Override
    public <ApiType extends KubernetesObject> Controller createController(
        String name,
        Class<ApiType> apiTypeClass,
        Set<String> namespaces,
        ResourceReconciler<ApiType> resourceReconciler,
        RateLimitingQueue<Request> workQueue) {
        return createController(name, apiTypeClass, namespaces, resourceReconciler, workQueue, null, null, null);
    }

    @Override
    public <ApiType extends KubernetesObject> Controller createController(
        String name,
        Class<ApiType> apiTypeClass,
        Set<String> namespaces,
        ResourceReconciler<ApiType> resourceReconciler,
        RateLimitingQueue<Request> workQueue,
        Predicate<ApiType> onAddFilterPredicate,
        BiPredicate<ApiType, ApiType> onUpdateFilterPredicate,
        BiPredicate<ApiType, Boolean> onDeleteFilterPredicate) {

        if (apiTypeClass == null) {
            throw new IllegalArgumentException("The apiTypeClass must be provided");
        }

        String controllerName = StringUtils.isEmpty(name) ? "operator-" + apiTypeClass.getSimpleName().toLowerCase() : name;
        if (controllers.containsKey(controllerName)) {
            throw new IllegalStateException("Controller with name '" + controllerName + "' has already been created");
        }

        RateLimitingQueue<Request> controllerWorkQueue = workQueue == null ? createDefaultRateLimitingQueue() : workQueue;

        // create a resource event handler for each informer found by apiTypeClass and namespace
        Map<InformerKey<ApiType>, ControllerResourceEventHandler<ApiType>> resourceEventHandlers = new HashMap<>();
        if (CollectionUtils.isEmpty(namespaces)) {
            InformerKey<ApiType> informerKey = new InformerKey<>(apiTypeClass, null);
            resourceEventHandlers.put(informerKey, createHandler(informerKey, controllerWorkQueue, onAddFilterPredicate, onUpdateFilterPredicate, onDeleteFilterPredicate));
        } else {
            namespaces.forEach(namespace -> {
                InformerKey<ApiType> informerKey = new InformerKey<>(apiTypeClass, namespace);
                resourceEventHandlers.put(informerKey, createHandler(informerKey, controllerWorkQueue, onAddFilterPredicate, onUpdateFilterPredicate, onDeleteFilterPredicate));
            });
        }

        // create a new controller
        Controller controller = new DefaultController(
            controllerName,
            request -> resourceReconciler.reconcile(request, new OperatorResourceLister<>(sharedIndexInformerFactory, apiTypeClass, CollectionUtils.isEmpty(namespaces))),
            controllerWorkQueue,
            operatorConfiguration.getWorkerCount(),
            threadFactoryUtil,
            meterRegistry
        );

        // register the new controller together with resource event handlers which prepare events for the controller
        controllers.put(controllerName, new ControllerHolder<>(controller, resourceEventHandlers));

        return controller;
    }

    @Override
    public void startControllers() {
        if (starting.getAndSet(true)) {
            LOG.info("Controller startup already initiated");
            return;
        }
        stopping.set(false);
        LOG.info("Starting controllers");
        if (controllers.isEmpty()) {
            LOG.info("There are no registered controllers");
            return;
        }
        controllers.forEach((name, controllerHolder) -> {
            executorService.submit(() -> {
                LOG.trace("Starting controller {}", name);
                try {
                    startController(controllerHolder);
                } catch (Exception e) {
                    LOG.error("Failed to start controller {}", name, e);
                }
            });
        });
    }

    @Override
    public void stopControllers() {
        if (stopping.getAndSet(true)) {
            LOG.info("Controller shutdown already initiated");
            return;
        }
        starting.set(false);
        LOG.info("Stopping controllers");
        controllers.values().forEach(controllerHolder -> {
            controllerHolder.resourceEventHandlers.values().forEach(ControllerResourceEventHandler::disable);
            controllerHolder.controller.shutdown();
        });
    }

    @PreDestroy
    void destroy() {
        executorService.shutdownNow();
    }

    private DefaultRateLimitingQueue<Request> createDefaultRateLimitingQueue() {
        ExecutorService waitingWorker = Executors.newSingleThreadExecutor(threadFactoryUtil.getNamedThreadFactory("queue-waiting-worker-%d"));
        return new DefaultRateLimitingQueue<>(waitingWorker);
    }

    private <ApiType extends KubernetesObject> ControllerResourceEventHandler<ApiType> createHandler(
        InformerKey<ApiType> informerKey,
        RateLimitingQueue<Request> workQueue,
        Predicate<ApiType> onAddFilterPredicate,
        BiPredicate<ApiType, ApiType> onUpdateFilterPredicate,
        BiPredicate<ApiType, Boolean> onDeleteFilterPredicate) {

        SharedIndexInformer<ApiType> informer = getInformer(informerKey);
        if (informer == null) {
            throw new IllegalStateException("Not found informer for '" + informerKey);
        }
        ControllerResourceEventHandler<ApiType> handler = new ControllerResourceEventHandler<>(workQueue, onAddFilterPredicate, onUpdateFilterPredicate, onDeleteFilterPredicate);
        informer.addEventHandler(handler);
        return handler;
    }

    private <ApiType extends KubernetesObject> void startController(ControllerHolder<ApiType> controllerHolder) {
        Map<InformerKey<ApiType>, ControllerResourceEventHandler<ApiType>> resourceEventHandlers = controllerHolder.resourceEventHandlers;

        // wait on informers to get synced before enabling resource handlers and starting controller
        List<InformerKey<ApiType>> notSyncedInformers = getNotSyncedInformers(resourceEventHandlers);
        long waitLimit = System.currentTimeMillis() + operatorConfiguration.getReadyTimeout().toMillis();
        while (!notSyncedInformers.isEmpty() && waitLimit > System.currentTimeMillis()) {
            try {
                long checkInterval = operatorConfiguration.getReadyCheckInternal().toMillis();
                LOG.trace("Waiting {}ms on informer to get synced", checkInterval);
                Thread.sleep(checkInterval);
            } catch (InterruptedException e) {
                LOG.error("The thread waiting on informers to be synced has been interrupted. Controller '{}' has not been started",
                    controllerHolder.controller.getName(),
                    e);
                Thread.currentThread().interrupt();
                break;
            }
            notSyncedInformers = getNotSyncedInformers(resourceEventHandlers);
        }

        if (!notSyncedInformers.isEmpty()) {
            LOG.error("Timed out waiting on following informers to be synced: {}. Controller '{}' has not been started. " +
                    "Consider increasing 'kubernetes.client.operator.ready-timeout' value",
                notSyncedInformers, controllerHolder.controller.getName());
            return;
        }

        // all informers are synced so start controller, enable event handlers and resend all events
        controllerHolder.controller.run();
        for (InformerKey<ApiType> informerKey : resourceEventHandlers.keySet()) {
            resourceEventHandlers.get(informerKey).enable();
            getInformer(informerKey).resyncListeners();
        }
    }

    private <ApiType extends KubernetesObject> List<InformerKey<ApiType>> getNotSyncedInformers(
        Map<InformerKey<ApiType>, ControllerResourceEventHandler<ApiType>> resourceEventHandlers) {

        List<InformerKey<ApiType>> notSyncedInformers = new ArrayList<>();
        resourceEventHandlers.forEach((informerKey, resourceEventHandler) -> {
            SharedIndexInformer<ApiType> informer = getInformer(informerKey);
            if (informer != null && !informer.hasSynced()) {
                notSyncedInformers.add(informerKey);
            }
        });
        return notSyncedInformers;
    }

    private <ApiType extends KubernetesObject> SharedIndexInformer<ApiType> getInformer(InformerKey<ApiType> informerKey) {
        return sharedIndexInformerFactory.getExistingSharedIndexInformer(informerKey.apiTypeClass, informerKey.namespace);
    }

    private record ControllerHolder<ApiType extends KubernetesObject> (
        @NonNull Controller controller,
        @NonNull Map<InformerKey<ApiType>, ControllerResourceEventHandler<ApiType>> resourceEventHandlers
    ) {
    }

    private record InformerKey<ApiType extends KubernetesObject> (
        @NonNull Class<ApiType> apiTypeClass,
        @Nullable String namespace
    ) {
    }
}
