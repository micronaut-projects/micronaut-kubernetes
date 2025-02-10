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
package io.micronaut.kubernetes.client.openapi.resolver;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.util.StringUtils;
import io.micronaut.kubernetes.client.openapi.config.KubernetesClientConfiguration;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Default implementation of {@link NamespaceResolver}. The resolution of namespace is evaluated in this order:
 * <ol>
 *     <li>Value configured by `kubernetes.client.namespace`.</li>
 *     <li>If the application is running inside a pod then the namespace is read from the service account namespace file.</li>
 *     <li>Namespace is set do `default`.</li>
 * </ol>
 */
@Singleton
@BootstrapContextCompatible
@Requires(env = Environment.KUBERNETES)
@Requires(beans = KubernetesClientConfiguration.class)
final class DefaultNamespaceResolver implements NamespaceResolver {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultNamespaceResolver.class);

    public static final String DEFAULT_NAMESPACE = "default";

    private final String namespace;

    DefaultNamespaceResolver(ResourceResolver resourceResolver, KubernetesClientConfiguration kubernetesClientConfiguration) {
        String resolvedNamespace = kubernetesClientConfiguration.getNamespace();
        if (StringUtils.isEmpty(resolvedNamespace)) {
            String namespacePath = kubernetesClientConfiguration.getServiceAccount().getNamespacePath();
            LOG.debug("Trying to read the Kubernetes namespace from the file: {}", namespacePath);
            Optional<InputStream> inputStreamOpt = resourceResolver.getResourceAsStream(namespacePath);
            if (inputStreamOpt.isPresent()) {
                InputStream inputStream = inputStreamOpt.get();
                try {
                    resolvedNamespace = new String(inputStream.readAllBytes());
                } catch (IOException e) {
                    LOG.error("Failed to read '{}' file so setting the Kubernetes namespace to: {}", namespacePath, DEFAULT_NAMESPACE);
                    resolvedNamespace = DEFAULT_NAMESPACE;
                }
            } else {
                LOG.info("The Kubernetes namespace not found in configuration files and there is no '{}' file, so setting the namespace to: {}",
                    namespacePath, DEFAULT_NAMESPACE);
                resolvedNamespace = DEFAULT_NAMESPACE;
            }
        }
        namespace = resolvedNamespace;
    }

    @Override
    public String resolveNamespace() {
        return namespace;
    }
}
