package io.micronaut.kubernetes.client.openapi.operator;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformer;
import io.micronaut.kubernetes.client.openapi.informer.SharedIndexInformerFactory;
import io.micronaut.kubernetes.client.openapi.operator.controller.ControllerConfiguration;
import io.micronaut.kubernetes.client.openapi.operator.controller.reconciler.Request;

import java.util.Optional;

/**
 * Operator lister simplifies retrieval of the resources from within the {@link SharedIndexInformer}'s cache.
 *
 * @param <ApiType> the api type of the listed resources
 * @author Pavol Gressa
 */
public final class OperatorResourceLister<ApiType extends KubernetesObject> {

    private final SharedIndexInformerFactory sharedIndexInformerFactory;
    private final ControllerConfiguration controllerConfiguration;

    public OperatorResourceLister(@NonNull ControllerConfiguration controllerConfiguration,
                                  @NonNull SharedIndexInformerFactory sharedIndexInformerFactory) {
        this.sharedIndexInformerFactory = sharedIndexInformerFactory;
        this.controllerConfiguration = controllerConfiguration;
    }

    /**
     * Get the kubernetes resource from the {@link SharedIndexInformer}'s cache for the given {@link Request}.
     * The operation returns {@link Optional} for cases when the resource is not present in the cache.
     *
     * @param request the reconciliation request
     * @return optional resource in local cache
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public Optional<ApiType> get(@NonNull Request request) {
        String namespace = request.getNamespace();
        String name = request.getName();
        Class<? extends KubernetesObject> apiType = controllerConfiguration.getApiType();
        SharedIndexInformer<ApiType> informer = (SharedIndexInformer<ApiType>) sharedIndexInformerFactory.getExistingSharedIndexInformer(apiType, namespace);
        String key = StringUtils.isEmpty(namespace) ? name : namespace + "/" + name;
        return Optional.ofNullable(informer.getIndexer().getByKey(key));
    }
}
