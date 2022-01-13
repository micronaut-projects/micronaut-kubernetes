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
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.kubernetes.client.DiscoveryCache;
import io.micronaut.kubernetes.client.informer.Informer;
import io.micronaut.kubernetes.client.informer.InformerAnnotationUtils;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * The default implementation of {@link InformerResourcePluralResolver}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Singleton
public class DefaultInformerResourcePluralResolver implements InformerResourcePluralResolver {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultInformerResourcePluralResolver.class);

    private final DiscoveryCache discoveryCache;

    public DefaultInformerResourcePluralResolver(@Nullable DiscoveryCache discoveryCache) {
        this.discoveryCache = discoveryCache;
    }

    @Override
    @NonNull
    public String resolveInformerResourcePlural(@NonNull AnnotationValue<Informer> informer) {
        Optional<String> resourcePluralOptional = informer.get("resourcePlural", String.class);

        String resourcePlural = null;
        if (resourcePluralOptional.isPresent()) {
            resourcePlural = resourcePluralOptional.get();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Resolved [{}] resource plural from @Informer's 'resourcePlural' value", resourcePlural);
            }
        }

        if (resourcePlural == null || Informer.RESOLVE_AUTOMATICALLY.equals(resourcePlural)) {
            if (discoveryCache == null) {
                throw new IllegalArgumentException("The discovery cache is disabled, provide the `resourcePlural` " +
                        " parameter to create shared the informer.");
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("@Informer's 'resourcePlural' not specified, resolving from api resources");
            }

            Class<? extends KubernetesObject> apiType = InformerAnnotationUtils.resolveApiType(informer);

            Optional<Discovery.APIResource> apiResourceOptional = discoveryCache.find(apiType);
            if (apiResourceOptional.isPresent()) {
                Discovery.APIResource apiResource = apiResourceOptional.get();
                resourcePlural = apiResource.getResourcePlural();
            } else {
                throw new IllegalArgumentException("Failed to resolve `resourcePlural` " +
                        " for " + apiType + " from the discovery cache.");
            }
        }

        if (LOG.isInfoEnabled()) {
            LOG.info(informer + " resolved resourcePlural [" + resourcePlural + "]");
        }
        return resourcePlural;
    }
}
