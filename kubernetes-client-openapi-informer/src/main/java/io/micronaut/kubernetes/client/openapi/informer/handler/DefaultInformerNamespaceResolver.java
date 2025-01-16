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
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.resolver.NamespaceResolver;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @NonNull
    public Set<String> resolveInformerNamespaces(@NonNull AnnotationValue<Informer> annotationValue) {
        Set<String> namespaces = new HashSet<>();

        Class<? extends KubernetesObject> apiType = annotationValue.classValue("apiType", KubernetesObject.class)
            .orElseThrow(() -> new NullPointerException("The apiType parameter of @Informer is required."));

        Optional<String[]> optionalNamespaces = annotationValue.get("namespaces", String[].class);
        if (optionalNamespaces.isPresent()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Found [{}] namespaces in @Informer's 'namespaces' value", String.join(",", optionalNamespaces.get()));
            }
            Collections.addAll(namespaces, optionalNamespaces.get());
        }

        Optional<Class<? extends Supplier>> namespacesSupplier = annotationValue.classValue("namespacesSupplier", Supplier.class);
        if (namespacesSupplier.isPresent()) {
            Class<? extends Supplier<String[]>> namespaceSupplierClass = (Class<? extends Supplier<String[]>>) namespacesSupplier.get();
            if (!Objects.equals(namespaceSupplierClass, EmptyNamespacesSupplier.class)) {
                LOG.trace("Found [{}] namespaces supplier in @Informer's 'namespacesSupplier' value", namespaceSupplierClass);
                Supplier<String[]> supplierBean = beanContext.getBean(namespaceSupplierClass);
                String[] suppliedNamespaces = supplierBean.get();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Found [{}] namespaces using @Informer's 'namespacesSupplier' value", String.join(",", suppliedNamespaces));
                }
                Collections.addAll(namespaces, suppliedNamespaces);
            }
        }

        String namespace = annotationValue.get("namespace", String.class).orElse(Informer.RESOLVE_AUTOMATICALLY);
        if (namespace.equals(Informer.RESOLVE_AUTOMATICALLY)) {
            if (namespaces.isEmpty()) {
                if (namespaceResolver == null) {
                    throw new IllegalStateException("Namespace not found in @Informer's members and namespace resolver not found");
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

        if (namespaces.contains(Informer.ALL_NAMESPACES)) {
            LOG.info("Resolved informer namespaces for apiType={}: {}", apiType, Informer.ALL_NAMESPACES);
            return Collections.emptySet();

        }
        LOG.info("Resolved informer namespaces for apiType={}: {}", apiType, namespaces);
        return namespaces;
    }
}
