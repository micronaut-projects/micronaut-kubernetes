package io.micronaut.kubernetes.client.openapi.operator;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import io.micronaut.kubernetes.client.openapi.informer.handler.Informer;
import io.micronaut.kubernetes.client.openapi.informer.handler.InformerLabelSelectorResolver;
import io.micronaut.kubernetes.client.openapi.informer.handler.InformerNamespaceResolver;
import io.micronaut.kubernetes.client.openapi.operator.configuration.OperatorConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.controller.Controller;
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerBuilder;
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerManager;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;
import io.micronaut.kubernetes.client.openapi.operator.workqueue.DefaultRateLimitingQueue;
import io.micronaut.kubernetes.client.openapi.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * {@link BeanCreatedEventListener} for the {@link ResourceReconciler} annotated by {@link Operator}.
 * <p>
 * The listener automatically creates the controller infrastructure based on the {@link Operator} configuration. The infrastructure
 * consists from {@link Controller} managed by {@link ControllerManager} and operated by the leader elector.
 * </p>
 *
 * @param <ApiType> kubernetes api type
 */
@Context
@Requires(beans = KubernetesClientConfiguration.class)
@Requires(property = OperatorConfiguration.PREFIX + ".enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
final class ResourceReconcilerCreatedListener<ApiType extends KubernetesObject> implements BeanCreatedEventListener<ResourceReconciler<ApiType>> {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceReconcilerCreatedListener.class);

    private final ControllerManager controllerManager;
    private final ControllerBuilder controllerBuilder;
    private final ThreadFactoryUtil threadFactoryUtil;
    private final InformerNamespaceResolver namespaceResolver;
    private final InformerLabelSelectorResolver labelSelectorResolver;
    private final BeanContext beanContext;

    ResourceReconcilerCreatedListener(
        ControllerManager controllerManager,
        ControllerBuilder controllerBuilder,
        ThreadFactoryUtil threadFactoryUtil,
        InformerNamespaceResolver namespaceResolver,
        InformerLabelSelectorResolver labelSelectorResolver,
        BeanContext beanContext) {
        this.controllerManager = controllerManager;
        this.controllerBuilder = controllerBuilder;
        this.threadFactoryUtil = threadFactoryUtil;
        this.namespaceResolver = namespaceResolver;
        this.labelSelectorResolver = labelSelectorResolver;
        this.beanContext = beanContext;
    }

    @Override
    public ResourceReconciler<ApiType> onCreated(BeanCreatedEvent<ResourceReconciler<ApiType>> event) {
        ResourceReconciler<ApiType> resourceReconciler = event.getBean();

        BeanDefinition<ResourceReconciler<ApiType>> beanDefinition = event.getBeanDefinition();
        if (!beanDefinition.hasAnnotation(Operator.class)) {
            LOG.warn("Bean [{}] implements ResourceReconciler but the @Operator annotation is missing", resourceReconciler);
            return resourceReconciler;
        }

        LOG.debug("Found @Operator annotation on {}", resourceReconciler);

        AnnotationValue<Operator> annotationValue = beanDefinition.getAnnotationMetadata().getAnnotation(Operator.class);
        ControllerConfiguration controllerConfiguration = getControllerConfiguration(annotationValue);

        LOG.debug("Created controller configuration for {}: {}", resourceReconciler, controllerConfiguration);

        ExecutorService waitingWorker = Executors.newSingleThreadExecutor(threadFactoryUtil.getNamedThreadFactory("queue-waiting-worker-%d"));
        DefaultRateLimitingQueue<Request> rateLimitingQueue = new DefaultRateLimitingQueue<>(waitingWorker);

        LOG.debug("Creating controller for {} operator", controllerConfiguration.getName());
        Controller controller = controllerBuilder.build(controllerConfiguration, resourceReconciler, rateLimitingQueue);
        controllerManager.addController(controller);
        return resourceReconciler;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ControllerConfiguration getControllerConfiguration(AnnotationValue<Operator> annotationValue) {
        AnnotationValue<Informer> informer = annotationValue.getAnnotation("informer", Informer.class)
            .orElseThrow(() -> new NullPointerException("The informer parameter of @Operator is required."));
        Class<? extends KubernetesObject> apiType = informer.classValue("apiType", KubernetesObject.class)
            .orElseThrow(() -> new NullPointerException("The apiType parameter of @Informer is required."));
        String name = annotationValue.get("name", String.class).orElseGet(() -> "Operator" + apiType.getSimpleName());
        Set<String> namespaces = namespaceResolver.resolveInformerNamespaces(informer);

        ControllerConfiguration.Builder builder = new ControllerConfiguration.Builder(name, apiType, namespaces)
            .withLabelSelector(labelSelectorResolver.resolveInformerLabels(informer))
            .withResyncCheckPeriod(informer.get("resyncCheckPeriod", Long.class).orElse(0L));

        Optional<Class<? extends Predicate>> onAddFilterOpt = annotationValue.classValue("onAddFilter", Predicate.class);
        if (onAddFilterOpt.isPresent()) {
            Class<? extends Predicate> onAddFilter = onAddFilterOpt.get();
            if (!Objects.equals(onAddFilter, OperatorFilter.OnAdd.class)) {
                LOG.trace("Found [{}] filter in @Operator's 'onAddFilter' value", onAddFilter.getName());
                builder.withOnAddFilter(beanContext.getBean(onAddFilter));
            }
        }

        Optional<Class<? extends BiPredicate>> onUpdateFilterOpt = annotationValue.classValue("onUpdateFilter", BiPredicate.class);
        if (onUpdateFilterOpt.isPresent()) {
            Class<? extends BiPredicate> onUpdateFilter = onUpdateFilterOpt.get();
            if (!Objects.equals(onUpdateFilter, OperatorFilter.OnUpdate.class)) {
                LOG.trace("Found [{}] filter in @Operator's 'onUpdateFilter' value", onUpdateFilter.getName());
                builder.withOnUpdateFilter(beanContext.getBean(onUpdateFilter));
            }
        }

        Optional<Class<? extends BiPredicate>> onDeleteFilterOpt = annotationValue.classValue("onDeleteFilter", BiPredicate.class);
        if (onDeleteFilterOpt.isPresent()) {
            Class<? extends BiPredicate> onDeleteFilter = onDeleteFilterOpt.get();
            if (!Objects.equals(onDeleteFilter, OperatorFilter.OnDelete.class)) {
                LOG.trace("Found [{}] filter in @Operator's 'onDeleteFilter' value", onDeleteFilter.getName());
                builder.withOnDeleteFilter(beanContext.getBean(onDeleteFilter));
            }
        }

        return builder.build();
    }
}
