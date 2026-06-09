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

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Implements {@link ControllerConfiguration}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Internal
public class ControllerConfigurationImpl implements ControllerConfiguration {

    @Nullable
    private String name;
    @Nullable
    private Class<? extends KubernetesObject> apiType;
    @Nullable
    private Class<? extends KubernetesListObject> apiListType;
    @Nullable
    private String resourcePlural;
    @Nullable
    private String apiGroup;
    @Nullable
    private Set<String> namespaces;
    @Nullable
    private String labelSelector;
    @Nullable
    private Long resyncCheckPeriod;
    @Nullable
    private Predicate<? extends KubernetesObject> onAddFilter;
    @Nullable
    private BiPredicate<? extends KubernetesObject, ? extends KubernetesObject> onUpdateFilter;
    @Nullable
    private BiPredicate<? extends KubernetesObject, Boolean> onDeleteFilter;


    /**
     * @return the operator controller name
     */
    @Override
    public String getName() {
        return Objects.requireNonNull(name);
    }

    /**
     *
     * @return api resource type
     */
    @Override
    public Class<? extends KubernetesObject> getApiType() {
        return Objects.requireNonNull(apiType);
    }

    /**
     *
     * @return api resource list type
     */
    @Override
    public Class<? extends KubernetesListObject> getApiListType() {
        return Objects.requireNonNull(apiListType);
    }

    /**
     *
     * @return resource plural
     */
    @Override
    public String getResourcePlural() {
        return Objects.requireNonNull(resourcePlural);
    }

    /**
     *
     * @return api group
     */
    @Override
    public String getApiGroup() {
        return Objects.requireNonNull(apiGroup);
    }

    /**
     *
     * @return namespaces
     */
    @Override
    public Set<String> getNamespaces() {
        return Objects.requireNonNull(namespaces);
    }

    /**
     *
     * @return label selector
     */
    @Override
    public String getLabelSelector() {
        if (labelSelector == null) {
            return "";
        }
        return labelSelector;
    }

    /**
     *
     * @return resync check period
     */
    @Override
    public Long getResyncCheckPeriod() {
        if (resyncCheckPeriod == null) {
            return 0L;
        }
        return resyncCheckPeriod;
    }

    /**
     *
     * @return add filter predicate
     */
    @Override
    @Nullable
    public Predicate<? extends KubernetesObject> getOnAddFilter() {
        return onAddFilter;
    }


    /**
     *
     * @return update filter predicate
     */
    @Override
    @Nullable
    public BiPredicate<? extends KubernetesObject, ? extends KubernetesObject> getOnUpdateFilter() {
        return onUpdateFilter;
    }

    /**
     *
     * @return delete filter predicate
     */
    @Override
    @Nullable
    public BiPredicate<? extends KubernetesObject, Boolean> getOnDeleteFilter() {
        return onDeleteFilter;
    }

    @Override
    public String toString() {
        return "ControllerConfiguration{" +
                "name='" + name + '\'' +
                ", apiType=" + apiType +
                ", apiListType=" + apiListType +
                ", resourcePlural='" + resourcePlural + '\'' +
                ", apiGroup='" + apiGroup + '\'' +
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
    public static class ControllerConfigurationBuilder {

        private final ControllerConfigurationImpl controllerConfiguration;

        public ControllerConfigurationBuilder() {
            this.controllerConfiguration = new ControllerConfigurationImpl();
        }

        /**
         * Set the operator name.
         * @param name name
         * @return builder
         */
        public ControllerConfigurationBuilder withName(String name) {
            this.controllerConfiguration.name = name;
            return this;
        }

        /**
         * Set the api type.
         * @param apiType api type
         * @return builder
         */
        public ControllerConfigurationBuilder withApiType(Class<? extends KubernetesObject> apiType) {
            this.controllerConfiguration.apiType = apiType;
            return this;
        }

        /**
         * Set the api list ype.
         * @param apiListType api list type
         * @return builder
         */
        public ControllerConfigurationBuilder withApiListType(Class<? extends KubernetesListObject> apiListType) {
            this.controllerConfiguration.apiListType = apiListType;
            return this;
        }

        /**
         * Set the resource plural.
         * @param resourcePlural resource plural
         * @return builder
         */
        public ControllerConfigurationBuilder withResourcePlural(String resourcePlural) {
            this.controllerConfiguration.resourcePlural = resourcePlural;
            return this;
        }

        /**
         * Set the api group.
         * @param apiGroup api group
         * @return builder
         */
        public ControllerConfigurationBuilder withApiGroup(String apiGroup) {
            this.controllerConfiguration.apiGroup = apiGroup;
            return this;
        }

        /**
         * Set the namespaces.
         * @param namespaces namespaces
         * @return builder
         */
        public ControllerConfigurationBuilder withNamespaces(Set<String> namespaces) {
            this.controllerConfiguration.namespaces = namespaces;
            return this;
        }

        /**
         * Set the label selector.
         * @param labelSelector label selector
         * @return builder
         */
        public ControllerConfigurationBuilder withLabelSelector(@Nullable String labelSelector) {
            this.controllerConfiguration.labelSelector = labelSelector;
            return this;
        }

        /**
         * Set the filter for added resources.
         * @param onAddFilter on add predicate
         * @return builder
         */
        public ControllerConfigurationBuilder withOnAddFilter(@Nullable Predicate<? extends KubernetesObject> onAddFilter) {
            this.controllerConfiguration.onAddFilter = onAddFilter;
            return this;
        }

        /**
         * Set the filter for updated resources.
         * @param onUpdateFilter on update predicate
         * @return builder
         */
        public ControllerConfigurationBuilder withOnUpdateFilter(@Nullable BiPredicate<? extends KubernetesObject, ? extends KubernetesObject> onUpdateFilter) {
            this.controllerConfiguration.onUpdateFilter = onUpdateFilter;
            return this;
        }

        /**
         * Set the filter for deleted resources.
         * @param onDeleteFilter on delete predicate
         * @return builder
         */
        public ControllerConfigurationBuilder withOnDeleteFilter(@Nullable BiPredicate<? extends KubernetesObject, Boolean> onDeleteFilter) {
            this.controllerConfiguration.onDeleteFilter = onDeleteFilter;
            return this;
        }

        /**
         * Set the resync check period.
         * @param resyncCheckPeriod resynch check period
         * @return builder
         */
        public ControllerConfigurationBuilder withResyncCheckPeriod(Long resyncCheckPeriod) {
            this.controllerConfiguration.resyncCheckPeriod = resyncCheckPeriod;
            return this;
        }

        /**
         * Builds the {@link ControllerConfiguration}.
         * @return the configuration
         */
        public ControllerConfiguration build() {
            return this.controllerConfiguration;
        }
    }
}
