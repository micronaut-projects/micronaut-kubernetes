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
package io.micronaut.kubernetes.client.informer.resolvers;

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.informer.EmptyLabelSupplier;
import io.micronaut.kubernetes.client.informer.Informer;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The default implementation of {@link InformerLabelSelectorResolver}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Singleton
public class DefaultInformerLabelSelectorResolver implements InformerLabelSelectorResolver {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultInformerLabelSelectorResolver.class);

    private final BeanContext beanContext;

    public DefaultInformerLabelSelectorResolver(@NonNull BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @Nullable
    public String resolveInformerLabels(@NonNull AnnotationValue<Informer> informer) {
        String labelSelector = null;

        Optional<String> labelSelectorOptional = informer.get("labelSelector", String.class);
        if (labelSelectorOptional.isPresent() && StringUtils.isNotEmpty(labelSelectorOptional.get())) {
            labelSelector = labelSelectorOptional.get();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Resolved [{}] label selector from @Informer's 'labelSelector' value", labelSelector);
            }
        }

        Optional<Class<? extends Supplier>> labelSelectorSupplier = informer.classValue("labelSelectorSupplier", Supplier.class);
        if (labelSelectorSupplier.isPresent() && !Objects.equals(labelSelectorSupplier.get(), EmptyLabelSupplier.class)) {
            Class<? extends Supplier<String>> selectorSupplierClass = (Class<? extends Supplier<String>>) labelSelectorSupplier.get();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Resolved [{}] label selector supplier from @Informer's 'labelSelectorSupplier' value", selectorSupplierClass);
            }
            Supplier<String> supplierBean = beanContext.getBean(selectorSupplierClass);
            String labelSelectoSupplierLabels = supplierBean.get();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Resolved [{}] labels from label selector supplier", labelSelectoSupplierLabels);
            }
            labelSelector = labelSelector == null ? labelSelectoSupplierLabels : labelSelector + "," + labelSelectoSupplierLabels;
        }

        if (LOG.isInfoEnabled()) {
            LOG.info(informer + " resolved labelSelector [" + labelSelector + "]");
        }
        return labelSelector;
    }
}
