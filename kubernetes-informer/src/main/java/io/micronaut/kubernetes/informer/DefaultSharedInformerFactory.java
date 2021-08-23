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
package io.micronaut.kubernetes.informer;

import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.concurrent.ExecutorService;

/**
 * Factory for {@link SharedInformerFactory}.
 *
 * @author Pavol Gressa
 * @since 3.1
 */
@Requires(property = DefaultSharedInformerFactory.INFORMER_ENABLED, notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@Factory
@BootstrapContextCompatible
public class DefaultSharedInformerFactory {

    public static final String INFORMER_ENABLED = "kubernetes.informer.enabled";

    /**
     * @param apiClient api client
     * @param executorService executor service
     * @return shared informer factory
     */
    @Singleton
    public SharedInformerFactory sharedInformerFactory(ApiClient apiClient, @Named("io") ExecutorService executorService) {
        return new SharedInformerFactory(apiClient, executorService);
    }
}
