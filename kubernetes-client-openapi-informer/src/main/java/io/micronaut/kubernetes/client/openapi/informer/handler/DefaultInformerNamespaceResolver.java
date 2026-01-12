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
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The default implementation of {@link InformerNamespaceResolver}.
 */
@Singleton
final class DefaultInformerNamespaceResolver implements InformerNamespaceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultInformerNamespaceResolver.class);

    private final BeanContext beanContext;
    private final NamespaceResolver namespaceResolver;

    DefaultInformerNamespaceResolver(@NonNull BeanContext beanContext,
                                     @Nullable NamespaceResolver namespaceResolver) {
        this.beanContext = beanContext;
        this.namespaceResolver = namespaceResolver;
    }

    @Override
    @NonNull
    public Set<String> resolveInformerNamespaces(@NonNull AnnotationValue<Informer> annotationValue) {
        Set<String> namespaces = new HashSet<>();

        Class<? extends KubernetesObject> apiType = annotationValue.classValue("apiType", KubernetesObject.class)
            .orElseThrow(() -> new NullPointerException("The apiType parameter of @Informer is required."));

        resolveFromNamespacesAttribute(annotationValue, namespaces);
        resolveFromNamespaceSupplierAttribute(annotationValue, namespaces);
        resolveFromNamespaceAttribute(annotationValue, namespaces);

        if (namespaces.contains(Informer.ALL_NAMESPACES)) {
            LOG.info("Resolved {} for apiType={}", Informer.ALL_NAMESPACES, apiType);
            return Collections.emptySet();

        }
        LOG.debug("Resolved {} namespaces for apiType={}", namespaces, apiType);
        return namespaces;
    }

    private void resolveFromNamespacesAttribute(AnnotationValue<Informer> annotationValue, Set<String> namespaces) {
        String[] namespaceArray = annotationValue.stringValues("namespaces");
        if (namespaceArray.length > 0) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Found {} namespaces in @Informer's 'namespaces' value", Arrays.toString(namespaceArray));
            }
            Collections.addAll(namespaces, namespaceArray);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void resolveFromNamespaceSupplierAttribute(AnnotationValue<Informer> annotationValue, Set<String> namespaces) {
        Optional<Class<? extends Supplier>> namespacesSupplier = annotationValue.classValue("namespacesSupplier", Supplier.class);
        if (namespacesSupplier.isPresent()) {
            Class<? extends Supplier<String[]>> namespaceSupplierClass = (Class<? extends Supplier<String[]>>) namespacesSupplier.get();
            if (!Objects.equals(namespaceSupplierClass, EmptyNamespacesSupplier.class)) {
                LOG.trace("Found [{}] namespaces supplier in @Informer's 'namespacesSupplier' value", namespaceSupplierClass.getName());
                Supplier<String[]> supplierBean = beanContext.getBean(namespaceSupplierClass);
                String[] suppliedNamespaces = supplierBean.get();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Found {} namespaces using @Informer's 'namespacesSupplier' value", Arrays.toString(suppliedNamespaces));
                }
                Collections.addAll(namespaces, suppliedNamespaces);
            }
        }
    }

    private void resolveFromNamespaceAttribute(AnnotationValue<Informer> annotationValue, Set<String> namespaces) {
        String namespace = annotationValue.stringValue("namespace").orElse(Informer.RESOLVE_AUTOMATICALLY);
        if (namespace.equals(Informer.RESOLVE_AUTOMATICALLY)) {
            if (namespaces.isEmpty()) {
                if (namespaceResolver == null) {
                    throw new IllegalStateException("The @Informer's namespace value is set to " + Informer.RESOLVE_AUTOMATICALLY +
                        " but namespace resolver not found");
                } else {
                    String resolvedNamespace = namespaceResolver.resolveNamespace();
                    LOG.trace("No namespace resolved from @Informer's members, using: {}", resolvedNamespace);
                    namespaces.add(resolvedNamespace);
                }
            }
        } else {
            LOG.trace("Found [{}] namespace in @Informer's 'namespace' value", namespace);
            namespaces.add(namespace);
        }
    }
}
