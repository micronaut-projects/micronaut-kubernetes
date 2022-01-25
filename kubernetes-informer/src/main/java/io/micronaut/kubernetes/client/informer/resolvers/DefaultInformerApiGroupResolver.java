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
 * The default implementation of {@link InformerApiGroupResolver}.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
@Singleton
public class DefaultInformerApiGroupResolver implements InformerApiGroupResolver {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultInformerApiGroupResolver.class);

    private final DiscoveryCache discoveryCache;

    public DefaultInformerApiGroupResolver(@Nullable DiscoveryCache discoveryCache) {
        this.discoveryCache = discoveryCache;
    }

    @Override
    @NonNull
    public String resolveInformerApiGroup(@NonNull AnnotationValue<Informer> informer) {
        String apiGroup = null;

        Optional<String> apiGroupOptional = informer.get("apiGroup", String.class);
        if (apiGroupOptional.isPresent()) {
            apiGroup = apiGroupOptional.get();
            if (LOG.isTraceEnabled()) {
                LOG.trace("Resolved [{}] api group from @Informer's 'apiGroup' value", apiGroup);
            }
        }

        if (apiGroup == null || Informer.RESOLVE_AUTOMATICALLY.equals(apiGroup)) {
            if (discoveryCache == null) {
                throw new IllegalArgumentException("The discovery cache is disabled, provide the `apiGroup`" +
                        " parameter to create shared the informer.");
            }

            if (LOG.isTraceEnabled()) {
                LOG.trace("@Informer's 'apiGroup' not specified, resolving from api resources");
            }

            Class<? extends KubernetesObject> apiType = InformerAnnotationUtils.resolveApiType(informer);
            Optional<Discovery.APIResource> apiResourceOptional = discoveryCache.find(apiType);
            if (apiResourceOptional.isPresent()) {
                Discovery.APIResource apiResource = apiResourceOptional.get();
                apiGroup = apiResource.getGroup();
            } else {
                throw new IllegalArgumentException("Failed to resolve the `apiGroup`" +
                        " for " + apiType + " from discovery cache.");
            }
        }

        if (LOG.isInfoEnabled()) {
            LOG.info(informer + " resolved apiGroup [" + apiGroup + "]");
        }
        return apiGroup;
    }
}
