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
package io.micronaut.kubernetes.client.openapi.informer.handler;

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The default implementation of {@link InformerLabelSelectorResolver}.
 */
@Singleton
final class DefaultInformerLabelSelectorResolver implements InformerLabelSelectorResolver {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultInformerLabelSelectorResolver.class);

    private final BeanContext beanContext;

    DefaultInformerLabelSelectorResolver(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @Nullable
    public String resolveInformerLabels(AnnotationValue<Informer> annotationValue) {
        String labelSelector = null;

        Class<? extends KubernetesObject> apiType = annotationValue.classValue("apiType", KubernetesObject.class)
            .orElseThrow(() -> new NullPointerException("The apiType parameter of @Informer is required."));

        Optional<String> labelSelectorOptional = annotationValue.stringValue("labelSelector");
        if (labelSelectorOptional.isPresent() && StringUtils.isNotEmpty(labelSelectorOptional.get())) {
            labelSelector = labelSelectorOptional.get();
            LOG.trace("Found [{}] label selector in @Informer's 'labelSelector' value", labelSelector);
        }

        Optional<Class<? extends Supplier>> labelSelectorSupplier = annotationValue.classValue("labelSelectorSupplier", Supplier.class);
        if (labelSelectorSupplier.isPresent()) {
            Class<? extends Supplier<String>> selectorSupplierClass = (Class<? extends Supplier<String>>) labelSelectorSupplier.get();
            if (!Objects.equals(selectorSupplierClass, EmptyLabelSupplier.class)) {
                LOG.trace("Found [{}] label selector supplier in @Informer's 'labelSelectorSupplier' value", selectorSupplierClass.getName());
                Supplier<String> supplierBean = beanContext.getBean(selectorSupplierClass);
                String labelSelectorSupplierLabels = supplierBean.get();
                LOG.trace("Found [{}] label selector using label selector supplier", labelSelectorSupplierLabels);
                labelSelector = labelSelector == null ? labelSelectorSupplierLabels : labelSelector + "," + labelSelectorSupplierLabels;
            }
        }
        LOG.debug("Resolved [{}] label selector for apiType={}", labelSelector, apiType);
        return labelSelector;
    }
}
