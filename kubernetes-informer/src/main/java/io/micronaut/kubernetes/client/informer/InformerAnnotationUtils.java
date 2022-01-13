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
package io.micronaut.kubernetes.client.informer;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.micronaut.core.annotation.AnnotationValue;

import java.util.Optional;

/**
 * Utility class for resolvers.
 *
 * @author Pavol Gressa
 * @since 3.3
 */
public class InformerAnnotationUtils {

    /**
     * Resolves the resource {@link KubernetesObject} api type from the {@link Informer}'s {@link AnnotationValue}.
     * @param annotationValue informer annotation value
     * @return the api type
     */
    public static Class<? extends KubernetesObject> resolveApiType(AnnotationValue<Informer> annotationValue) {
        Optional<Class<? extends KubernetesObject>> optionalClass = annotationValue.classValue("apiType", KubernetesObject.class);
        return optionalClass.orElseThrow(() ->
                new NullPointerException("The apiType parameter of @Informer is required."));
    }

    /**
     * Resolves the resource {@link KubernetesListObject} api list type from the {@link Informer}'s {@link AnnotationValue}.
     * @param annotationValue informer annotation value
     * @return the api list type
     */
    public static Class<? extends KubernetesListObject> resolveApiListType(AnnotationValue<Informer> annotationValue) {
        Optional<Class<? extends KubernetesListObject>> optionalClass = annotationValue.classValue("apiListType", KubernetesListObject.class);
        return optionalClass.orElseThrow(() ->
                new NullPointerException("The apiListType parameter of @Informer is required."));
    }
}
