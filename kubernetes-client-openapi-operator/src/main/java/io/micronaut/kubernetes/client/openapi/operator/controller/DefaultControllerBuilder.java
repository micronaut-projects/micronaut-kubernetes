package io.micronaut.kubernetes.client.openapi.operator.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.informer.handler.ResourceEventHandler;
import io.micronaut.kubernetes.client.openapi.operator.OperatorResourceLister;
import io.micronaut.kubernetes.client.openapi.operator.ResourceReconciler;
import io.micronaut.kubernetes.client.openapi.operator.configuration.OperatorConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.controller.watch.ControllerWatch;
import io.micronaut.kubernetes.client.openapi.operator.controller.watch.ControllerWatchBuilder;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.RateLimitingQueue;
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Default implementation of the {@link ControllerBuilder}.
 *
 * @author Pavol Gressa
 */
@Singleton
final class DefaultControllerBuilder implements ControllerBuilder {
    public static final Logger LOG = LoggerFactory.getLogger(DefaultControllerBuilder.class);

    private final BeanContext beanContext;
    private final ControllerWatchBuilder controllerWatchBuilder;
    private final SharedIndexInformerFactory sharedIndexInformerFactory;
    private final OperatorConfiguration operatorConfiguration;
    private final MeterRegistry meterRegistry;
    private final ThreadFactoryUtil threadFactoryUtil;

    DefaultControllerBuilder(@NonNull BeanContext beanContext,
                             @NonNull ControllerWatchBuilder controllerWatchBuilder,
                             @NonNull SharedIndexInformerFactory sharedIndexInformerFactory,
                             @NonNull OperatorConfiguration operatorConfiguration,
                             MeterRegistry meterRegistry,
                             ThreadFactoryUtil threadFactoryUtil) {
        this.beanContext = beanContext;
        this.controllerWatchBuilder = controllerWatchBuilder;
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.operatorConfiguration = operatorConfiguration;
        this.meterRegistry = meterRegistry;
        this.threadFactoryUtil = threadFactoryUtil;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Controller build(ControllerConfiguration controllerConfiguration, ResourceReconciler<?> resourceReconciler, RateLimitingQueue<Request> workQueue) {
        Set<String> namespaces = controllerConfiguration.getNamespaces();
        String name = controllerConfiguration.getName();

        LOG.info("Creating controller for {}", controllerConfiguration.getName());

        ControllerWatch<? extends KubernetesObject> controllerWatch = controllerWatchBuilder.buildControllerWatch(controllerConfiguration, workQueue);
        List<Supplier<Boolean>> readyFuncs = new ArrayList<>(namespaces.size());
        namespaces.forEach(namespace -> {
            LOG.trace("Creating controller[{}] informer in namespace {}", name, namespace);

            SharedIndexInformer<? extends KubernetesObject> informer = sharedIndexInformerFactory.sharedIndexInformerFor(
                controllerConfiguration.getApiType(),
                namespace,
                controllerConfiguration.getLabelSelector(),
                false,
                controllerConfiguration.getResyncCheckPeriod());

            informer.addEventHandler((ResourceEventHandler) controllerWatch.getResourceEventHandler());
            readyFuncs.add(informer::hasSynced);
        });

        return new DefaultController(
            controllerConfiguration.getName(),
            request -> resourceReconciler.reconcile(request, new OperatorResourceLister<>(controllerConfiguration, sharedIndexInformerFactory)),
            workQueue,
            operatorConfiguration.getWorkerCount(),
            threadFactoryUtil,
            meterRegistry,
            readyFuncs,
            operatorConfiguration.getReadyTimeout().orElse(Duration.ofSeconds(30)),
            operatorConfiguration.getReadyCheckInternal().orElse(Duration.ofSeconds(1)));
    }
}
