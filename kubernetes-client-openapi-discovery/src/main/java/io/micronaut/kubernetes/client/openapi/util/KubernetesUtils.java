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
package io.micronaut.kubernetes.client.openapi.util;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.common.KubernetesObject;
import io.micronaut.kubernetes.client.openapi.model.V1ObjectMeta;
import io.micronaut.kubernetes.client.openapi.model.V1Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utility class with methods to help with ConfigMaps and Secrets.
 */
@Internal
public class KubernetesUtils {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesUtils.class);

    private static final String OPAQUE_SECRET_TYPE = "Opaque";

    /**
     * @return a {@link Predicate} based on the {@code Opaque} secret type that needs to be matched to return {@code true}
     */
    public static Predicate<V1Secret> getIncludeOpaqueSecretTypeFilter() {
        return secret -> {
            V1ObjectMeta metadata = requireMetadata(secret, "opaque secret type");
            String secretName = metadata.getName();
            boolean result = Objects.equals(secret.getType(), OPAQUE_SECRET_TYPE);
            LOG.trace("Include opaque secret type filter {}matched: {}", result ? StringUtils.EMPTY_STRING : "not ", secretName);
            return result;
        };
    }

    /**
     * @param includes the objects to include
     * @return a {@link Predicate} based on a collection of object names to include
     */
    public static Predicate<KubernetesObject> getIncludesFilter(Collection<String> includes) {
        if (includes.isEmpty()) {
            return kubernetesObject -> true;
        }
        LOG.trace("Includes filter: {}", includes);
        return kubernetesObject -> {
            V1ObjectMeta metadata = requireMetadata(kubernetesObject, "includes");
            String objectName = metadata.getName();
            boolean result = includes.contains(objectName);
            LOG.trace("Includes filter {}matched: {}", result ? StringUtils.EMPTY_STRING : "not ", objectName);
            return result;
        };
    }

    /**
     * @param excludes the objects to excludes
     * @return a {@link Predicate} based on a collection of object names to exclude
     */
    public static Predicate<KubernetesObject> getExcludesFilter(Collection<String> excludes) {
        if (excludes.isEmpty()) {
            return kubernetesObject -> true;
        }
        LOG.trace("Excludes filter: {}", excludes);
        return kubernetesObject -> {
            V1ObjectMeta metadata = requireMetadata(kubernetesObject, "excludes");
            String objectName = metadata.getName();
            boolean result = !excludes.contains(objectName);
            LOG.trace("Excludes filter {}matched: {}", result ?  "not " : StringUtils.EMPTY_STRING, objectName);
            return result;
        };
    }

    /**
     * @param labels the labels to include
     * @return a {@link Predicate} based on labels the kubernetes objects has to match to return {@code true}
     */
    public static Predicate<KubernetesObject> getLabelsFilter(Map<String, String> labels) {
        if (labels.isEmpty()) {
            return kubernetesObject -> true;
        }
        LOG.trace("Label include filter: {}", labels.keySet());
        return kubernetesObject -> {
            V1ObjectMeta metadata = requireMetadata(kubernetesObject, "labels");
            Map<String, String> objectLabels = metadata.getLabels();
            if (CollectionUtils.isEmpty(objectLabels)) {
                LOG.trace("Label includes filter not matched: {}", metadata.getName());
                return false;
            }
            boolean result = labels.entrySet().stream().allMatch(
                e -> objectLabels.containsKey(e.getKey()) && objectLabels.get(e.getKey()).equals(e.getValue()));
            LOG.trace("Label includes filter {}matched: {}", result ? StringUtils.EMPTY_STRING : "not ", metadata.getName());
            return result;
        };
    }

    private static V1ObjectMeta requireMetadata(KubernetesObject kubernetesObject, String filterName) {
        V1ObjectMeta metadata = kubernetesObject.getMetadata();
        if (metadata == null) {
            throw new IllegalArgumentException("Object metadata is required to apply " + filterName +
                " filter for " + kubernetesObject.getClass().getSimpleName());
        }
        return metadata;
    }
}
