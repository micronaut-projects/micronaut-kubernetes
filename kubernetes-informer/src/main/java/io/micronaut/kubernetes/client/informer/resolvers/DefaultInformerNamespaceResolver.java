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

import io.kubernetes.client.Discovery;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.util.Namespaces;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.kubernetes.client.DiscoveryCache;
import io.micronaut.kubernetes.client.NamespaceResolver;
import io.micronaut.kubernetes.client.informer.EmptyNamespacesSupplier;
import io.micronaut.kubernetes.client.informer.Informer;
import io.micronaut.kubernetes.client.informer.InformerAnnotationUtils;
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
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Singleton
public class DefaultInformerNamespaceResolver implements InformerNamespaceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultInformerNamespaceResolver.class);

    private final NamespaceResolver namespaceResolver;
    private final BeanContext beanContext;
    private final DiscoveryCache discoveryCache;

    public DefaultInformerNamespaceResolver(@NonNull NamespaceResolver namespaceResolver,
                                            @NonNull BeanContext beanContext,
                                            @Nullable DiscoveryCache discoveryCache) {
        this.namespaceResolver = namespaceResolver;
        this.beanContext = beanContext;
        this.discoveryCache = discoveryCache;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @NonNull
    public Set<String> resolveInformerNamespaces(@NonNull AnnotationValue<Informer> informer) {
        Set<String> namespaces = new HashSet<>();

        Optional<String[]> optionalNamespaces = informer.get("namespaces", String[].class);
        if (optionalNamespaces.isPresent()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Resolved [{}] namespaces from @Informer's 'namespaces' value", String.join(",", optionalNamespaces.get()));
            }
            Collections.addAll(namespaces, optionalNamespaces.get());
        }

        Optional<Class<? extends Supplier>> namespacesSupplier = informer.classValue("namespacesSupplier", Supplier.class);
        if (namespacesSupplier.isPresent()) {
            Class<? extends Supplier<String[]>> namespaceSupplierClass = (Class<? extends Supplier<String[]>>) namespacesSupplier.get();
            if (!Objects.equals(namespaceSupplierClass, EmptyNamespacesSupplier.class)) {
                Supplier<String[]> supplierBean = beanContext.getBean(namespaceSupplierClass);
                String[] suppliedNamespaces = supplierBean.get();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Resolved [{}] namespaces from @Informer's 'namespacesSupplier' value", String.join(",", suppliedNamespaces));
                }

                Collections.addAll(namespaces, suppliedNamespaces);
            }
        }

        String namespace = informer.get("namespace", String.class).orElse(Informer.RESOLVE_AUTOMATICALLY);
        if (namespace.equals(Informer.RESOLVE_AUTOMATICALLY) && namespaces.isEmpty()) {
            String resolvedNamespace = namespaceResolver.resolveNamespace();
            if (LOG.isTraceEnabled()) {
                LOG.trace("No namespace resolved from @Informer's members, using: {}", resolvedNamespace);
            }
            namespaces.add(resolvedNamespace);
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Resolved [{}] namespace from @Informer's 'namespace' value", namespace);
            }
            namespaces.add(namespace);
        }

        if (namespaces.contains(Informer.ALL_NAMESPACES)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Resolved [{}] from @Informer's value", Informer.ALL_NAMESPACES);
            }
            namespaces = Collections.singleton(Namespaces.NAMESPACE_ALL);
        }

        if (discoveryCache != null) {
            Class<? extends KubernetesObject> apiType = InformerAnnotationUtils.resolveApiType(informer);

            Optional<Discovery.APIResource> apiResourceOptional = discoveryCache.find(apiType);
            if (apiResourceOptional.isPresent()) {
                Discovery.APIResource apiResource = apiResourceOptional.get();

                if (apiResource.getNamespaced() != null && !apiResource.getNamespaced()) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("@Informer's resource {} is not namespaced, configuring {}", apiResource.getResourcePlural(), Informer.ALL_NAMESPACES);
                    }
                    namespaces = Collections.singleton(Namespaces.NAMESPACE_ALL);
                }
            }
        }

        if (LOG.isInfoEnabled()) {
            LOG.info(informer + " resolved namespaces [" + String.join(",", namespaces) + "]");
        }
        return namespaces;
    }
}
