package io.micronaut.kubernetes.client.openapi.operator.controller;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * The controller configuration.
 */
public final class ControllerConfiguration {

    private final String name;
    private final Class<? extends KubernetesObject> apiType;
    private final Set<String> namespaces;
    private String labelSelector;
    private long resyncCheckPeriod = 0L;
    private Predicate<? extends KubernetesObject> onAddFilter;
    private BiPredicate<? extends KubernetesObject, ? extends KubernetesObject> onUpdateFilter;
    private BiPredicate<? extends KubernetesObject, Boolean> onDeleteFilter;

    private ControllerConfiguration(String name, Class<? extends KubernetesObject> apiType, Set<String> namespaces) {
        this.name = name;
        this.apiType = apiType;
        this.namespaces = namespaces;
    }

    /**
     * @return the operator controller name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * @return api resource type
     */
    @NonNull
    public Class<? extends KubernetesObject> getApiType() {
        return apiType;
    }

    /**
     * @return namespaces
     */
    @NonNull
    public Set<String> getNamespaces() {
        return namespaces;
    }

    /**
     * @return label selector
     */
    @Nullable
    public String getLabelSelector() {
        return labelSelector;
    }

    /**
     * @return resync check period
     */
    public long getResyncCheckPeriod() {
        return resyncCheckPeriod;
    }

    /**
     * @return add filter predicate
     */
    @Nullable
    public Predicate<? extends KubernetesObject> getOnAddFilter() {
        return onAddFilter;
    }


    /**
     * @return update filter predicate
     */
    @Nullable
    public BiPredicate<? extends KubernetesObject, ? extends KubernetesObject> getOnUpdateFilter() {
        return onUpdateFilter;
    }

    /**
     * @return delete filter predicate
     */
    @Nullable
    public BiPredicate<? extends KubernetesObject, Boolean> getOnDeleteFilter() {
        return onDeleteFilter;
    }

    @Override
    public String toString() {
        return "ControllerConfiguration{" +
            "name='" + name + '\'' +
            ", apiType=" + apiType +
            ", namespaces=" + namespaces +
            ", labelSelector='" + labelSelector + '\'' +
            ", resyncCheckPeriod=" + resyncCheckPeriod +
            ", onAddFilter=" + onAddFilter +
            ", onUpdateFilter=" + onUpdateFilter +
            ", onDeleteFilter=" + onDeleteFilter +
            '}';
    }

    /**
     * Operator configuration builder.
     */
    public static class Builder {

        private final ControllerConfiguration controllerConfiguration;

        public Builder(@NonNull String name,
                       @NonNull Class<? extends KubernetesObject> apiType,
                       @NonNull Set<String> namespaces) {
            controllerConfiguration = new ControllerConfiguration(name, apiType, namespaces);
        }

        /**
         * Set the label selector.
         *
         * @param labelSelector label selector
         * @return builder
         */
        public Builder withLabelSelector(String labelSelector) {
            controllerConfiguration.labelSelector = labelSelector;
            return this;
        }

        /**
         * Set the filter for added resources.
         *
         * @param onAddFilter on add predicate
         * @return builder
         */
        public Builder withOnAddFilter(Predicate<? extends KubernetesObject> onAddFilter) {
            controllerConfiguration.onAddFilter = onAddFilter;
            return this;
        }

        /**
         * Set the filter for updated resources.
         *
         * @param onUpdateFilter on update predicate
         * @return builder
         */
        public Builder withOnUpdateFilter(BiPredicate<? extends KubernetesObject, ? extends KubernetesObject> onUpdateFilter) {
            controllerConfiguration.onUpdateFilter = onUpdateFilter;
            return this;
        }

        /**
         * Set the filter for deleted resources.
         *
         * @param onDeleteFilter on delete predicate
         * @return builder
         */
        public Builder withOnDeleteFilter(BiPredicate<? extends KubernetesObject, Boolean> onDeleteFilter) {
            controllerConfiguration.onDeleteFilter = onDeleteFilter;
            return this;
        }

        /**
         * Set the resync check period.
         *
         * @param resyncCheckPeriod resynch check period
         * @return builder
         */
        public Builder withResyncCheckPeriod(long resyncCheckPeriod) {
            controllerConfiguration.resyncCheckPeriod = resyncCheckPeriod;
            return this;
        }

        /**
         * Builds the {@link ControllerConfiguration}.
         *
         * @return the configuration
         */
        public ControllerConfiguration build() {
            return controllerConfiguration;
        }
    }
}
